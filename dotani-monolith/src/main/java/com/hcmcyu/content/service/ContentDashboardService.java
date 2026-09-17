package com.hcmcyu.content.service;

import com.hcmcyu.content.dto.ContentDashboardSummaryResponse;
import com.hcmcyu.content.dto.PostResponse;
import com.hcmcyu.content.entity.PostStatus;
import com.hcmcyu.content.entity.PostType;
import com.hcmcyu.content.mapper.PostMapper;
import com.hcmcyu.content.repository.PostRepository;
import com.hcmcyu.content.repository.PostSpecifications;
import com.hcmcyu.content.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentDashboardService {

    private static final int DASHBOARD_POST_LIMIT = 5;
    private static final int RECENT_DAYS = 30;

    private final PostRepository postRepository;
    private final PostMapper postMapper;

    public ContentDashboardService(PostRepository postRepository, PostMapper postMapper) {
        this.postRepository = postRepository;
        this.postMapper = postMapper;
    }

    @Transactional(readOnly = true)
    public ContentDashboardSummaryResponse getSummary(CurrentUser currentUser) {
        Specification<com.hcmcyu.content.entity.Post> visiblePublished = Specification
                .where(PostSpecifications.visibleTo(currentUser))
                .and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), PostStatus.PUBLISHED));

        long recentActivityReportCount = postRepository.count(visiblePublished
                .and(PostSpecifications.hasType(PostType.ACTIVITY_REPORT))
                .and(PostSpecifications.createdAfter(LocalDateTime.now().minusDays(RECENT_DAYS))));

        List<PostResponse> newPosts = postRepository.findAll(
                        visiblePublished,
                        PageRequest.of(0, DASHBOARD_POST_LIMIT, Sort.by("createdAt").descending())
                )
                .map(postMapper::toResponse)
                .toList();

        return new ContentDashboardSummaryResponse(recentActivityReportCount, newPosts);
    }
}
