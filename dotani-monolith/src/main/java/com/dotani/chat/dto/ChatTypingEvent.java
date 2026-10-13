package com.dotani.chat.dto;

/** Sent to /topic/conversations/{id}/typing while a member types. */
public record ChatTypingEvent(String memberId, String memberName) {
}
