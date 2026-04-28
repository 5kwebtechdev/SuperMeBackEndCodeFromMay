package com.superme.controller;

import com.superme.dto.*;
import com.superme.service.UserCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class UserCartController {

    private final UserCartService userCartService;

    // Cart is scoped to user + school + class
    @GetMapping
    public CartSummaryDto getCart(@RequestParam Long userId,
                                  @RequestParam Long schoolId,
                                  @RequestParam List<Long> classId) {
        return userCartService.getCart(userId, schoolId, classId);
    }

    @PostMapping("/items")
    public CartSummaryDto addItem(@RequestBody AddToCartRequest request) {
        return userCartService.addItem(request);
    }

    @PutMapping("/items/{itemId}")
    public CartSummaryDto updateItem(@PathVariable Long itemId,
                                     @RequestParam Long userId,
                                     @RequestBody UpdateCartItemRequest request) {
        return userCartService.updateItem(userId, itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    public void removeItem(@PathVariable Long itemId) {
        userCartService.removeItem(itemId);
    }

    @DeleteMapping
    public void clearCart(@RequestParam Long userId,
                          @RequestParam Long schoolId,
                          @RequestParam List<Long> classId) {
        userCartService.clearCart(userId, schoolId, classId);
    }
}
