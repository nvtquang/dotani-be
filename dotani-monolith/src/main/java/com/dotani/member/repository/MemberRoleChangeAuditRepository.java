package com.dotani.member.repository;

import com.dotani.member.entity.MemberRoleChangeAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRoleChangeAuditRepository extends JpaRepository<MemberRoleChangeAudit, String> {
}
