package com.superme.admin.controller;

import com.superme.dto.ProductFormRequest;
import com.superme.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    // Products grid
    @GetMapping
    public ResponseEntity<List<ProductFormRequest>> listProducts() {
        return ResponseEntity.ok(productService.list());
    }

    // Load existing product into Edit modal
    @GetMapping("/{id}")
    public ResponseEntity<ProductFormRequest> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    // Add New Product
    @PostMapping
    public ResponseEntity<Long> createProduct(@RequestBody ProductFormRequest request) {
        return ResponseEntity.ok(productService.create(request));
    }

    // Edit Product
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateProduct(@PathVariable Long id,
                                              @RequestBody ProductFormRequest request) {
        productService.update(id, request);
        return ResponseEntity.noContent().build();
    }

    // Delete Product (trash icon)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
