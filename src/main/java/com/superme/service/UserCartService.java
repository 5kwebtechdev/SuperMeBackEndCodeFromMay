package com.superme.service;

import com.superme.dto.AddToCartRequest;
import com.superme.dto.CartItemDto;
import com.superme.dto.CartSummaryDto;
import com.superme.dto.UpdateCartItemRequest;
import com.superme.model.CartItem;
import com.superme.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserCartService {

    private final CartItemRepository cartItemRepository;
    // TODO: inject product pricing + auth service


    public CartSummaryDto getCart(Long userId, Long schoolId, List<Long> classIdList) {
        // Accepts userId, schoolId, and classId (list) from controller
        List<CartItem> items = classIdList.stream()
            .map(classId -> cartItemRepository.findByUserIdAndSchoolIdAndClassId(userId, schoolId, classId))
            .flatMap(List::stream)
            .toList();
        return buildSummary(items);
    }

    public CartSummaryDto addItem(AddToCartRequest request) {
//        Long userId = getCurrentUserId();

        // TODO: fetch product price from Product table; for now assume quantity * 0
        BigDecimal unitPrice = BigDecimal.ZERO;

        CartItem item = CartItem.builder()
                .userId(Long.valueOf(request.getUserId()))
                .schoolId(request.getSchoolId())
                .classId(request.getClassId())
                .productId(request.getProductId())
                .sizeLabel(request.getSizeLabel())
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .lineTotal(unitPrice.multiply(BigDecimal.valueOf(request.getQuantity())))
                .build();

        cartItemRepository.save(item);

        List<CartItem> items = cartItemRepository.findByUserIdAndSchoolIdAndClassId(
                Long.valueOf(request.getUserId()), request.getSchoolId(), request.getClassId());
        return buildSummary(items);
    }

    public CartSummaryDto updateItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found: " + itemId));

        if (!item.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized cart item");
        }

        if (request.getSizeLabel() != null) {
            item.setSizeLabel(request.getSizeLabel());
        }
        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }
        item.setLineTotal(item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity())));

        cartItemRepository.save(item);

        List<CartItem> items = cartItemRepository.findByUserIdAndSchoolIdAndClassId(
                userId, item.getSchoolId(), item.getClassId());
        return buildSummary(items);
    }

    public void removeItem(Long itemId) {
        cartItemRepository.deleteById(itemId);
    }

    public void clearCart(Long userId, Long schoolId, List<Long> classIdList) {
        classIdList.forEach(classId -> cartItemRepository.deleteByUserIdAndSchoolIdAndClassId(userId, schoolId, classId));
    }

    // ===== helpers =====

    private CartSummaryDto buildSummary(List<CartItem> items) {
        List<CartItemDto> itemDtos = items.stream()
                .map(this::toDto)
                .toList();

        BigDecimal itemsTotal = items.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountTotal = BigDecimal.ZERO; // plug promotions later
        BigDecimal payableTotal = itemsTotal.subtract(discountTotal);

        return CartSummaryDto.builder()
                .items(itemDtos)
                .itemsTotal(itemsTotal)
                .discountTotal(discountTotal)
                .payableTotal(payableTotal)
                .build();
    }

    private CartItemDto toDto(CartItem item) {
        return CartItemDto.builder()
                .id(item.getId())
                .productId(item.getProductId())
                // these need joins to Product / School; keep null for now
                .productName(null)
                .thumbnailUrl(null)
                .categoryName(null)
                .schoolName(null)
                .sizeLabel(item.getSizeLabel())
                .quantity(item.getQuantity())
                // treat unitPrice as sellingPrice
                .mrp(item.getUnitPrice())          // or real MRP if you add it to entity
                .sellingPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .inStock(true)                     // or compute from inventory
                .build();
    }

}
