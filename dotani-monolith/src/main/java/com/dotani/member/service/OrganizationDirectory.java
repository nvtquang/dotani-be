package com.dotani.member.service;

import com.dotani.member.repository.OrganizationUnitRepository;
import com.dotani.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Lets other modules (events, posts) validate organization ids before referencing them. */
@Service
public class OrganizationDirectory {

    private final OrganizationUnitRepository organizationUnitRepository;

    public OrganizationDirectory(OrganizationUnitRepository organizationUnitRepository) {
        this.organizationUnitRepository = organizationUnitRepository;
    }

    public void requireExists(String organizationId) {
        if (organizationId == null || !organizationUnitRepository.existsById(organizationId.trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ORGANIZATION_NOT_FOUND", "Organization unit not found");
        }
    }
}
