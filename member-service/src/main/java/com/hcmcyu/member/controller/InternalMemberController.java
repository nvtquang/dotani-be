package com.hcmcyu.member.controller;

import com.hcmcyu.member.dto.InternalMemberProfileCompletionRequest;
import com.hcmcyu.member.dto.InternalMemberRegistrationRequest;
import com.hcmcyu.member.dto.MemberDisplayNameRequest;
import com.hcmcyu.member.dto.MemberDisplayNameResponse;
import com.hcmcyu.member.dto.MemberResponse;
import com.hcmcyu.member.service.InternalMemberDirectoryService;
import com.hcmcyu.member.service.InternalMemberRegistrationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/members")
public class InternalMemberController {

    private final InternalMemberRegistrationService internalMemberRegistrationService;
    private final InternalMemberDirectoryService internalMemberDirectoryService;

    public InternalMemberController(
            InternalMemberRegistrationService internalMemberRegistrationService,
            InternalMemberDirectoryService internalMemberDirectoryService
    ) {
        this.internalMemberRegistrationService = internalMemberRegistrationService;
        this.internalMemberDirectoryService = internalMemberDirectoryService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse registerMemberProfile(
            @Valid @RequestBody InternalMemberRegistrationRequest request,
            @RequestHeader(name = "X-Internal-Secret", required = false) String internalSecret
    ) {
        return internalMemberRegistrationService.registerMemberProfile(request, internalSecret);
    }

    @PutMapping("/{memberId}/profile-completion")
    public MemberResponse completeMemberProfile(
            @PathVariable String memberId,
            @Valid @RequestBody InternalMemberProfileCompletionRequest request,
            @RequestHeader(name = "X-Internal-Secret", required = false) String internalSecret
    ) {
        return internalMemberRegistrationService.completeMemberProfile(memberId, request, internalSecret);
    }

    @PostMapping("/display-names")
    public List<MemberDisplayNameResponse> findDisplayNames(
            @Valid @RequestBody MemberDisplayNameRequest request,
            @RequestHeader(name = "X-Internal-Secret", required = false) String internalSecret
    ) {
        return internalMemberDirectoryService.findDisplayNames(request, internalSecret);
    }
}
