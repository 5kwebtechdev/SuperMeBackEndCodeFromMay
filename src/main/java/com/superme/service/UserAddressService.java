package com.superme.service;

import com.superme.dto.AddressDto;
import com.superme.dto.SaveAddressRequest;
import com.superme.model.UserAddress;
import com.superme.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserAddressService {

    private final UserAddressRepository userAddressRepository;

    // TODO: replace with actual auth logic
    private Long getCurrentUserId() {
        return 1L;
    }

    public List<AddressDto> listAddresses() {
        Long userId = getCurrentUserId();
        List<UserAddress> entities =
                userAddressRepository.findByUserIdOrderByIsDefaultDescIdDesc(userId);
        return entities.stream().map(this::toDto).toList();
    }

    public AddressDto createAddress(SaveAddressRequest request) {
        Long userId = getCurrentUserId();

        UserAddress entity = new UserAddress();
        entity.setUserId(userId);
        apply(request, entity);

        // first address for user → set default
        if (!userAddressRepository.existsByUserId(userId)) {
            entity.setIsDefault(true);
        } else if (entity.getIsDefault() == null) {
            entity.setIsDefault(false);
        }

        UserAddress saved = userAddressRepository.save(entity);
        return toDto(saved);
    }

    public AddressDto updateAddress(Long id, SaveAddressRequest request) {
        System.out.println(request.getId()+" request.getId()");
        System.out.println(request.getUserId()+" request.getUserId()");
        UserAddress entity = userAddressRepository.findByIdAndUserId(request.getId(),request.getUserId())
                .orElseThrow(() -> new RuntimeException("Address not found: " + id));

        apply(request, entity);
        UserAddress saved = userAddressRepository.save(entity);
        return toDto(saved);
    }

    public void deleteAddress(Long id) {
        Long userId = getCurrentUserId();

        UserAddress entity = userAddressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Address not found: " + id));

        boolean wasDefault = Boolean.TRUE.equals(entity.getIsDefault());
        userAddressRepository.delete(entity);

        if (wasDefault) {
            userAddressRepository.findFirstByUserIdOrderByIsDefaultDescIdDesc(userId)
                    .ifPresent(addr -> {
                        addr.setIsDefault(true);
                        userAddressRepository.save(addr);
                    });
        }
    }

    public void setDefaultAddress(Long id,Long userId) {
//        Long userId = getCurrentUserId();

        UserAddress target = userAddressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Address not found: " + id));

        userAddressRepository.clearDefaultForUser(userId);

        target.setIsDefault(true);
        userAddressRepository.save(target);
    }

    // ===== helpers =====

    private void apply(SaveAddressRequest req, UserAddress e) {
        e.setReceiverName(req.getReceiverName());
        e.setMobile(req.getMobile());
        e.setLine1(req.getLine1());
        e.setLine2(req.getLine2());
        e.setCity(req.getCity());
        e.setState(req.getState());
        e.setPostalCode(req.getPostalCode());
        e.setLatitude(req.getLatitude());
        e.setLongitude(req.getLongitude());
    }

    private AddressDto toDto(UserAddress e) {
        return AddressDto.builder()
                .id(e.getId())
                .receiverName(e.getReceiverName())
                .mobile(e.getMobile())
                .line1(e.getLine1())
                .line2(e.getLine2())
                .city(e.getCity())
                .state(e.getState())
                .postalCode(e.getPostalCode())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .isDefault(e.getIsDefault())
                .build();
    }
}



