package com.markethub.customer.api;

import com.markethub.customer.infrastructure.jpa.AddressEntity;
public record AddressResponse(Long id, String recipientName, String phone, String line1, String line2,
        String city, String region, String postalCode, String countryCode, String label, boolean isDefault) {
    public static AddressResponse from(AddressEntity address) {
        return new AddressResponse(address.getId(), address.getRecipientName(), address.getPhone(),
                address.getLine1(), address.getLine2(), address.getCity(), address.getRegion(),
                address.getPostalCode(), address.getCountryCode(), address.getLabel(), address.isDefault());
    }
}
