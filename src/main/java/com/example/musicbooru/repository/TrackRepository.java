package com.example.musicbooru.repository;

import com.example.musicbooru.model.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackRepository extends JpaRepository<Track, UUID> {

    boolean existsByFileName(String fileName);

    interface FileNameOnly {
        String getFileName();
    }

    Optional<FileNameOnly> findProjectedById(UUID trackId);

    @Query(value = """
            SELECT * FROM (
                SELECT DISTINCT ON (t.id) t.*,
                    GREATEST(
                        word_similarity(:query, COALESCE(t.title, '')),
                        word_similarity(:query, COALESCE(t.artist, '')),
                        word_similarity(:query, COALESCE(t.album, ''))
                    ) AS relevance
                FROM track t
                WHERE
                    t.title  ILIKE '%' || :query || '%'
                    OR t.artist ILIKE '%' || :query || '%'
                    OR t.album  ILIKE '%' || :query || '%'
                    OR word_similarity(:query, COALESCE(t.title, ''))  > LEAST(0.1 + LENGTH(:query) * 0.06, 0.5)
                    OR word_similarity(:query, COALESCE(t.artist, '')) > LEAST(0.1 + LENGTH(:query) * 0.06, 0.5)
                    OR word_similarity(:query, COALESCE(t.album, ''))  > LEAST(0.1 + LENGTH(:query) * 0.06, 0.5)
                ORDER BY t.id
            ) ranked
            ORDER BY relevance DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Track> searchTracks(@Param("query") String query, @Param("limit") int characterLimit);
}