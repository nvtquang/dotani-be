package com.dotani.feed;

import com.dotani.content.service.PostService;
import com.dotani.event.service.EventService;
import com.dotani.member.service.MemberService;
import com.dotani.shared.security.CurrentUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class FeedService {

    static final int MAX_PAGE_SIZE = 50;
    static final int SEARCH_LIMIT = 5;

    private final EventService eventService;
    private final PostService postService;
    private final MemberService memberService;

    public FeedService(EventService eventService, PostService postService, MemberService memberService) {
        this.eventService = eventService;
        this.postService = postService;
        this.memberService = memberService;
    }

    /**
     * Keyset page of events and posts, newest first. Each source is asked for {@code size} items
     * older than the cursor, the two lists are merged and cut to {@code size}; the createdAt of the
     * last item becomes the next cursor.
     */
    public FeedResponse feed(Instant before, int size, FeedKind kind, CurrentUser currentUser) {
        int limit = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        List<FeedItem> merged = new ArrayList<>();
        if (kind != FeedKind.POST) {
            eventService.findCreatedBefore(before, limit, currentUser).forEach(event -> merged.add(FeedItem.of(event)));
        }
        if (kind != FeedKind.EVENT) {
            postService.findCreatedBefore(before, limit, currentUser).forEach(post -> merged.add(FeedItem.of(post)));
        }
        merged.sort(Comparator.comparing(FeedItem::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));

        boolean hasMore = merged.size() > limit;
        List<FeedItem> page = hasMore ? merged.subList(0, limit) : merged;
        // a full page from one source may still have older items behind it
        boolean mayHaveMore = hasMore || page.size() == limit;
        Instant nextCursor = mayHaveMore && !page.isEmpty() ? page.get(page.size() - 1).createdAt() : null;
        return new FeedResponse(List.copyOf(page), nextCursor);
    }

    /** Quick search for the Ctrl+K palette: a few members, events and posts the caller may see. */
    public SearchResponse search(String query, CurrentUser currentUser) {
        String text = query == null ? "" : query.trim();
        if (text.length() < 2) {
            return new SearchResponse(List.of(), List.of(), List.of());
        }
        return new SearchResponse(
                memberService.findDirectory(text, PageRequest.of(0, SEARCH_LIMIT)).getContent(),
                eventService.searchByTitle(text, SEARCH_LIMIT, currentUser),
                postService.searchByTitle(text, SEARCH_LIMIT, currentUser)
        );
    }
}
