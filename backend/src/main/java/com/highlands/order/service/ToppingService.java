package com.highlands.order.service;

import com.highlands.order.dto.ToppingRequest;
import com.highlands.order.dto.ToppingResponse;
import com.highlands.order.exception.ResourceNotFoundException;
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

    public List<ToppingResponse> getAllToppings() {
        return toppingRepository.findAll().stream()
                .map(ToppingResponse::from)
                .toList();
    }

    @Transactional
    public ToppingResponse create(ToppingRequest request) {
        Topping topping = Topping.builder()
                .name(request.name().trim())
                .price(request.price())
                .build();
        return ToppingResponse.from(toppingRepository.save(topping));
    }

    @Transactional
    public ToppingResponse update(Long id, ToppingRequest request) {
        Topping topping = toppingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy topping ID: " + id));
        topping.setName(request.name().trim());
        topping.setPrice(request.price());
        return ToppingResponse.from(topping);
    }

    @Transactional
    public void delete(Long id) {
        toppingRepository.deleteById(id);
    }
}
