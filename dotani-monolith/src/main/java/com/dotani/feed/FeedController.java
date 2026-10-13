package com.dotani.feed;

import com.dotani.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Feed", description = "Mixed news feed and global search.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT Bearer token")
})
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping("/api/feed")
    @Operation(summary = "Feed", description = "Events and posts visible to the caller, newest first. Infinite scroll: pass the returned nextCursor as before.")
    public FeedResponse feed(
            @RequestParam(required = false, name = "before") Instant before,
            @RequestParam(required = false, name = "size", defaultValue = "10") int size,
            @RequestParam(required = false, name = "kind", defaultValue = "ALL") FeedKind kind,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return feedService.feed(before, size, kind, currentUser);
    }

    @GetMapping("/api/search")
    @Operation(summary = "Global search", description = "Up to 5 members (directory fields only), events and posts whose name/title contains q (min 2 chars), within the caller's scope.")
    public SearchResponse search(
            @RequestParam(name = "q") String query,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return feedService.search(query, currentUser);
    }
}
