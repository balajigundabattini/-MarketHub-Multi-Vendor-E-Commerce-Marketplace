package com.markethub.seller.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.markethub.identity.infrastructure.jpa.RoleEntity;
import com.markethub.identity.infrastructure.jpa.RoleRepository;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.identity.infrastructure.jpa.UserRoleEntity;
import com.markethub.identity.infrastructure.jpa.UserRoleRepository;
import com.markethub.seller.api.SellerReviewRequest;
import com.markethub.seller.domain.SellerStatus;
import com.markethub.seller.infrastructure.jpa.SellerEntity;
import com.markethub.seller.infrastructure.jpa.SellerRepository;
import com.markethub.seller.infrastructure.jpa.StoreRepository;
import org.junit.jupiter.api.Test;

class SellerManagementServiceTest {
    private final SellerRepository sellers = mock(SellerRepository.class);
    private final StoreRepository stores = mock(StoreRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final UserRoleRepository userRoles = mock(UserRoleRepository.class);
    private final SellerManagementService service = new SellerManagementService(sellers, stores, users, roles, userRoles);

    @Test
    void approvalGrantsSellerRoleAndCreatesStore() {
        SellerEntity seller = mock(SellerEntity.class);
        UserEntity sellerUser = mock(UserEntity.class);
        UserEntity admin = mock(UserEntity.class);
        RoleEntity sellerRole = mock(RoleEntity.class);
        when(sellers.findByIdWithUserForUpdate(17L)).thenReturn(Optional.of(seller));
        when(seller.getStatus()).thenReturn(SellerStatus.PENDING_REVIEW, SellerStatus.APPROVED);
        when(seller.getId()).thenReturn(17L);
        when(seller.getUser()).thenReturn(sellerUser);
        when(seller.getBusinessName()).thenReturn("North & Pine Goods");
        when(seller.getLegalName()).thenReturn("North Pine LLC");
        when(seller.getReviewNote()).thenReturn("Verified");
        when(seller.getCreatedAt()).thenReturn(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        when(sellerUser.getEmail()).thenReturn("seller@example.test");
        when(sellerUser.getDisplayName()).thenReturn("Seller");
        when(users.findById(91L)).thenReturn(Optional.of(admin));
        when(roles.findByCode("SELLER")).thenReturn(Optional.of(sellerRole));

        var response = service.review(17L, 91L, new SellerReviewRequest(SellerStatus.APPROVED, "Verified"));

        assertEquals("APPROVED", response.status());
        verify(seller).review(SellerStatus.APPROVED, admin, "Verified");
        verify(userRoles).save(any(UserRoleEntity.class));
        verify(stores).save(any());
    }

    @Test
    void rejectionDoesNotGrantSellerRoleOrCreateStore() {
        SellerEntity seller = mock(SellerEntity.class);
        UserEntity sellerUser = mock(UserEntity.class);
        UserEntity admin = mock(UserEntity.class);
        when(sellers.findByIdWithUserForUpdate(17L)).thenReturn(Optional.of(seller));
        when(seller.getStatus()).thenReturn(SellerStatus.PENDING_REVIEW, SellerStatus.REJECTED);
        when(seller.getId()).thenReturn(17L);
        when(seller.getUser()).thenReturn(sellerUser);
        when(seller.getLegalName()).thenReturn("North Pine LLC");
        when(seller.getBusinessName()).thenReturn("North Pine");
        when(seller.getReviewNote()).thenReturn("Missing information");
        when(seller.getCreatedAt()).thenReturn(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        when(sellerUser.getEmail()).thenReturn("seller@example.test");
        when(sellerUser.getDisplayName()).thenReturn("Seller");
        when(users.findById(91L)).thenReturn(Optional.of(admin));

        var response = service.review(17L, 91L, new SellerReviewRequest(SellerStatus.REJECTED, "Missing information"));

        assertEquals("REJECTED", response.status());
        verify(userRoles, never()).save(any());
        verify(stores, never()).save(any());
    }
}
