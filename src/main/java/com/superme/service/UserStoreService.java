package com.superme.service;

import com.superme.dto.*;
import com.superme.model.Product;
import com.superme.model.ProductCategory;
import com.superme.model.ProductSizeRow;
import com.superme.model.School;
import com.superme.repository.ProductCategoryRepository;
import com.superme.repository.ProductRepository;
import com.superme.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserStoreService {

    private final SchoolRepository schoolRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductRepository productRepository;

    public StoreHomeResponse getHome(Long schoolId, List<String> className) {
        String classNameStr = className != null && !className.isEmpty() ? className.get(0) : null;

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found: " + schoolId));

        // categories are global
        List<ProductCategory> categories = productCategoryRepository.findAll();

        // featured + recommended (for now both same query)
        List<Product> featured =
                productRepository.findTop10BySchoolAndClassNameAndMarkAsMandatoryItemTrueOrderByIdDesc(
                        school, classNameStr);

        List<Product> recommended =
                productRepository.findTop10BySchoolAndClassNameOrderByIdDesc(
                        school, classNameStr);

        return StoreHomeResponse.builder()
                .categories(categories.stream().map(this::toCategoryDto).toList())
                .featured(featured.stream().map(p -> toProductCardDto(p, school)).toList())
                .recommended(recommended.stream().map(p -> toProductCardDto(p, school)).toList())
                .build();
    }

    public Iterable<StoreCategoryDto> getCategoriesForClass(Long schoolId, List<String> className) {
        // for now ignore schoolId/className; categories are global
        List<ProductCategory> categories = productCategoryRepository.findAll();
        return categories.stream().map(this::toCategoryDto).toList();
    }

    public Page<ProductCardDto> listProducts(Long schoolId,
                                             String className,
                                             Long categoryId,
                                             String gender,
                                             String search,
                                             Pageable pageable) {

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found: " + schoolId));

        Page<Product> page;
        boolean hasSearch = search != null && !search.trim().isEmpty();

        if (hasSearch) {
            page = productRepository.findBySchoolAndClassNameAndProductNameContainingIgnoreCase(
                    school, className, search, pageable);
        } else {
            page = productRepository.findBySchoolAndClassName(
                    school, className, pageable);
        }

        return page.map(p -> toProductCardDto(p, school));
    }

    public ProductDetailDto getProductDetail(Long productId, Long schoolId, List<String> className) {
        String classNameStr = className != null && !className.isEmpty() ? className.get(0) : null;

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found: " + schoolId));

        Product product = productRepository.findByIdAndSchool(productId, school)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        return toProductDetailDto(product, school, classNameStr);
    }

    // ===== mapping helpers =====

    private StoreCategoryDto toCategoryDto(ProductCategory c) {
        return StoreCategoryDto.builder()
                .id(c.getId())
                .name(c.getName())
                .iconUrl(null)   // ProductCategory has no icon field yet
                .build();
    }

    private ProductCardDto toProductCardDto(Product p, School school) {
        return ProductCardDto.builder()
                .id(p.getId())
                .name(p.getProductName())
                .thumbnailUrl(
                        p.getProductImages() != null && !p.getProductImages().isEmpty()
                                ? p.getProductImages().get(0)
                                : null
                )
                .categoryName(p.getCategory())
                .schoolName(school != null ? school.getSchoolName() : null)
                .tag(p.getProductTag())
                .mrp(BigDecimal.valueOf(p.getMrp()))
                .sellingPrice(BigDecimal.valueOf(p.getSellingPrice()))
                .discountText(p.getDiscountText())
                .mandatory(p.isMarkAsMandatoryItem())
                .inStock(p.getStock() > 0)
                .build();
    }

    private ProductSizeOptionDto toSizeOptionDto(ProductSizeRow row) {
        return ProductSizeOptionDto.builder()
                .sizeLabel(row.getSize())
                .mrp(BigDecimal.valueOf(row.getMrp()))
                .sellingPrice(BigDecimal.valueOf(row.getSellingPrice()))
                .stock(row.getStock())
                .available(row.getStock() > 0)
                .build();
    }

    private ProductDetailDto toProductDetailDto(Product p, School school, String className) {
        return ProductDetailDto.builder()
                .id(p.getId())
                .name(p.getProductName())
                .description(p.getDescription())
                .details(p.getProductDetails())
                .categoryName(p.getCategory())
                .schoolName(school != null ? school.getSchoolName() : null)
                .tag(p.getProductTag())
                .ageGroup(p.getAgeGroup())
                .mandatory(p.isMarkAsMandatoryItem())
                .images(p.getProductImages())
                .mrp(BigDecimal.valueOf(p.getMrp()))
                .sellingPrice(BigDecimal.valueOf(p.getSellingPrice()))
                .discountText(p.getDiscountText())
                .gstClass(p.getGstClass())
                .stock(p.getStock())
                .sizeChartTemplate(p.getSizeChartTemplate())
                .sizeChartInImageUrl(p.getSizeChartInImageUrl())
                .sizeChartCmImageUrl(p.getSizeChartCmImageUrl())
                .sizes(
                        p.getSizes() != null
                                ? p.getSizes().stream()
                                .map(this::toSizeOptionDto)
                                .toList()
                                : List.of()
                )
                .build();
    }
}
