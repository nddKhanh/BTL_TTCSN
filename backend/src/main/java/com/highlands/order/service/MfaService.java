package com.highlands.order.service;

import com.highlands.order.model.AppUser;
import com.highlands.order.model.MfaEnrollment;
import com.highlands.order.repository.MfaEnrollmentRepository;
import com.highlands.order.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
@Transactional
public class MfaService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private final MfaEnrollmentRepository enrollments;
    private final UserRepository users;
    private final byte[] encryptionKey;

    public MfaService(MfaEnrollmentRepository enrollments, UserRepository users,
                      @Value("${app.mfa.encryption-key}") String encryptionKey) {
        this.enrollments = enrollments;
        this.users = users;
        this.encryptionKey = digest(encryptionKey);
    }

    public Setup start(String email) {
        AppUser user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        byte[] raw = new byte[20];
        RANDOM.nextBytes(raw);
        String secret = base32(raw);
        MfaEnrollment enrollment = enrollments.findByUserId(user.getId()).orElseGet(MfaEnrollment::new);
        enrollment.setUser(user);
        enrollment.setEncryptedSecret(encrypt(secret));
        enrollment.setEnabled(false);
        enrollment.setRecoveryCodeHashes(null);
        enrollments.save(enrollment);
        String uri = "otpauth://totp/MocNhien:" + user.getEmail() + "?secret=" + secret + "&issuer=MocNhien";
        return new Setup(secret, uri);
    }

    public List<String> confirm(String email, String code) {
        MfaEnrollment enrollment = enrollment(email);
        if (!validTotp(reveal(enrollment), code)) throw new IllegalArgumentException("Mã xác thực không đúng");
        List<String> recoveryCodes = new ArrayList<>();
        List<String> hashes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            byte[] raw = new byte[8]; RANDOM.nextBytes(raw);
            String recovery = HexFormat.of().formatHex(raw).toUpperCase();
            recoveryCodes.add(recovery); hashes.add(hashText(recovery));
        }
        enrollment.setRecoveryCodeHashes(String.join(",", hashes));
        enrollment.setEnabled(true);
        enrollment.setConfirmedAt(java.time.LocalDateTime.now());
        return recoveryCodes;
    }

    public void challenge(String email, String code) {
        MfaEnrollment enrollment = enrollment(email);
        if (!enrollment.isEnabled()) throw new IllegalArgumentException("MFA chưa được bật");
        if (validTotp(reveal(enrollment), code)) return;
        String candidate = hashText(code.trim().toUpperCase());
        List<String> hashes = new ArrayList<>(List.of(enrollment.getRecoveryCodeHashes().split(",")));
        int match = hashes.indexOf(candidate);
        if (match < 0) throw new IllegalArgumentException("Mã xác thực không đúng");
        hashes.remove(match);
        enrollment.setRecoveryCodeHashes(String.join(",", hashes));
    }

    private MfaEnrollment enrollment(String email) {
        AppUser user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        return enrollments.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("MFA chưa được cấu hình"));
    }

    static boolean validTotp(String secret, String code) {
        if (code == null || !code.matches("\\d{6}")) return false;
        long window = System.currentTimeMillis() / 30_000L;
        for (long offset = -1; offset <= 1; offset++) {
            if (totp(secret, window + offset).equals(code)) return true;
        }
        return false;
    }

    static String totp(String secret, long window) {
        try {
            byte[] key = decodeBase32(secret);
            byte[] counter = java.nio.ByteBuffer.allocate(8).putLong(window).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] digest = mac.doFinal(counter);
            int offset = digest[digest.length - 1] & 15;
            int binary = ((digest[offset] & 127) << 24) | ((digest[offset + 1] & 255) << 16)
                    | ((digest[offset + 2] & 255) << 8) | (digest[offset + 3] & 255);
            return String.format("%06d", binary % 1_000_000);
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    String reveal(MfaEnrollment enrollment) {
        try {
            byte[] packed = Base64.getDecoder().decode(enrollment.getEncryptedSecret());
            byte[] iv = java.util.Arrays.copyOfRange(packed, 0, 12);
            byte[] ciphertext = java.util.Arrays.copyOfRange(packed, 12, packed.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể giải mã MFA secret", ex);
        }
    }

    private String encrypt(String value) {
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, iv));
            byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] packed = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, packed, 0, iv.length);
            System.arraycopy(ciphertext, 0, packed, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(packed);
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể mã hóa MFA secret", ex);
        }
    }

    private static byte[] digest(String value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    private static String hashText(String value) {
        return HexFormat.of().formatHex(digest(value));
    }

    private static byte[] decodeBase32(String value) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int buffer = 0, bits = 0;
        for (char item : value.toUpperCase().toCharArray()) {
            int index = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(item);
            if (index < 0) continue;
            buffer = (buffer << 5) | index; bits += 5;
            if (bits >= 8) out.write((buffer >> (bits -= 8)) & 255);
        }
        return out.toByteArray();
    }

    private static String base32(byte[] value) {
        StringBuilder out = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte item : value) {
            buffer = (buffer << 8) | (item & 0xff); bits += 8;
            while (bits >= 5) { out.append(BASE32[(buffer >> (bits -= 5)) & 31]); }
        }
        if (bits > 0) out.append(BASE32[(buffer << (5 - bits)) & 31]);
        return out.toString();
    }

    public record Setup(String secret, String otpauthUri) { }
}
