package com.dotani.feed;

import java.time.Instant;
import java.util.List;

/** @param nextCursor pass as {@code before} to load the next page; null when there is nothing older */
public record FeedResponse(List<FeedItem> items, Instant nextCursor) {
}
