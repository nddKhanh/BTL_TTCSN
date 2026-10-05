package com.highlands.order.seeder;

import com.highlands.order.model.*;
import com.highlands.order.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ToppingRepository toppingRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(CategoryRepository categoryRepository,
                      ProductRepository productRepository,
                      ToppingRepository toppingRepository,
                      CouponRepository couponRepository,
                      UserRepository userRepository,
                      OrderRepository orderRepository,
                      PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.toppingRepository = toppingRepository;
        this.couponRepository = couponRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return; // Dữ liệu đã được khởi tạo trước đó
        }

        // 1. Tạo Người Dùng (1 Admin, 3 Khách Hàng)
        AppUser admin = userRepository.save(AppUser.builder()
                .fullName("Quản Trị Viên Mộc Nhiên")
                .email("admin@mocnhien.vn")
                .phone("0901234567")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .build());

        AppUser userA = userRepository.save(AppUser.builder()
                .fullName("Nguyễn Văn A")
                .email("nguyen.van.a@gmail.com")
                .phone("0912345678")
                .passwordHash(passwordEncoder.encode("123456"))
                .role(Role.CUSTOMER)
                .build());

        AppUser userB = userRepository.save(AppUser.builder()
                .fullName("Trần Thị B")
                .email("tran.thi.b@gmail.com")
                .phone("0987654321")
                .passwordHash(passwordEncoder.encode("123456"))
                .role(Role.CUSTOMER)
                .build());

        AppUser userC = userRepository.save(AppUser.builder()
                .fullName("Lê Văn C")
                .email("le.van.c@gmail.com")
                .phone("0933445566")
                .passwordHash(passwordEncoder.encode("123456"))
                .role(Role.CUSTOMER)
                .build());

        // 2. Tạo 6 Danh Mục Sản Phẩm
        Category coffee = categoryRepository.save(Category.builder().code("COFFEE").name("Cà Phê Mộc Nhiên").imageUrl("https://placehold.co/480x320?text=Ca+Phe").build());
        Category tea = categoryRepository.save(Category.builder().code("TEA").name("Trà Trái Cây & Trà Sữa").imageUrl("https://placehold.co/480x320?text=Tra").build());
        Category freeze = categoryRepository.save(Category.builder().code("FREEZE").name("Đá Xay Special").imageUrl("https://placehold.co/480x320?text=Da+Xay").build());
        Category juice = categoryRepository.save(Category.builder().code("JUICE").name("Nước Ép & Sinh Tố").imageUrl("https://placehold.co/480x320?text=Sinh+To").build());
        Category cake = categoryRepository.save(Category.builder().code("CAKE").name("Bánh & Snack").imageUrl("https://placehold.co/480x320?text=Banh").build());
        Category combo = categoryRepository.save(Category.builder().code("COMBO").name("Combo Tiết Kiệm").imageUrl("https://placehold.co/480x320?text=Combo").build());

        // 3. Tạo 5 Topping
        Topping thachDao = toppingRepository.save(Topping.builder().name("Thạch Đào").price(BigDecimal.valueOf(10000)).build());
        Topping thachCaPhe = toppingRepository.save(Topping.builder().name("Thạch Cà Phê").price(BigDecimal.valueOf(10000)).build());
        Topping kemCheese = toppingRepository.save(Topping.builder().name("Kem Cheese").price(BigDecimal.valueOf(10000)).build());
        Topping tranChau = toppingRepository.save(Topping.builder().name("Trân Châu Đen").price(BigDecimal.valueOf(10000)).build());
        Topping hatSen = toppingRepository.save(Topping.builder().name("Hạt Sen Extra").price(BigDecimal.valueOf(15000)).build());

        // Helper để thêm Sản Phẩm với các Size chuẩn
        List<Product> seededProducts = new ArrayList<>();

        // Danh mục 1: Cà Phê (5 món)
        seededProducts.add(createProduct(coffee, "Phin Sữa Đá", "Cà phê rang đậm kết hợp sữa đặc ngọt béo, chuẩn gu đậm đà.", 29000, "https://placehold.co/600x450?text=Phin+Sua+Da"));
        seededProducts.add(createProduct(coffee, "Phin Đen Đá", "Cà phê Phin nguyên chất đậm đà, đắng thanh giải nhiệt.", 29000, "https://placehold.co/600x450?text=Phin+Den+Da"));
        seededProducts.add(createProduct(coffee, "Bạc Xỉu Mộc Nhiên", "Nhiều sữa ít cà phê, béo ngậy thanh ngọt dễ uống.", 35000, "https://placehold.co/600x450?text=Bac+Xiu"));
        seededProducts.add(createProduct(coffee, "Cà Phê Muối Nắp Kem", "Lớp màng kem muối mặn béo kết hợp cà phê phin đắng nhẹ.", 39000, "https://placehold.co/600x450?text=Ca+Phe+Muoi"));
        seededProducts.add(createProduct(coffee, "Americano Đá", "Cà phê Espresso pha loãng với nước đá mát lạnh sảng khoái.", 35000, "https://placehold.co/600x450?text=Americano"));

        // Danh mục 2: Trà (5 món)
        seededProducts.add(createProduct(tea, "Trà Sen Vàng", "Hương trà thanh mát kết hợp hạt sen bùi ngậy và củ năng giòn ngọt.", 45000, "https://placehold.co/600x450?text=Tra+Sen+Vang"));
        seededProducts.add(createProduct(tea, "Trà Đào Cam Sả", "Trà đen vị đào mộng nước thơm lừng hương sả tươi và cam tươi.", 45000, "https://placehold.co/600x450?text=Tra+Dao+Cam+Sa"));
        seededProducts.add(createProduct(tea, "Trà Vải Lài", "Trà lài thơm ngát quyện cùng những trái vải mọng nước ngọt ngào.", 45000, "https://placehold.co/600x450?text=Tra+Vai+Lai"));
        seededProducts.add(createProduct(tea, "Trà Ổi Hồng Kem Cheese", "Trà ổi hồng thanh nhẹ phủ lớp kem cheese béo mặn cuốn hút.", 49000, "https://placehold.co/600x450?text=Tra+Oi+Hong"));
        seededProducts.add(createProduct(tea, "Trà Sữa Ô Long Mộc Nhiên", "Trà Ô Long đậm đà kết hợp sữa tươi thanh trùng ngọt dịu.", 42000, "https://placehold.co/600x450?text=Tra+Sua+O+Long"));

        // Danh mục 3: Đá Xay (4 món)
        seededProducts.add(createProduct(freeze, "Freeze Trà Xanh", "Đá xay matcha Nhật lừng danh kết hợp thạch trà xanh giòn sần sật.", 55000, "https://placehold.co/600x450?text=Freeze+Tra+Xanh"));
        seededProducts.add(createProduct(freeze, "Freeze Cà Phê Cốt Dừa", "Cà phê đắng nhẹ xay cùng cốt dừa béo thơm ngậy.", 55000, "https://placehold.co/600x450?text=Freeze+Cot+Dua"));
        seededProducts.add(createProduct(freeze, "Freeze Socola Cookie", "Socola đậm đà kết hợp bánh cookie giòn tan và kem tươi phủ trên.", 59000, "https://placehold.co/600x450?text=Freeze+Socola"));
        seededProducts.add(createProduct(freeze, "Freeze Chanh Dây Thạch Đào", "Vị chanh dây chua thanh giải nhiệt cực đã kèm thạch đào ngọt mát.", 55000, "https://placehold.co/600x450?text=Freeze+Chanh+Day"));

        // Danh mục 4: Sinh Tố & Nước Ép (3 món)
        seededProducts.add(createProduct(juice, "Sinh Tố Bơ Béo", "Bơ sáp chín cây xay sánh mịn béo ngậy thơm ngon.", 49000, "https://placehold.co/600x450?text=Sinh+To+Bo"));
        seededProducts.add(createProduct(juice, "Sinh Tố Xoài Dừa", "Xoài chín ngọt mát quyện cốt dừa thơm lừng.", 45000, "https://placehold.co/600x450?text=Sinh+To+Xoai"));
        seededProducts.add(createProduct(juice, "Nước Ép Cam Tươi", "100% cam sành vắt tươi bổ sung Vitamin C tự nhiên.", 39000, "https://placehold.co/600x450?text=Nuoc+Ep+Cam"));

        // Danh mục 5: Bánh (3 món)
        seededProducts.add(createSingleSizeProduct(cake, "Bánh Mì Que Pate", "Bánh mì que giòn rụm với nhân pate béo ngậy chuẩn vị.", 19000, "https://placehold.co/600x450?text=Banh+Mi+Que"));
        seededProducts.add(createSingleSizeProduct(cake, "Bánh Croissant Bơ Tỏi", "Bánh sừng bò ngàn lớp thơm nức mùi bơ Pháp và tỏi nướng.", 29000, "https://placehold.co/600x450?text=Croissant"));
        seededProducts.add(createSingleSizeProduct(cake, "Bánh Tiramisu Truyền Thống", "Bánh bông lan thấm đượm cà phê rum kết hợp lớp kem mascarpone.", 35000, "https://placehold.co/600x450?text=Tiramisu"));

        // Danh mục 6: Combo (2 món)
        seededProducts.add(createSingleSizeProduct(combo, "Combo Bữa Sáng Năng Lượng", "1 Phin Sữa Đá + 1 Bánh Mì Que Pate tiết kiệm 10.000đ.", 45000, "https://placehold.co/600x450?text=Combo+Sang"));
        seededProducts.add(createSingleSizeProduct(combo, "Combo Xế Chiều Thư Giãn", "1 Trà Sen Vàng + 1 Bánh Croissant bơ tỏi.", 69000, "https://placehold.co/600x450?text=Combo+Xe+Chieu"));

        // 4. Tạo 3 Coupon Mẫu
        couponRepository.saveAll(List.of(
                Coupon.builder()
                        .code("WELCOME10")
                        .discountType(CouponType.PERCENT)
                        .discountValue(BigDecimal.valueOf(10))
                        .maxDiscountAmount(BigDecimal.valueOf(50000))
                        .minOrderAmount(BigDecimal.ZERO)
                        .active(true)
                        .build(),
                Coupon.builder()
                        .code("MOCNHIEN20K")
                        .discountType(CouponType.FIXED)
                        .discountValue(BigDecimal.valueOf(20000))
                        .minOrderAmount(BigDecimal.valueOf(50000))
                        .active(true)
                        .build(),
                Coupon.builder()
                        .code("FREESHIP15K")
                        .discountType(CouponType.FIXED)
                        .discountValue(BigDecimal.valueOf(15000))
                        .minOrderAmount(BigDecimal.valueOf(100000))
                        .active(true)
                        .build()
        ));

        // 5. Tạo 9 Đơn Hàng Mẫu Đa Dạng Trạng Thái
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

    private Product createProduct(Category cat, String name, String desc, double basePrice, String imgUrl) {
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

        Order order = Order.builder()
                .orderCode(code)
                .customerName(user.getFullName())
                .customerPhone(user.getPhone())
                .deliveryAddress("123 Đường 3/2, Quận 10, TP. Hồ Chí Minh")
                .note("Giao giờ hành chính giúp em ạ.")
                .status(status)
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
        orderRepository.save(order);
    }
}
