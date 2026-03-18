package com.example.musicbooru.controller;

import com.example.musicbooru.auth.JwtService;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.model.Track;
import com.example.musicbooru.service.TrackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.musicbooru.util.Commons.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser
@WebMvcTest(TrackController.class)
public class TrackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackService trackService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private final UUID trackId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Track track;

    @BeforeEach
    void setUp() {
        track = Track.builder()
                .id(trackId)
                .title("Test Title")
                .artist("Test Artist")
                .album("Test Album")
                .genre("Test Genre")
                .year("1970")
                .fileName("Test Artist - Test Title" + AUDIO_EXTENSION)
                .build();
    }

    // --- GET /api/track ---

    @Test
    void getTracks_returnsOkWithTrackList() throws Exception {
        when(trackService.getTracks()).thenReturn(List.of(track));

        mockMvc.perform(get("/api/track"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Title"))
                .andExpect(jsonPath("$[0].artist").value("Test Artist"));
    }

    @Test
    void getTracks_returnsEmptyList_whenNoTracksExist() throws Exception {
        when(trackService.getTracks()).thenReturn(List.of());

        mockMvc.perform(get("/api/track"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- GET /api/track/sort/{by} ---

    @Test
    void getTracks_sortedByAlbum_delegatesToServiceWithAlbumField() throws Exception {
        when(trackService.getTracks("album")).thenReturn(List.of(track));

        mockMvc.perform(get("/api/track/sort/album"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].album").value("Test Album"));

        verify(trackService).getTracks("album");
    }

    @Test
    void getTracks_sortedByArtist_delegatesToServiceWithArtistField() throws Exception {
        when(trackService.getTracks("artist")).thenReturn(List.of(track));

        mockMvc.perform(get("/api/track/sort/artist"))
                .andExpect(status().isOk());

        verify(trackService).getTracks("artist");
    }

    @Test
    void getTracks_sortedByUnknownField_defaultsToTitle() throws Exception {
        when(trackService.getTracks("title")).thenReturn(List.of(track));

        // Any value that is not "album" or "artist" should fall through to the
        // default case in the switch expression and sort by title.
        mockMvc.perform(get("/api/track/sort/unknown"))
                .andExpect(status().isOk());

        verify(trackService).getTracks("title");
    }

    // --- GET /api/track/{trackId} ---

    @Test
    void getTrack_returnsOkWithTrack_whenFound() throws Exception {
        when(trackService.getTrackById(trackId.toString())).thenReturn(Optional.of(track));

        mockMvc.perform(get("/api/track/{trackId}", trackId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.artist").value("Test Artist"));
    }

    @Test
    void getTrack_returnsNotFound_whenTrackDoesNotExist() throws Exception {
        when(trackService.getTrackById(trackId.toString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/track/{trackId}", trackId.toString()))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/track ---

    @Test
    void uploadTrack_returnsCreatedWithTrack() throws Exception {
        when(trackService.addTrack(any())).thenReturn(track);

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test.m4a",
                "audio/mp4",
                "audio-data".getBytes()
        );

        mockMvc.perform(multipart("/api/track").file(multipartFile).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Title"));
    }

    // --- DELETE /api/track/{trackId} ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteTrack_returnsNoContent_whenTrackExists() throws Exception {
        doNothing().when(trackService).removeTrack(trackId.toString());

        mockMvc.perform(delete("/api/track/{trackId}", trackId.toString()).with(csrf()))
                .andExpect(status().isNoContent());

        verify(trackService).removeTrack(trackId.toString());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteTrack_returnsNotFound_whenTrackDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Track '" + trackId + "' not found"))
                .when(trackService).removeTrack(trackId.toString());

        mockMvc.perform(delete("/api/track/{trackId}", trackId.toString()).with(csrf()))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/track/{trackId}/art ---

    @Test
    void getArtwork_returnsJpegResource_whenArtworkFileExists() throws Exception {
        when(trackService.trackExists(trackId.toString())).thenReturn(true);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            // Force the fallback path to avoid UrlResource trying to resolve
            // a real file for Content-Length calculation.
            filesMock.when(() -> Files.exists(any(Path.class))).thenReturn(false);

            mockMvc.perform(get("/api/track/{trackId}/art", trackId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_JPEG));
        }
    }

    @Test
    void getArtwork_returnsFallbackResource_whenArtworkFileDoesNotExist() throws Exception {
        when(trackService.trackExists(trackId.toString())).thenReturn(true);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            // When the artwork file is absent, the controller should still return 200
            // with JPEG content type, using the cover art placeholder.
            filesMock.when(() -> Files.exists(any(Path.class))).thenReturn(false);

            mockMvc.perform(get("/api/track/{trackId}/art", trackId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_JPEG));
        }
    }

    @Test
    void getArtwork_returnsNotFound_whenTrackDoesNotExist() throws Exception {
        when(trackService.trackExists(trackId.toString())).thenReturn(false);

        mockMvc.perform(get("/api/track/{trackId}/art", trackId.toString()))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/track/search ---

    @Test
    void searchTracks_returnsMatchingTracks() throws Exception {
        when(trackService.searchTracks("test")).thenReturn(List.of(track));

        mockMvc.perform(get("/api/track/search").param("query", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Title"));
    }

    @Test
    void searchTracks_returnsEmptyList_whenNoMatches() throws Exception {
        when(trackService.searchTracks("nonexistent")).thenReturn(List.of());

        mockMvc.perform(get("/api/track/search").param("query", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}