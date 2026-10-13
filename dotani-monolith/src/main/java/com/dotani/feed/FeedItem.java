package com.dotani.feed;

import com.dotani.content.dto.PostResponse;
import com.dotani.event.dto.EventResponse;
import java.time.Instant;

/** One entry of the mixed feed: exactly one of event/post is set, matching kind. */
public record FeedItem(FeedKind kind, Instant createdAt, EventResponse event, PostResponse post) {

    static FeedItem of(EventResponse event) {
        return new FeedItem(FeedKind.EVENT, event.createdAt(), event, null);
    }

    static FeedItem of(PostResponse post) {
        return new FeedItem(FeedKind.POST, post.createdAt(), null, post);
    }
}
