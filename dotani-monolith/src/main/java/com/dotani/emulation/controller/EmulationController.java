package com.dotani.emulation.controller;

import com.dotani.emulation.dto.EmulationDtos.AwardRequest;
import com.dotani.emulation.dto.EmulationDtos.AwardResponse;
import com.dotani.emulation.dto.EmulationDtos.BoardRow;
import com.dotani.emulation.dto.EmulationDtos.ProfileResponse;
import com.dotani.emulation.dto.EmulationDtos.RatingRequest;
import com.dotani.emulation.dto.EmulationDtos.RatingResponse;
import com.dotani.emulation.dto.EmulationDtos.SummaryResponse;
import com.dotani.emulation.service.EmulationService;
import com.dotani.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/emulation")
@Tag(name = "Emulation", description = "Hồ sơ thi đua: khen thưởng, kỷ luật và đánh giá xếp loại đoàn viên hằng năm.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error or year out of range"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT Bearer token"),
        @ApiResponse(responseCode = "403", description = "Role or organization scope denied"),
        @ApiResponse(responseCode = "404", description = "Member, award or rating not found")
})
public class EmulationController {

    private final EmulationService emulationService;

    public EmulationController(EmulationService emulationService) {
        this.emulationService = emulationService;
    }

    @GetMapping("/members/{memberId}")
    @Operation(summary = "Member's emulation record", description = "Ratings and decisions of a member. Members read their own; officers those in scope.")
    public ProfileResponse profile(@PathVariable("memberId") String memberId, @AuthenticationPrincipal CurrentUser currentUser) {
        return emulationService.profile(memberId, currentUser);
    }

    @PostMapping("/awards")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a commendation or discipline")
    public AwardResponse createAward(@Valid @RequestBody AwardRequest request, @AuthenticationPrincipal CurrentUser currentUser) {
        return emulationService.createAward(request, currentUser);
    }

    @PutMapping("/awards/{id}")
    @Operation(summary = "Update a commendation or discipline")
    public AwardResponse updateAward(
            @PathVariable("id") String id,
            @Valid @RequestBody AwardRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return emulationService.updateAward(id, request, currentUser);
    }

    @DeleteMapping("/awards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a commendation or discipline")
    public void deleteAward(@PathVariable("id") String id, @AuthenticationPrincipal CurrentUser currentUser) {
        emulationService.deleteAward(id, currentUser);
    }

    @PutMapping("/ratings")
    @Operation(summary = "Rate a member for a year", description = "Creates or replaces the member's rating for that year.")
    public RatingResponse rate(@Valid @RequestBody RatingRequest request, @AuthenticationPrincipal CurrentUser currentUser) {
        return emulationService.rate(request, currentUser);
    }

    @DeleteMapping("/ratings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a rating")
    public void deleteRating(@PathVariable("id") String id, @AuthenticationPrincipal CurrentUser currentUser) {
        emulationService.deleteRating(id, currentUser);
    }

    @GetMapping("/ratings")
    @Operation(summary = "Rating board of a year", description = "Officers only: active members in scope with their rating for the year.")
    public Page<BoardRow> board(
            @RequestParam("year") int year,
            @RequestParam(name = "organizationId", required = false) String organizationId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return emulationService.board(year, organizationId, keyword, pageable, currentUser);
    }

    @GetMapping("/summary")
    @Operation(summary = "Emulation summary of a year", description = "Officers only: members per rating, commendations and disciplines of the year.")
    public SummaryResponse summary(
            @RequestParam("year") int year,
            @RequestParam(name = "organizationId", required = false) String organizationId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return emulationService.summary(year, organizationId, currentUser);
    }
}
