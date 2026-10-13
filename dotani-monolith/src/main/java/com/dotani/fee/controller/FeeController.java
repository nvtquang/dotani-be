package com.dotani.fee.controller;

import com.dotani.fee.dto.FeeMemberStatusResponse;
import com.dotani.fee.dto.FeePaymentRequest;
import com.dotani.fee.dto.FeePeriodRequest;
import com.dotani.fee.dto.FeePeriodResponse;
import com.dotani.fee.entity.FeePeriodStatus;
import com.dotani.fee.service.FeeService;
import com.dotani.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fees/periods")
@Tag(name = "Fees", description = "Đoàn phí: collection periods, payments, reminders and the collector's QR Banking.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT Bearer token"),
        @ApiResponse(responseCode = "403", description = "Role or organization scope denied"),
        @ApiResponse(responseCode = "404", description = "Fee period, payment or member not found"),
        @ApiResponse(responseCode = "409", description = "Period closed, already paid, or has payments")
})
public class FeeController {

    private final FeeService feeService;

    public FeeController(FeeService feeService) {
        this.feeService = feeService;
    }

    @GetMapping
    @Operation(summary = "List fee periods", description = "Periods of the caller's ward and own TDP, newest first. Each carries the caller's own payment status; officers also get the payment progress of the members they oversee.")
    public Page<FeePeriodResponse> findAll(
            @RequestParam(name = "status", required = false) FeePeriodStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return feeService.findAll(status, pageable, currentUser);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get fee period")
    public FeePeriodResponse findById(@PathVariable("id") String id, @AuthenticationPrincipal CurrentUser currentUser) {
        return feeService.findById(id, currentUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create fee period", description = "Ward officers for the ward or any TDP; TDP officers for their own TDP.")
    public FeePeriodResponse create(@Valid @RequestBody FeePeriodRequest request, @AuthenticationPrincipal CurrentUser currentUser) {
        return feeService.create(request, currentUser);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update fee period", description = "Also opens or closes the period (status).")
    public FeePeriodResponse update(
            @PathVariable("id") String id,
            @Valid @RequestBody FeePeriodRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return feeService.update(id, request, currentUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete fee period", description = "Only while nobody has paid; otherwise close it.")
    public void delete(@PathVariable("id") String id, @AuthenticationPrincipal CurrentUser currentUser) {
        feeService.delete(id, currentUser);
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "Members and their payment", description = "Officers only. Ward officers see every member owing the period, TDP officers the members of their TDP. Filter by paid=true|false and name.")
    public Page<FeeMemberStatusResponse> members(
            @PathVariable("id") String id,
            @RequestParam(name = "paid", required = false) Boolean paid,
            @RequestParam(name = "keyword", required = false) String keyword,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return feeService.findMembers(id, paid, keyword, pageable, currentUser);
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a payment", description = "Amount defaults to the period amount, paidAt to now.")
    public FeeMemberStatusResponse recordPayment(
            @PathVariable("id") String id,
            @Valid @RequestBody FeePaymentRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return feeService.recordPayment(id, request, currentUser);
    }

    @DeleteMapping("/{id}/payments/{paymentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel a recorded payment")
    public void cancelPayment(
            @PathVariable("id") String id,
            @PathVariable("paymentId") String paymentId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        feeService.cancelPayment(id, paymentId, currentUser);
    }

    @PostMapping("/{id}/reminders")
    @Operation(summary = "Remind unpaid members", description = "Sends a notification to every unpaid member the caller oversees. At most once per hour per period.")
    public Map<String, Integer> remind(@PathVariable("id") String id, @AuthenticationPrincipal CurrentUser currentUser) {
        return Map.of("recipientCount", feeService.remindUnpaid(id, currentUser));
    }

    @GetMapping("/{id}/collector-qr")
    @Operation(summary = "Collector's QR Banking image", description = "Readable by everyone who can see the period, so members can transfer the fee.")
    public ResponseEntity<Resource> collectorQr(@PathVariable("id") String id, @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(feeService.loadCollectorQr(id, currentUser));
    }
}
