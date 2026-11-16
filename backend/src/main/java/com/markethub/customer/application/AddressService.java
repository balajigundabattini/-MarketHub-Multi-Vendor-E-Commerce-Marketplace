package com.markethub.customer.application;

import java.util.List;
import com.markethub.customer.api.AddressRequest;
import com.markethub.customer.api.AddressResponse;
import com.markethub.customer.infrastructure.jpa.AddressEntity;
import com.markethub.customer.infrastructure.jpa.AddressRepository;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AddressService {
    private final AddressRepository addresses;
    private final UserRepository users;
    public AddressService(AddressRepository addresses, UserRepository users) {
        this.addresses = addresses; this.users = users;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        return addresses.findAllActiveForUser(userId)
                .stream().map(AddressResponse::from).toList();
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        UserEntity user = lockUser(userId);
        boolean makeDefault = request.isDefault() || addresses.countActiveForUser(userId) == 0;
        if (makeDefault) addresses.clearDefaultForUser(userId);
        AddressEntity address = new AddressEntity(user, request.recipientName(), request.phone(), request.line1(),
                request.line2(), request.city(), request.region(), request.postalCode(), request.countryCode(),
                request.label(), makeDefault);
        return AddressResponse.from(addresses.save(address));
    }

    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        lockUser(userId);
        AddressEntity address = requireOwned(userId, addressId);
        if (request.isDefault()) addresses.clearDefaultForUser(userId);
        address.update(request.recipientName(), request.phone(), request.line1(), request.line2(),
                request.city(), request.region(), request.postalCode(), request.countryCode(), request.label());
        address.setDefault(request.isDefault());
        return AddressResponse.from(address);
    }

    @Transactional
    public AddressResponse makeDefault(Long userId, Long addressId) {
        lockUser(userId);
        AddressEntity address = requireOwned(userId, addressId);
        addresses.clearDefaultForUser(userId);
        address.setDefault(true);
        return AddressResponse.from(address);
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        lockUser(userId);
        requireOwned(userId, addressId).deactivate();
    }

    private UserEntity lockUser(Long userId) {
        return users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }
    private AddressEntity requireOwned(Long userId, Long addressId) {
        return addresses.findActiveOwned(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
    }
}
