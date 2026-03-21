package com.example.musicbooru.repository;

import com.example.musicbooru.model.Playlist;
import com.example.musicbooru.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {
    @Query(value = """
            SELECT DISTINCT p
            FROM Playlist p
            LEFT JOIN FETCH p.entries e LEFT JOIN FETCH e.track
            WHERE p.owner = :owner
            """)
    List<Playlist> findByOwner(@Param("owner") User owner);

    @Query("""
            SELECT p FROM Playlist p
            LEFT JOIN FETCH p.entries e
            LEFT JOIN FETCH e.track
            WHERE p.id = :playlistId
            """)
    Optional<Playlist> findByIdWithTracks(@Param("playlistId") UUID playlistId);
}
