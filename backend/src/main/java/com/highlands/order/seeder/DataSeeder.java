package com.highlands.order.seeder;

import com.highlands.order.model.Category;
import com.highlands.order.model.Product;
import com.highlands.order.model.ProductSize;
import com.highlands.order.model.Topping;
import com.highlands.order.repository.CategoryRepository;
import com.highlands.order.repository.ProductRepository;
import com.highlands.order.repository.ToppingRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ToppingRepository toppingRepository;

    public DataSeeder(CategoryRepository categoryRepository,
                      ProductRepository productRepository,
                      ToppingRepository toppingRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.toppingRepository = toppingRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return; // Đã có dữ liệu
        }

        // 1. Tạo Danh mục
        Category coffee = categoryRepository.save(Category.builder().code("COFFEE").name("Cà Phê").imageUrl("https://order.highlandscoffee.com.vn/v2.1/img/category/coffee.png").build());
        Category tea = categoryRepository.save(Category.builder().code("TEA").name("Trà").imageUrl("https://order.highlandscoffee.com.vn/v2.1/img/category/tea.png").build());
        Category freeze = categoryRepository.save(Category.builder().code("FREEZE").name("Freeze").imageUrl("https://order.highlandscoffee.com.vn/v2.1/img/category/freeze.png").build());
        Category cake = categoryRepository.save(Category.builder().code("CAKE").name("Bánh").imageUrl("https://order.highlandscoffee.com.vn/v2.1/img/category/cake.png").build());

        // 2. Tạo Topping
        toppingRepository.saveAll(List.of(
                Topping.builder().name("Thạch Đào").price(BigDecimal.valueOf(10000)).build(),
                Topping.builder().name("Thạch Cà Phê").price(BigDecimal.valueOf(10000)).build(),
                Topping.builder().name("Kem Cheese").price(BigDecimal.valueOf(10000)).build(),
                Topping.builder().name("Hạt Sen Extra").price(BigDecimal.valueOf(15000)).build()
        ));

        // 3. Tạo Sản phẩm
        // Product 1: Phin Sữa Đá
        Product phinSua = Product.builder()
                .category(coffee)
                .name("Phin Sữa Đá")
                .description("Hương vị cà phê đậm đà kết hợp cùng sữa đặc ngọt ngào truyền thống của Highlands Coffee.")
                .basePrice(BigDecimal.valueOf(29000))
                .imageUrl("https://vtcpay.vn/blog/wp-content/uploads/2023/04/phin-sua-da-highlands.jpg")
                .isActive(true)
                .build();
        phinSua.getSizes().add(ProductSize.builder().product(phinSua).sizeName("S").extraPrice(BigDecimal.ZERO).build());
        phinSua.getSizes().add(ProductSize.builder().product(phinSua).sizeName("M").extraPrice(BigDecimal.valueOf(10000)).build());
        phinSua.getSizes().add(ProductSize.builder().product(phinSua).sizeName("L").extraPrice(BigDecimal.valueOf(16000)).build());
        productRepository.save(phinSua);

        // Product 2: Phin Đen Đá
        Product phinDen = Product.builder()
                .category(coffee)
                .name("Phin Đen Đá")
                .description("Cà phê Phin nguyên chất đậm đà, chuẩn gu người Việt.")
                .basePrice(BigDecimal.valueOf(29000))
                .imageUrl("https://highlandscoffee.com.vn/vnt_upload/product/04_2023/thumbs/270_crop_PHIN_DEN_DA.jpg")
                .isActive(true)
                .build();
        phinDen.getSizes().add(ProductSize.builder().product(phinDen).sizeName("S").extraPrice(BigDecimal.ZERO).build());
        phinDen.getSizes().add(ProductSize.builder().product(phinDen).sizeName("M").extraPrice(BigDecimal.valueOf(10000)).build());
        phinDen.getSizes().add(ProductSize.builder().product(phinDen).sizeName("L").extraPrice(BigDecimal.valueOf(16000)).build());
        productRepository.save(phinDen);

        // Product 3: Trà Sen Vàng
        Product traSen = Product.builder()
                .category(tea)
                .name("Trà Sen Vàng")
                .description("Sự kết hợp hoàn hảo giữa hương trà thanh mát, hạt sen bùi ngậy và lớp củ năng giòn ngọt.")
                .basePrice(BigDecimal.valueOf(45000))
                .imageUrl("https://highlandscoffee.com.vn/vnt_upload/product/04_2023/thumbs/270_crop_TRA_SEN_VANG_OT.jpg")
                .isActive(true)
                .build();
        traSen.getSizes().add(ProductSize.builder().product(traSen).sizeName("S").extraPrice(BigDecimal.ZERO).build());
        traSen.getSizes().add(ProductSize.builder().product(traSen).sizeName("M").extraPrice(BigDecimal.valueOf(10000)).build());
        traSen.getSizes().add(ProductSize.builder().product(traSen).sizeName("L").extraPrice(BigDecimal.valueOf(16000)).build());
        productRepository.save(traSen);

        // Product 4: Freeze Trà Xanh
        Product freezeMatcha = Product.builder()
                .category(freeze)
                .name("Freeze Trà Xanh")
                .description("Thức uống đá xay thơm lừng vị matcha Nhật Bản kết hợp cùng thạch trà xanh giòn dai.")
                .basePrice(BigDecimal.valueOf(55000))
                .imageUrl("https://highlandscoffee.com.vn/vnt_upload/product/04_2023/thumbs/270_crop_FREEZE_TRA_XANH.jpg")
                .isActive(true)
                .build();
        freezeMatcha.getSizes().add(ProductSize.builder().product(freezeMatcha).sizeName("S").extraPrice(BigDecimal.ZERO).build());
        freezeMatcha.getSizes().add(ProductSize.builder().product(freezeMatcha).sizeName("M").extraPrice(BigDecimal.valueOf(10000)).build());
        freezeMatcha.getSizes().add(ProductSize.builder().product(freezeMatcha).sizeName("L").extraPrice(BigDecimal.valueOf(16000)).build());
        productRepository.save(freezeMatcha);

        // Product 5: Bánh Mì Que Pate
        Product banhMi = Product.builder()
                .category(cake)
                .name("Bánh Mì Que Pate")
                .description("Bánh mì giòn rụm với nhân pate béo ngậy chuẩn vị.")
                .basePrice(BigDecimal.valueOf(19000))
                .imageUrl("https://highlandscoffee.com.vn/vnt_upload/product/04_2023/thumbs/270_crop_BMQ_PATE.jpg")
                .isActive(true)
                .build();
        banhMi.getSizes().add(ProductSize.builder().product(banhMi).sizeName("Tiêu chuẩn").extraPrice(BigDecimal.ZERO).build());
        productRepository.save(banhMi);
    }
}
