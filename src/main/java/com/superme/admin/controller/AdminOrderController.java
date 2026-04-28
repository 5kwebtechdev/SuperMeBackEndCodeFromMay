package com.superme.admin.controller;

import com.superme.dto.*;
import com.superme.enums.OrderStatus;
import com.superme.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public Page<OrderListItemDto> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        return orderService.listOrders(status, search, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public OrderDetailDto getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    @PostMapping("/manual")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createManualOrder(@RequestBody CreateManualOrderRequest request) {
        return orderService.createManualOrder(request);
    }

    @PostMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(@PathVariable Long id,
                             @RequestBody UpdateOrderStatusRequest request) {
        orderService.updateStatus(id, request);
    }

    @PostMapping("/{id}/accept-pack")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptAndPack(@PathVariable Long id,
                              @RequestBody AcceptAndPackRequest request) {
        orderService.acceptAndPack(id, request);
    }

    @PostMapping("/{id}/mark-shipped")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsShipped(@PathVariable Long id,
                              @RequestBody MarkAsShippedRequest request) {
        orderService.markAsShipped(id, request);
    }

    @PostMapping("/{id}/mark-delivered")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsDelivered(@PathVariable Long id,
                                @RequestBody MarkAsDeliveredRequest request) {
        orderService.markAsDelivered(id, request);
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelOrder(@PathVariable Long id,
                            @RequestBody CancelOrderRequest request) {
        orderService.cancelOrder(id, request);
    }

    @PostMapping("/{id}/initiate-return")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void initiateReturn(@PathVariable Long id,
                               @RequestBody InitiateReturnRequest request) {
        orderService.initiateReturn(id, request);
    }

    @PostMapping("/{id}/return-received")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markReturnReceived(@PathVariable Long id,
                                   @RequestBody ReturnReceivedChecklistRequest request) {
        orderService.markReturnReceived(id, request);
    }

    @PostMapping("/{id}/refund")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refundOrder(@PathVariable Long id,
                            @RequestBody RefundOrderRequest request) {
        orderService.refundOrder(id, request);
    }

    @PostMapping("/{id}/accept-exchange")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptExchange(@PathVariable Long id,
                               @RequestBody AcceptExchangeRequest request) {
        orderService.acceptExchangeRequest(id, request);
    }

    @PostMapping("/{id}/ship-exchange")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void shipExchange(@PathVariable Long id,
                             @RequestBody ShipExchangeRequest request) {
        orderService.shipExchange(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
    }
}
