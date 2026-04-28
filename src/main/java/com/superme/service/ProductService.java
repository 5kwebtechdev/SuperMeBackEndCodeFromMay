package com.superme.service;

import com.superme.model.School;
import com.superme.repository.SchoolRepository;
import com.superme.exception.ResourceNotFoundException;
import com.superme.dto.ProductFormRequest;
import com.superme.dto.ProductListItem;
import com.superme.model.Product;
import com.superme.model.ProductSizeRow;
import com.superme.repository.ProductRepository;
import com.superme.repository.ProductSizeRowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSizeRowRepository sizeRowRepository;
    private final SchoolRepository schoolRepository;

    @Transactional(readOnly = true)
    public List<ProductFormRequest> list() {
        return productRepository.findAll()
                .stream()
                .map(this::toFormRequest)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductFormRequest getById(Long id) {
        return toFormRequest(findProduct(id));
    }

    @Transactional
    public Long create(ProductFormRequest request) {
        Product product = new Product();
        mapFromRequest(request, product);
        product = productRepository.save(product);
        syncSizes(product, request.getSizes());
        return product.getId();
    }

    @Transactional
    public void update(Long id, ProductFormRequest request) {
        log.info("Updating product with ID: {}", id);
        Product product = findProduct(id);
        log.info("Existing product details: {}", product);
        
        mapFromRequest(request, product);
        log.info("Mapped product details: {}", product);
        
        productRepository.save(product);
        log.info("Product saved successfully.");
        
        syncSizes(product, request.getSizes());
        log.info("Sizes synchronized successfully.");
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }

    // ── mapping helpers ──

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private void mapFromRequest(ProductFormRequest req, Product p) {
        log.info("Mapping request to product. Request: {}", req);
        p.setCategory(req.getCategory());
        p.setProductImages(req.getProductImages());
        p.setProductName(req.getProductName());
        p.setDescription(req.getDescription());
        p.setProductDetails(req.getProductDetails());
        p.setProductTag(req.getProductTag());

        if (req.getSchoolId() != null) {
            School school = schoolRepository.findById(req.getSchoolId())
                    .orElseThrow(() -> new ResourceNotFoundException("School not found: " + req.getSchoolId()));
            p.setSchool(school);
        } else {
            p.setSchool(null);
        }

        p.setMarkAsMandatoryItem(req.isMarkAsMandatoryItem());
        p.setClassName(req.getClassName());
        p.setAuthorName(req.getAuthorName());
        p.setPublisherName(req.getPublisherName());
        p.setEditionYear(req.getEditionYear());
        p.setAgeGroup(req.getAgeGroup());

        p.setMrp(req.getMrp());
        p.setSellingPrice(req.getSellingPrice());
        p.setDiscountText(req.getDiscountText());
        p.setGstClass(req.getGstClass());
        p.setStock(req.getStock());

        p.setSizeChartTemplate(req.getSizeChartTemplate());
        p.setSizeChartInImageUrl(req.getSizeChartInImageUrl());
        p.setSizeChartCmImageUrl(req.getSizeChartCmImageUrl());
        p.setStatus(req.getStatus());
        log.info("Mapped product: {}", p);
    }

    private void syncSizes(Product product, List<ProductFormRequest.SizeRowInput> sizes) {
        log.info("Synchronizing sizes for product ID: {}", product.getId());
        List<ProductSizeRow> existing = sizeRowRepository.findByProductId(product.getId());
        log.info("Existing sizes: {}", existing);
        
        sizeRowRepository.deleteAll(existing);
        log.info("Deleted existing sizes.");

        if (sizes == null) {
            log.info("No sizes provided in the request.");
            return;
        }

        for (ProductFormRequest.SizeRowInput s : sizes) {
            log.info("Adding size: {}", s);
            ProductSizeRow row = ProductSizeRow.builder()
                    .product(product)
                    .size(s.getSize())
                    .mrp(s.getMrp())
                    .sellingPrice(s.getSellingPrice())
                    .stock(s.getStock())
                    .build();
            sizeRowRepository.save(row);
        }
        log.info("All sizes synchronized.");
    }

    private ProductListItem toListItem(Product p) {
        String schoolName = p.getSchool() != null ? p.getSchool().getSchoolName() : null;
        String thumb = (p.getProductImages() != null && !p.getProductImages().isEmpty())
                ? p.getProductImages().get(0)
                : null;

        return ProductListItem.builder()
                .id(p.getId())
                .category(p.getCategory())
                .schoolName(schoolName)
                .productName(p.getProductName())
                .description(p.getDescription())
                .mrp(p.getMrp())
                .sellingPrice(p.getSellingPrice())
                .stock(p.getStock())
                .status(p.getStatus())
                .productTag(p.getProductTag())
                .thumbnailUrl(thumb)
                .build();
    }

    public ProductFormRequest toFormRequest(Product p) {
        Long schoolId = p.getSchool() != null ? p.getSchool().getId() : null;

        List<ProductFormRequest.SizeRowInput> sizeInputs = p.getSizes() == null ? List.of() :
                p.getSizes().stream()
                        .map(row -> ProductFormRequest.SizeRowInput.builder()
                                .id(row.getId())
                                .size(row.getSize())
                                .mrp(row.getMrp())
                                .sellingPrice(row.getSellingPrice())
                                .stock(row.getStock())
                                .build())
                        .toList();

        return ProductFormRequest.builder()
                .category(p.getCategory())
                .productImages(p.getProductImages())
                .productName(p.getProductName())
                .description(p.getDescription())
                .productDetails(p.getProductDetails())
                .productTag(p.getProductTag())
                .schoolId(schoolId)
                .markAsMandatoryItem(p.isMarkAsMandatoryItem())
                .className(p.getClassName())
                .authorName(p.getAuthorName())
                .publisherName(p.getPublisherName())
                .editionYear(p.getEditionYear())
                .ageGroup(p.getAgeGroup())
                .mrp(p.getMrp())
                .sellingPrice(p.getSellingPrice())
                .discountText(p.getDiscountText())
                .gstClass(p.getGstClass())
                .stock(p.getStock())
                .sizeChartTemplate(p.getSizeChartTemplate())
                .sizes(sizeInputs)
                .sizeChartInImageUrl(p.getSizeChartInImageUrl())
                .sizeChartCmImageUrl(p.getSizeChartCmImageUrl())
                .status(p.getStatus())
                .build();
    }
}
