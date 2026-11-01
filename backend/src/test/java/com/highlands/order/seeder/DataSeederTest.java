package com.highlands.order.seeder;

import com.highlands.order.model.AppUser;
import com.highlands.order.repository.CategoryRepository;
import com.highlands.order.repository.CouponRepository;
import com.highlands.order.repository.OrderRepository;
import com.highlands.order.repository.ProductRepository;
import com.highlands.order.repository.ToppingRepository;
import com.highlands.order.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ToppingRepository toppingRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        dataSeeder = new DataSeeder(
                categoryRepository,
                productRepository,
                toppingRepository,
                couponRepository,
                userRepository,
                orderRepository,
                passwordEncoder
        );
    }

    @Test
    void run_seedsMissingUsersAndCouponsEvenWhenCategoriesAlreadyExist() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(couponRepository.existsByCodeIgnoreCase(anyString())).thenReturn(false);
        when(categoryRepository.count()).thenReturn(1L);

        dataSeeder.run();

        verify(userRepository, times(4)).save(any(AppUser.class));
        verify(couponRepository, times(3)).save(any());
        verify(productRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void run_doesNotOverwriteExistingDemoUsersOrCoupons() {
        AppUser existingUser = new AppUser();
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(existingUser));
        when(couponRepository.existsByCodeIgnoreCase(anyString())).thenReturn(true);
        when(categoryRepository.count()).thenReturn(1L);

        dataSeeder.run();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
        verify(couponRepository, never()).save(any());
    }
}
