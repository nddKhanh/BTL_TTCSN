package com.highlands.order.seeder;

import com.highlands.order.config.ApprovalProviderProperties;
import com.highlands.order.domain.approval.ApprovalStatus;
import com.highlands.order.domain.fulfillment.FulfillmentStatus;
import com.highlands.order.model.*;
import com.highlands.order.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@Component
@Profile({"dev", "demo"})
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ToppingRepository toppingRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderApprovalRepository orderApprovalRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final FulfillmentHistoryRepository fulfillmentHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApprovalProviderProperties approvalProvider;

    public DataSeeder(CategoryRepository categoryRepository,
                      ProductRepository productRepository,
                      ToppingRepository toppingRepository,
                      CouponRepository couponRepository,
                      UserRepository userRepository,
                      OrderRepository orderRepository,
                      OrderApprovalRepository orderApprovalRepository,
                      ApprovalHistoryRepository approvalHistoryRepository,
                      FulfillmentHistoryRepository fulfillmentHistoryRepository,
                      PasswordEncoder passwordEncoder,
                      ApprovalProviderProperties approvalProvider) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.toppingRepository = toppingRepository;
        this.couponRepository = couponRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderApprovalRepository = orderApprovalRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.fulfillmentHistoryRepository = fulfillmentHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.approvalProvider = approvalProvider;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Tài khoản và coupon được seed độc lập để database demo thiếu một phần
        // vẫn được bổ sung mà không tạo trùng dữ liệu đã có.
        seedDemoUser("Quản Trị Viên Mộc Nhiên", "admin@mocnhien.vn", "0901234567", "admin123", Role.ADMIN);

        AppUser userA = seedDemoUser("Nguyễn Văn A", "nguyen.van.a@gmail.com", "0912345678", "123456", Role.CUSTOMER);
        AppUser userB = seedDemoUser("Trần Thị B", "tran.thi.b@gmail.com", "0987654321", "123456", Role.CUSTOMER);
        AppUser userC = seedDemoUser("Lê Văn C", "le.van.c@gmail.com", "0933445566", "123456", Role.CUSTOMER);

        seedDemoCoupons();

        // 1. Tạo 6 Danh Mục Sản Phẩm
        Category coffee = seedCategory("COFFEE", "Cà Phê Mộc Nhiên", "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/85/C%C3%A0_ph%C3%AA_s%E1%BB%AFa_%C4%91%C3%A1_%28509309424%29.jpg/960px-C%C3%A0_ph%C3%AA_s%E1%BB%AFa_%C4%91%C3%A1_%28509309424%29.jpg");
        Category tea = seedCategory("TEA", "Trà Trái Cây & Trà Sữa", "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6b/Peach_iced_tea_with_orange_slices.jpg/960px-Peach_iced_tea_with_orange_slices.jpg");
        Category freeze = seedCategory("FREEZE", "Đá Xay Special", "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/8f/Ceremonial_Grade_Matcha_Frappe_%28no_coffee%29_and_Pumpkin_Spice_Frappe_%28blended_with_espresso%29_-_Brooklands_Cafe_2026-09-23.jpg/960px-Ceremonial_Grade_Matcha_Frappe_%28no_coffee%29_and_Pumpkin_Spice_Frappe_%28blended_with_espresso%29_-_Brooklands_Cafe_2026-09-23.jpg");
        Category juice = seedCategory("JUICE", "Nước Ép & Sinh Tố", "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/47/Fruit_smoothie.jpg/960px-Fruit_smoothie.jpg");
        Category cake = seedCategory("CAKE", "Bánh & Snack", "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/41/Pastries_in_QAZAQ_COFFEE_2024-11-28.jpg/960px-Pastries_in_QAZAQ_COFFEE_2024-11-28.jpg");
        Category combo = seedCategory("COMBO", "Combo Tiết Kiệm", "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a6/Coffee%2C_breakfast%2C_croissant.jpg/960px-Coffee%2C_breakfast%2C_croissant.jpg");

        // 2. Tạo 5 Topping
        seedTopping("Thạch Đào", 10000);
        seedTopping("Thạch Cà Phê", 10000);
        seedTopping("Kem Cheese", 10000);
        seedTopping("Trân Châu Đen", 10000);
        seedTopping("Hạt Sen Extra", 15000);

        // Helper để thêm Sản Phẩm với các Size chuẩn
        List<Product> seededProducts = new ArrayList<>();

        // Danh mục 1: Cà Phê (5 món)
        // Ảnh lấy từ Wikimedia Commons, nguồn và giấy phép ở docs/image-credits.md.
        seededProducts.add(createProduct(coffee, "Phin Sữa Đá", "Cà phê rang đậm kết hợp sữa đặc ngọt béo, chuẩn gu đậm đà.", 29000, "https://upload.wikimedia.org/wikipedia/commons/1/1b/C%C3%A0_ph%C3%AA_s%E1%BB%AFa_%C4%91%C3%A1.jpg"));
        seededProducts.add(createProduct(coffee, "Phin Đen Đá", "Cà phê Phin nguyên chất đậm đà, đắng thanh giải nhiệt.", 29000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/47/Ca_phe_Nha_Trang.jpg/960px-Ca_phe_Nha_Trang.jpg"));
        seededProducts.add(createProduct(coffee, "Bạc Xỉu Mộc Nhiên", "Nhiều sữa ít cà phê, béo ngậy thanh ngọt dễ uống.", 35000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/35/Iced_B%E1%BA%A1c_X%E1%BB%89u_served_in_tall_glass_on_the_table_in_tha_cafe%2C_Danang_city%2C_2023.jpg/960px-Iced_B%E1%BA%A1c_X%E1%BB%89u_served_in_tall_glass_on_the_table_in_tha_cafe%2C_Danang_city%2C_2023.jpg"));
        seededProducts.add(createProduct(coffee, "Cà Phê Muối Nắp Kem", "Lớp màng kem muối mặn béo kết hợp cà phê phin đắng nhẹ.", 39000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0d/Ly_c%C3%A0_ph%C3%AA_mu%E1%BB%91i_%E1%BB%9F_T%C3%A2n_Ph%C3%BA_ng14th5n2023_%281%29.jpg/960px-Ly_c%C3%A0_ph%C3%AA_mu%E1%BB%91i_%E1%BB%9F_T%C3%A2n_Ph%C3%BA_ng14th5n2023_%281%29.jpg"));
        seededProducts.add(createProduct(coffee, "Americano Đá", "Cà phê Espresso pha loãng với nước đá mát lạnh sảng khoái.", 35000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/be/Iced_Americano_1.jpg/960px-Iced_Americano_1.jpg"));

        // Danh mục 2: Trà (5 món)
        seededProducts.add(createProduct(tea, "Trà Sen Vàng", "Hương trà thanh mát kết hợp hạt sen bùi ngậy và củ năng giòn ngọt.", 45000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1c/Lotus_flower_tea.jpg/960px-Lotus_flower_tea.jpg"));
        seededProducts.add(createProduct(tea, "Trà Đào Cam Sả", "Trà đen vị đào mộng nước thơm lừng hương sả tươi và cam tươi.", 45000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6b/Peach_iced_tea_with_orange_slices.jpg/960px-Peach_iced_tea_with_orange_slices.jpg"));
        seededProducts.add(createProduct(tea, "Trà Vải Lài", "Trà lài thơm ngát quyện cùng những trái vải mọng nước ngọt ngào.", 45000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d4/Iced_lychee_tea_and_Es_teh_manis_-_Bali_2025-09-25.jpg/960px-Iced_lychee_tea_and_Es_teh_manis_-_Bali_2025-09-25.jpg"));
        seededProducts.add(createProduct(tea, "Trà Ổi Hồng Kem Cheese", "Trà ổi hồng thanh nhẹ phủ lớp kem cheese béo mặn cuốn hút.", 49000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/32/Guava_juice_%283110765359%29.jpg/960px-Guava_juice_%283110765359%29.jpg"));
        seededProducts.add(createProduct(tea, "Trà Sữa Ô Long Mộc Nhiên", "Trà Ô Long đậm đà kết hợp sữa tươi thanh trùng ngọt dịu.", 42000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/62/Fresh_Milk_Tea_3.jpg/960px-Fresh_Milk_Tea_3.jpg"));

        // Danh mục 3: Đá Xay (4 món)
        seededProducts.add(createProduct(freeze, "Freeze Trà Xanh", "Đá xay matcha Nhật lừng danh kết hợp thạch trà xanh giòn sần sật.", 55000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/8f/Ceremonial_Grade_Matcha_Frappe_%28no_coffee%29_and_Pumpkin_Spice_Frappe_%28blended_with_espresso%29_-_Brooklands_Cafe_2026-09-23.jpg/960px-Ceremonial_Grade_Matcha_Frappe_%28no_coffee%29_and_Pumpkin_Spice_Frappe_%28blended_with_espresso%29_-_Brooklands_Cafe_2026-09-23.jpg"));
        seededProducts.add(createProduct(freeze, "Freeze Cà Phê Cốt Dừa", "Cà phê đắng nhẹ xay cùng cốt dừa béo thơm ngậy.", 55000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/21/Vietnamese_Salted_Coffee_%26_Xin_chao%27s_coconut_Coffee_-_Xin_Chao%2C_Eastbourne_2026-08-14.jpg/960px-Vietnamese_Salted_Coffee_%26_Xin_chao%27s_coconut_Coffee_-_Xin_Chao%2C_Eastbourne_2026-08-14.jpg"));
        seededProducts.add(createProduct(freeze, "Freeze Socola Cookie", "Socola đậm đà kết hợp bánh cookie giòn tan và kem tươi phủ trên.", 59000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c3/Oreo_milkshake_-_Creperie_Doux_Sourire_2025-05-03.jpg/960px-Oreo_milkshake_-_Creperie_Doux_Sourire_2025-05-03.jpg"));
        seededProducts.add(createProduct(freeze, "Freeze Chanh Dây Thạch Đào", "Vị chanh dây chua thanh giải nhiệt cực đã kèm thạch đào ngọt mát.", 55000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/8e/Siem_Reap_iced_passion_fruit_juice_%285057850765%29.jpg/960px-Siem_Reap_iced_passion_fruit_juice_%285057850765%29.jpg"));

        // Danh mục 4: Sinh Tố & Nước Ép (3 món)
        seededProducts.add(createProduct(juice, "Sinh Tố Bơ Béo", "Bơ sáp chín cây xay sánh mịn béo ngậy thơm ngon.", 49000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/93/Avocado%2C_milk%2C_condensed_milk_and_ice_smoothies_-_Amazing_Chef_food_processor.jpg/960px-Avocado%2C_milk%2C_condensed_milk_and_ice_smoothies_-_Amazing_Chef_food_processor.jpg"));
        seededProducts.add(createProduct(juice, "Sinh Tố Xoài Dừa", "Xoài chín ngọt mát quyện cốt dừa thơm lừng.", 45000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/09/Fresh-mango-smoothie_01.jpg/960px-Fresh-mango-smoothie_01.jpg"));
        seededProducts.add(createProduct(juice, "Nước Ép Cam Tươi", "100% cam sành vắt tươi bổ sung Vitamin C tự nhiên.", 39000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/67/Orange_juice_1_edit1.jpg/960px-Orange_juice_1_edit1.jpg"));

        // Danh mục 5: Bánh (3 món)
        seededProducts.add(createSingleSizeProduct(cake, "Bánh Mì Que Pate", "Bánh mì que giòn rụm với nhân pate béo ngậy chuẩn vị.", 19000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0d/Banh_my_que.jpg/960px-Banh_my_que.jpg"));
        seededProducts.add(createSingleSizeProduct(cake, "Bánh Croissant Bơ Tỏi", "Bánh sừng bò ngàn lớp thơm nức mùi bơ Pháp và tỏi nướng.", 29000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a6/Croissant_3.jpg/960px-Croissant_3.jpg"));
        seededProducts.add(createSingleSizeProduct(cake, "Bánh Tiramisu Truyền Thống", "Bánh bông lan thấm đượm cà phê rum kết hợp lớp kem mascarpone.", 35000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/78/Tiramisu_-_Bistro_Don_Giovanni_-_Sarah_Stierch_2018_02.jpg/960px-Tiramisu_-_Bistro_Don_Giovanni_-_Sarah_Stierch_2018_02.jpg"));

        // Danh mục 6: Combo (2 món)
        seededProducts.add(createSingleSizeProduct(combo, "Combo Bữa Sáng Năng Lượng", "1 Phin Sữa Đá + 1 Bánh Mì Que Pate tiết kiệm 10.000đ.", 45000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c5/2023-12-10_Coffee_and_b%C3%A1nh_m%C3%AC_in_Saigon.jpg/960px-2023-12-10_Coffee_and_b%C3%A1nh_m%C3%AC_in_Saigon.jpg"));
        seededProducts.add(createSingleSizeProduct(combo, "Combo Xế Chiều Thư Giãn", "1 Trà Sen Vàng + 1 Bánh Croissant bơ tỏi.", 69000, "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/5c/Croissant-newspaper-and-tea.jpg/960px-Croissant-newspaper-and-tea.jpg"));

        if (approvalProvider.usesDxos()) {
            return;
        }

        // 3. Tạo 9 Đơn Hàng Mẫu Đa Dạng Trạng Thái
        if (orderRepository.count() > 0) {
            return; // Catalog có thể được bổ sung, nhưng không tạo lại đơn mẫu.
        }

        createSampleOrder(userA, seededProducts.get(0), "M", "100% Đá", "100% Đường", "Thạch Cà Phê", 2, OrderStatus.COMPLETED, "WELCOME10", BigDecimal.valueOf(6800));
        createSampleOrder(userB, seededProducts.get(5), "L", "70% Đá", "50% Đường", "Hạt Sen Extra", 1, OrderStatus.COMPLETED, "MOCNHIEN20K", BigDecimal.valueOf(20000));
        createSampleOrder(userC, seededProducts.get(10), "M", "100% Đá", "100% Đường", "Kem Cheese", 2, OrderStatus.DELIVERING, null, BigDecimal.ZERO);
        createSampleOrder(userA, seededProducts.get(2), "S", "50% Đá", "70% Đường", null, 3, OrderStatus.DELIVERING, null, BigDecimal.ZERO);
        createSampleOrder(userB, seededProducts.get(8), "L", "100% Đá", "100% Đường", "Thạch Đào", 1, OrderStatus.CONFIRMED, null, BigDecimal.ZERO);
        createSampleOrder(userC, seededProducts.get(15), "Tiêu chuẩn", null, null, null, 2, OrderStatus.CONFIRMED, null, BigDecimal.ZERO);
        createSampleOrder(userA, seededProducts.get(18), "Tiêu chuẩn", null, null, null, 1, OrderStatus.PENDING, null, BigDecimal.ZERO);
        createSampleOrder(userB, seededProducts.get(3), "M", "100% Đá", "100% Đường", "Kem Cheese", 2, OrderStatus.PENDING, null, BigDecimal.ZERO);
        createSampleOrder(userC, seededProducts.get(1), "S", "Không Đá", "Không Đường", null, 1, OrderStatus.CANCELLED, null, BigDecimal.ZERO);
    }

    private AppUser seedDemoUser(String fullName, String email, String phone, String password, Role role) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> userRepository.save(AppUser.builder()
                        .fullName(fullName)
                        .email(email)
                        .phone(phone)
                        .passwordHash(passwordEncoder.encode(password))
                        .role(role)
                        .build()));
    }

    private Category seedCategory(String code, String name, String imageUrl) {
        return categoryRepository.findByCode(code)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .code(code).name(name).imageUrl(imageUrl).build()));
    }

    private void seedTopping(String name, double price) {
        boolean exists = toppingRepository.findAll().stream()
                .anyMatch(topping -> topping.getName().equalsIgnoreCase(name));
        if (!exists) {
            toppingRepository.save(Topping.builder()
                    .name(name).price(BigDecimal.valueOf(price)).build());
        }
    }

    private void seedDemoCoupons() {
        seedDemoCoupon("WELCOME10", CouponType.PERCENT, BigDecimal.valueOf(10),
                BigDecimal.ZERO, BigDecimal.valueOf(50000));
        seedDemoCoupon("MOCNHIEN20K", CouponType.FIXED, BigDecimal.valueOf(20000),
                BigDecimal.valueOf(50000), null);
        seedDemoCoupon("FREESHIP15K", CouponType.FIXED, BigDecimal.valueOf(15000),
                BigDecimal.valueOf(100000), null);
    }

    private void seedDemoCoupon(String code, CouponType discountType, BigDecimal discountValue,
                                BigDecimal minOrderAmount, BigDecimal maxDiscountAmount) {
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            return;
        }

        couponRepository.save(Coupon.builder()
                .code(code)
                .discountType(discountType)
                .discountValue(discountValue)
                .minOrderAmount(minOrderAmount)
                .maxDiscountAmount(maxDiscountAmount)
                .active(true)
                .build());
    }

    private Product createProduct(Category cat, String name, String desc, double basePrice, String imgUrl) {
        Product existing = productRepository.findAll().stream()
                .filter(product -> product.getCategory().getId().equals(cat.getId())
                        && product.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        Product p = Product.builder()
                .category(cat)
                .name(name)
                .description(desc)
                .basePrice(BigDecimal.valueOf(basePrice))
                .imageUrl(imgUrl)
                .isActive(true)
                .build();

        p.getSizes().add(ProductSize.builder().product(p).sizeName("S").extraPrice(BigDecimal.ZERO).build());
        p.getSizes().add(ProductSize.builder().product(p).sizeName("M").extraPrice(BigDecimal.valueOf(10000)).build());
        p.getSizes().add(ProductSize.builder().product(p).sizeName("L").extraPrice(BigDecimal.valueOf(16000)).build());

        return productRepository.save(p);
    }

    private Product createSingleSizeProduct(Category cat, String name, String desc, double basePrice, String imgUrl) {
        Product existing = productRepository.findAll().stream()
                .filter(product -> product.getCategory().getId().equals(cat.getId())
                        && product.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        Product p = Product.builder()
                .category(cat)
                .name(name)
                .description(desc)
                .basePrice(BigDecimal.valueOf(basePrice))
                .imageUrl(imgUrl)
                .isActive(true)
                .build();

        p.getSizes().add(ProductSize.builder().product(p).sizeName("Tiêu chuẩn").extraPrice(BigDecimal.ZERO).build());

        return productRepository.save(p);
    }

    private void createSampleOrder(AppUser user, Product product, String size, String ice, String sugar, String topping, int qty, OrderStatus status, String couponCode, BigDecimal discount) {
        String code = "COF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BigDecimal sizeExtra = size.equals("M") ? BigDecimal.valueOf(10000) : (size.equals("L") ? BigDecimal.valueOf(16000) : BigDecimal.ZERO);
        BigDecimal toppingPrice = topping != null ? BigDecimal.valueOf(10000) : BigDecimal.ZERO;
        BigDecimal unitPrice = product.getBasePrice().add(sizeExtra).add(toppingPrice);
        BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
        BigDecimal shippingFee = itemSubtotal.compareTo(BigDecimal.valueOf(200000)) >= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(15000);
        BigDecimal totalAmount = itemSubtotal.subtract(discount).add(shippingFee);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) totalAmount = BigDecimal.ZERO;

        FulfillmentStatus fulfillmentStatus = fulfillmentStatus(status);
        Order order = Order.builder()
                .orderCode(code)
                .customerName(user.getFullName())
                .customerPhone(user.getPhone())
                .deliveryAddress("123 Đường 3/2, Quận 10, TP. Hồ Chí Minh")
                .note("Giao giờ hành chính giúp em ạ.")
                .status(status)
                .fulfillmentStatus(fulfillmentStatus)
                .subtotal(itemSubtotal)
                .discountAmount(discount)
                .couponCode(couponCode)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .user(user)
                .items(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .productName(product.getName())
                .sizeName(size)
                .iceLevel(ice)
                .sugarLevel(sugar)
                .toppings(topping)
                .quantity(qty)
                .unitPrice(unitPrice)
                .subtotal(itemSubtotal)
                .build();

        order.getItems().add(item);
        order = orderRepository.save(order);
        ApprovalStatus approvalStatus = approvalStatus(status);
        OrderApproval approval = orderApprovalRepository.save(OrderApproval.builder()
                .approvalRef(UUID.randomUUID().toString())
                .order(order)
                .status(approvalStatus)
                .createdBy("user:" + user.getId())
                .createdAt(LocalDateTime.now())
                .decidedBy(approvalStatus.isTerminal() ? "seed" : null)
                .decidedAt(approvalStatus.isTerminal() ? LocalDateTime.now() : null)
                .build());
        approvalHistoryRepository.save(ApprovalHistory.builder()
                .approval(approval)
                .action("SEED")
                .actorId("seed")
                .toStatus(approvalStatus)
                .reason("Demo data")
                .requestId("seed:" + code)
                .build());
        fulfillmentHistoryRepository.save(FulfillmentHistory.builder()
                .order(order)
                .action("SEED")
                .actorId("seed")
                .fromStatus(fulfillmentStatus)
                .toStatus(fulfillmentStatus)
                .reason("Demo data")
                .requestId("seed:" + code)
                .build());
    }

    private ApprovalStatus approvalStatus(OrderStatus status) {
        return switch (status) {
            case PENDING -> ApprovalStatus.PENDING;
            case CANCELLED -> ApprovalStatus.WITHDRAWN;
            case CONFIRMED, DELIVERING, COMPLETED -> ApprovalStatus.ACCEPTED;
        };
    }

    private FulfillmentStatus fulfillmentStatus(OrderStatus status) {
        return switch (status) {
            case PENDING -> FulfillmentStatus.NOT_STARTED;
            case CONFIRMED -> FulfillmentStatus.PREPARING;
            case DELIVERING -> FulfillmentStatus.DELIVERING;
            case COMPLETED -> FulfillmentStatus.COMPLETED;
            case CANCELLED -> FulfillmentStatus.CANCELLED;
        };
    }
}
