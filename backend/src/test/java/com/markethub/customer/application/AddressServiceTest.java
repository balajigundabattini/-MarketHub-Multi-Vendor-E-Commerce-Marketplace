package com.markethub.customer.application;

import java.util.Optional;
import com.markethub.customer.infrastructure.jpa.AddressRepository;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddressServiceTest {
    @Test
    void looksUpAddressWithinAuthenticatedUsersScope() {
        AddressRepository addresses = mock(AddressRepository.class);
        UserRepository users = mock(UserRepository.class);
        when(users.findByIdForUpdate(12L)).thenReturn(Optional.of(new UserEntity("owner@example.com", "hash", "Owner")));
        when(addresses.findActiveOwned(99L, 12L)).thenReturn(Optional.empty());
        AddressService service = new AddressService(addresses, users);
        assertThatThrownBy(() -> service.delete(12L, 99L)).isInstanceOf(ResponseStatusException.class);
        verify(addresses).findActiveOwned(99L, 12L);
    }
}
