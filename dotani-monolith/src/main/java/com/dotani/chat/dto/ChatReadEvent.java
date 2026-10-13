package com.dotani.chat.dto;

import java.time.Instant;

/** Sent to /topic/conversations/{id}/read when a member reads the conversation. */
public record ChatReadEvent(String memberId, Instant lastReadAt) {
}
