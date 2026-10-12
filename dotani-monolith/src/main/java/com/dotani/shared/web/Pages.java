package com.dotani.shared.web;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Pages an already loaded, already ordered list. Used by list endpoints over small collections (a member's
 * conversations, an organization's members) so every list API has the same page/size contract.
 */
public final class Pages {

    private Pages() {
    }

    public static <T> Page<T> of(List<T> all, Pageable pageable) {
        if (pageable.isUnpaged()) {
            return new PageImpl<>(all);
        }
        int from = (int) Math.min(pageable.getOffset(), all.size());
        int to = Math.min(from + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(from, to), pageable, all.size());
    }
}
