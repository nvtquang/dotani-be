package com.hcmcyu.member.repository;

import com.hcmcyu.member.entity.MemberRoleChangeAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRoleChangeAuditRepository extends JpaRepository<MemberRoleChangeAudit, String> {
}
