package com.superme.controller;

import com.superme.dto.*;
import com.superme.service.UserOrderService;   // changed package
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/orders")
@RequiredArgsConstructor
public class UserOrderController {

    private final UserOrderService userOrderService;

    @GetMapping
    public Page<UserOrderListItemDto> listUserOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return userOrderService.listOrders(PageRequest.of(page, size));
    }

    @GetMapping("/{orderId}")
    public UserOrderDetailDto getOrder(@PathVariable Long orderId) {
        return userOrderService.getOrder(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public void cancelOrder(@PathVariable Long orderId,
                            @RequestBody UserCancelOrderRequest request) {
        userOrderService.cancelOrder(orderId, request);
    }

    @PostMapping("/{orderId}/return")
    public void createReturnOrExchange(@PathVariable Long orderId,
                                       @RequestBody UserReturnOrExchangeRequest request) {
        userOrderService.createReturnOrExchange(orderId, request);
    }
}
