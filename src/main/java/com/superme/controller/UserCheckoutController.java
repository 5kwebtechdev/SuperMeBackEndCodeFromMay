package com.superme.controller;

import com.superme.dto.*;
import com.superme.service.UserCheckoutService;   // <-- updated import
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/checkout")
@RequiredArgsConstructor
public class UserCheckoutController {

    private final UserCheckoutService userCheckoutService;  // <-- concrete @Service

    @PostMapping("/summary")
    public CheckoutSummaryResponse getSummary(@RequestBody CheckoutSummaryRequest request) {
        return userCheckoutService.buildSummary(request);
    }

    @PostMapping("/create-order")
    public CreateOrderResponse createOrder(@RequestBody CreateOrderRequest request) {
        return userCheckoutService.createOrder(request);
    }

    @PostMapping("/payment/callback")
    public void handlePaymentCallback(@RequestBody PaymentCallbackRequest request) {
        userCheckoutService.handlePaymentCallback(request);
    }
}
