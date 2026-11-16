package com.markethub.customer.infrastructure.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddressRepository extends JpaRepository<AddressEntity, Long> {
    @Query("select a from AddressEntity a where a.user.id = :userId and a.isActive = true order by a.isDefault desc, a.createdAt desc")
    List<AddressEntity> findAllActiveForUser(@Param("userId") Long userId);
    @Query("select a from AddressEntity a where a.id = :id and a.user.id = :userId and a.isActive = true")
    Optional<AddressEntity> findActiveOwned(@Param("id") Long id, @Param("userId") Long userId);
    @Query("select count(a) from AddressEntity a where a.user.id = :userId and a.isActive = true")
    long countActiveForUser(@Param("userId") Long userId);
    @Modifying
    @Query("update AddressEntity a set a.isDefault = false where a.user.id = :userId and a.isActive = true")
    int clearDefaultForUser(@Param("userId") Long userId);
}
