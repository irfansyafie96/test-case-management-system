package com.yourproject.tcm.repository;

import com.yourproject.tcm.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByName(String name);
    Optional<Organization> findByDomain(String domain);
    Boolean existsByName(String name);
}
