package com.highlands.order.service;

import com.highlands.order.model.Topping;
import com.highlands.order.repository.ToppingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ToppingService {

    private final ToppingRepository toppingRepository;

    public ToppingService(ToppingRepository toppingRepository) {
        this.toppingRepository = toppingRepository;
    }

    public List<Topping> getAllToppings() {
        return toppingRepository.findAll();
    }

    @Transactional
    public Topping create(com.highlands.order.dto.ToppingRequest request) {
        Topping topping = Topping.builder()
                .name(request.name().trim())
                .price(request.price())
                .build();
        return toppingRepository.save(topping);
    }

    @Transactional
    public Topping update(Long id, com.highlands.order.dto.ToppingRequest request) {
        Topping topping = toppingRepository.findById(id)
                .orElseThrow(() -> new com.highlands.order.exception.ResourceNotFoundException("Không tìm thấy topping ID: " + id));
        topping.setName(request.name().trim());
        topping.setPrice(request.price());
        return topping;
    }

    @Transactional
    public void delete(Long id) {
        toppingRepository.deleteById(id);
    }
}
