package com.superme.controller;

import com.superme.dto.AddressDto;
import com.superme.dto.SaveAddressRequest;
import com.superme.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;

    @GetMapping
    public List<AddressDto> listAddresses() {
        return userAddressService.listAddresses();
    }

    @PostMapping
    public AddressDto createAddress(@RequestBody SaveAddressRequest request) {
        return userAddressService.createAddress(request);
    }

    @PutMapping("/{id}")
    public AddressDto updateAddress(@PathVariable Long id,
                                    @RequestBody SaveAddressRequest request) {
        return userAddressService.updateAddress(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteAddress(@PathVariable Long id) {
        userAddressService.deleteAddress(id);
    }

    @PutMapping("/{addressId}/{userId}/default")
    public void setDefaultAddress(@PathVariable Long addressId,@PathVariable Long userId) {
        userAddressService.setDefaultAddress(addressId,userId);
    }

}


