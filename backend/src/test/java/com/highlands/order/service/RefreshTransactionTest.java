package com.highlands.order.service;

import com.highlands.order.model.*;
import com.highlands.order.repository.RefreshSessionRepository;
import com.highlands.order.repository.UserRepository;
import com.highlands.order.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTransactionTest {
    @Test
    void springTransactionCommitsFamilyRevocationEvenWhenReplayIsRejected() {
        var repository = mock(RefreshSessionRepository.class);
        var user = AppUser.builder().id(9L).accountStatus(AccountStatus.ACTIVE).build();
        var replay = RefreshSession.builder().user(user).familyId("family")
                .expiresAt(LocalDateTime.now().plusDays(1)).revokedAt(LocalDateTime.now().minusMinutes(1)).build();
        var active = RefreshSession.builder().user(user).familyId("family").build();
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(replay));
        when(repository.findAllByFamilyIdAndRevokedAtIsNull("family")).thenReturn(List.of(active));
        var transactions = new RecordingTransactions();
        var target = new RefreshSessionService(repository, mock(JwtService.class), mock(UserRepository.class));
        var factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        var service = (RefreshSessionService) factory.getProxy();

        assertThrows(BadCredentialsException.class, () -> service.rotate("replayed-token"));

        assertNotNull(active.getRevokedAt());
        assertEquals(1, transactions.commits);
        assertEquals(0, transactions.rollbacks);
    }

    private static class RecordingTransactions extends AbstractPlatformTransactionManager {
        int commits;
        int rollbacks;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { commits++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
    }
}
