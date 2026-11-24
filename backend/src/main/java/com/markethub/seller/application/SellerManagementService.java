package com.markethub.seller.application;

import java.text.Normalizer;
import java.util.Locale;

import com.markethub.identity.infrastructure.jpa.RoleEntity;
import com.markethub.identity.infrastructure.jpa.RoleRepository;
import com.markethub.identity.infrastructure.jpa.UserEntity;
import com.markethub.identity.infrastructure.jpa.UserRepository;
import com.markethub.identity.infrastructure.jpa.UserRoleEntity;
import com.markethub.identity.infrastructure.jpa.UserRoleRepository;
import com.markethub.seller.api.SellerApplicationRequest;
import com.markethub.seller.api.SellerApplicationResponse;
import com.markethub.seller.api.SellerReviewRequest;
import com.markethub.seller.api.StoreResponse;
import com.markethub.seller.api.StoreUpdateRequest;
import com.markethub.seller.domain.SellerStatus;
import com.markethub.seller.infrastructure.jpa.SellerEntity;
import com.markethub.seller.infrastructure.jpa.SellerRepository;
import com.markethub.seller.infrastructure.jpa.StoreEntity;
import com.markethub.seller.infrastructure.jpa.StoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SellerManagementService {
    private final SellerRepository sellers;
    private final StoreRepository stores;
    private final UserRepository users;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;

    public SellerManagementService(SellerRepository sellers, StoreRepository stores,
            UserRepository users, RoleRepository roles, UserRoleRepository userRoles) {
        this.sellers = sellers;
        this.stores = stores;
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
    }

    @Transactional(readOnly = true)
    public SellerApplicationResponse getMyApplication(Long userId) {
        return sellers.findByUserId(userId).map(this::toApplication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No seller application was found"));
    }

    @Transactional
    public SellerApplicationResponse apply(Long userId, SellerApplicationRequest request) {
        if (sellers.existsByUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A seller application already exists for this account");
        }
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account was not found"));
        SellerEntity seller = sellers.save(new SellerEntity(user, request.legalName().trim(), request.businessName().trim()));
        return toApplication(seller);
    }

    @Transactional(readOnly = true)
    public Page<SellerApplicationResponse> listApplications(SellerStatus status, Pageable pageable) {
        Page<SellerEntity> page = status == null ? sellers.findAll(pageable) : sellers.findAllByStatus(status, pageable);
        return page.map(this::toApplication);
    }

    @Transactional
    public SellerApplicationResponse review(Long sellerId, Long adminId, SellerReviewRequest request) {
        if (request.status() != SellerStatus.APPROVED && request.status() != SellerStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seller applications can only be approved or rejected");
        }
        SellerEntity seller = sellers.findByIdWithUserForUpdate(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller application was not found"));
        if (seller.getStatus() != SellerStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This application has already been reviewed");
        }
        UserEntity admin = users.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Administrator was not found"));
        String note = request.reason() == null ? null : request.reason().trim();
        seller.review(request.status(), admin, note);
        if (request.status() == SellerStatus.APPROVED) {
            RoleEntity sellerRole = roles.findByCode("SELLER")
                    .orElseThrow(() -> new IllegalStateException("SELLER role is missing from the role table"));
            userRoles.save(new UserRoleEntity(seller.getUser(), sellerRole));
            String storeName = seller.getBusinessName();
            String slug = slugify(storeName) + "-" + seller.getId();
            stores.save(new StoreEntity(seller, storeName, slug));
        }
        return toApplication(seller);
    }

    @Transactional(readOnly = true)
    public StoreResponse getMyStore(Long userId) {
        SellerEntity seller = approvedSeller(userId);
        return stores.findBySellerId(seller.getId()).map(this::toStore)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store was not found"));
    }

    @Transactional
    public StoreResponse updateMyStore(Long userId, StoreUpdateRequest request) {
        SellerEntity seller = approvedSeller(userId);
        StoreEntity store = stores.findBySellerId(seller.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store was not found"));
        store.updateProfile(request.name().trim(), request.description() == null ? null : request.description().trim());
        return toStore(store);
    }

    private SellerEntity approvedSeller(Long userId) {
        SellerEntity seller = sellers.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Approved seller access is required"));
        if (seller.getStatus() != SellerStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Approved seller access is required");
        }
        return seller;
    }

    private SellerApplicationResponse toApplication(SellerEntity seller) {
        UserEntity user = seller.getUser();
        return new SellerApplicationResponse(seller.getId(), seller.getLegalName(), seller.getBusinessName(),
                seller.getStatus().name(), seller.getReviewNote(), seller.getCreatedAt(), seller.getReviewedAt(),
                user.getEmail(), user.getDisplayName());
    }

    private StoreResponse toStore(StoreEntity store) {
        return new StoreResponse(store.getId(), store.getName(), store.getSlug(), store.getDescription(), store.getStatus());
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return slug.isBlank() ? "store" : slug;
    }
}
