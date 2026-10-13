package com.dotani.feed;

import com.dotani.content.dto.PostResponse;
import com.dotani.event.dto.EventResponse;
import com.dotani.member.dto.MemberDirectoryResponse;
import java.util.List;

public record SearchResponse(
        List<MemberDirectoryResponse> members,
        List<EventResponse> events,
        List<PostResponse> posts
) {
}
