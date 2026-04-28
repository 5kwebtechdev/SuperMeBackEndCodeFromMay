package com.superme.controller;

import com.superme.dto.*;
import com.superme.service.UserStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/store")
@RequiredArgsConstructor
public class UserStoreController {

    private final UserStoreService userStoreService;

    @GetMapping("/home")
    public StoreHomeResponse home(@RequestParam Long schoolId,
                                  @RequestParam List<String> className) {


        System.out.println("UserStoreController.home called with schoolId=" + schoolId + " className=" + className);
        return userStoreService.getHome(schoolId, className);
    }

    @GetMapping("/categories")
    public Iterable<StoreCategoryDto> categories(@RequestParam Long schoolId,
                                                 @RequestParam List<String> className) {
        return userStoreService.getCategoriesForClass(schoolId, className);
    }

    @GetMapping("/products")
    public Page<ProductCardDto> listProducts(
            @RequestParam Long schoolId,
            @RequestParam String className,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return userStoreService.listProducts(
                schoolId, className, categoryId, gender, search, PageRequest.of(page, size)
        );
    }

    @GetMapping("/products/{productId}")
    public ProductDetailDto getProduct(
            @PathVariable Long productId,
            @RequestParam Long schoolId,
            @RequestParam List<String> className
    ) {
        return userStoreService.getProductDetail(productId, schoolId, className);
    }
}
