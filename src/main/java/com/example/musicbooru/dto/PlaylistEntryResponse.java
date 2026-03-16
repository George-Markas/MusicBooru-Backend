package com.example.musicbooru.dto;

import com.example.musicbooru.model.PlaylistEntry;

import java.time.Instant;

public record PlaylistEntryResponse(
        String id,
        String trackId,
        Instant addedOn
) {
    public static PlaylistEntryResponse from(PlaylistEntry entry) {
        return new PlaylistEntryResponse(
                entry.getId().toString(),
                entry.getTrack().getId().toString(),
                entry.getAddedOn()
        );
    }
}
