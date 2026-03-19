package com.example.musicbooru.controller;

import com.example.musicbooru.dto.AddTrackToPlaylistRequest;
import com.example.musicbooru.dto.CreatePlaylistRequest;
import com.example.musicbooru.dto.PlaylistResponse;
import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.model.*;
import com.example.musicbooru.auth.JwtService;
import com.example.musicbooru.service.PlaylistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("SpellCheckingInspection")
@WebMvcTest(PlaylistController.class)
public class PlaylistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // We need this to serialize request bodies to JSON
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlaylistService playlistService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private final UUID playlistId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID trackId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID entryId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private final UUID ownerId = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private User owner;
    private Track track;
    private Playlist playlist;
    private PlaylistResponse playlistResponse;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(ownerId);
        owner.setUsername("testuser");
        owner.setRole(Role.USER);

        track = Track.builder()
                .id(trackId)
                .title("Test Title")
                .build();

        playlist = Playlist.builder()
                .id(playlistId)
                .name("Test Playlist")
                .owner(owner)
                .entries(new java.util.ArrayList<>())
                .build();

        // Pre-built response DTO used for stubbing service methods that return PlaylistResponse
        playlistResponse = new PlaylistResponse(
                playlistId.toString(),
                "Test Playlist",
                ownerId.toString(),
                List.of()
        );
    }

    // --- POST /api/playlist ---

    @Test
    void createPlaylist_returnsCreatedWithPlaylist() throws Exception {
        when(playlistService.createPlaylist(any(User.class), eq("Test Playlist")))
                .thenReturn(playlist);

        mockMvc.perform(post("/api/playlist")
                        .with(csrf())
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePlaylistRequest("Test Playlist"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Playlist"));
    }

    @Test
    void createPlaylist_returnsForbidden_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/playlist")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePlaylistRequest("Test Playlist"))))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/playlist ---

    @Test
    void getPlaylists_returnsOkWithPlaylistList() throws Exception {
        when(playlistService.getPlaylistsByOwner(any(User.class)))
                .thenReturn(List.of(playlistResponse));

        mockMvc.perform(get("/api/playlist")
                        .with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Playlist"))
                .andExpect(jsonPath("$[0].ownerId").value(ownerId.toString()));
    }

    @Test
    void getPlaylists_returnsEmptyList_whenOwnerHasNoPlaylists() throws Exception {
        when(playlistService.getPlaylistsByOwner(any(User.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/playlist")
                        .with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- GET /api/playlist/{playlistId}/tracks ---

    @Test
    void getPlaylistTracks_returnsOkWithTracks() throws Exception {
        when(playlistService.getTracksByPlaylistId(eq(playlistId.toString()), any(User.class)))
                .thenReturn(List.of(track));

        mockMvc.perform(get("/api/playlist/{playlistId}/tracks", playlistId.toString())
                        .with(user(owner)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Title"));
    }

    @Test
    void getPlaylistTracks_returnsNotFound_whenPlaylistDoesNotExist() throws Exception {
        when(playlistService.getTracksByPlaylistId(eq(playlistId.toString()), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Playlist '" + playlistId + "' not found"));

        mockMvc.perform(get("/api/playlist/{playlistId}/tracks", playlistId.toString())
                        .with(user(owner)))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/playlist/{playlistId}/track ---

    @Test
    void addTrackToPlaylist_returnsOkWithUpdatedPlaylist() throws Exception {
        when(playlistService.addTrack(eq(playlistId.toString()), eq(trackId.toString()), any(User.class)))
                .thenReturn(playlistResponse);

        mockMvc.perform(post("/api/playlist/{playlistId}/track", playlistId.toString())
                        .with(csrf())
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddTrackToPlaylistRequest(trackId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Playlist"));
    }

    @Test
    void addTrackToPlaylist_returnsNotFound_whenPlaylistDoesNotExist() throws Exception {
        when(playlistService.addTrack(eq(playlistId.toString()), eq(trackId.toString()), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Playlist '" + playlistId + "' not found"));

        mockMvc.perform(post("/api/playlist/{playlistId}/track", playlistId.toString())
                        .with(csrf())
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddTrackToPlaylistRequest(trackId.toString()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void addTrackToPlaylist_returnsNotFound_whenTrackDoesNotExist() throws Exception {
        when(playlistService.addTrack(eq(playlistId.toString()), eq(trackId.toString()), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Track '" + trackId + "' not found"));

        mockMvc.perform(post("/api/playlist/{playlistId}/track", playlistId.toString())
                        .with(csrf())
                        .with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddTrackToPlaylistRequest(trackId.toString()))))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/playlist/{playlistId}/track/{entryId} ---

    @Test
    void removeTrackFromPlaylist_returnsNoContent() throws Exception {
        doNothing().when(playlistService).removeTrack(eq(playlistId.toString()), eq(entryId.toString()), any(User.class));

        mockMvc.perform(
                delete(
                        "/api/playlist/{playlistId}/track/{entryId}",
                        playlistId.toString(),
                        entryId.toString()
                )
                        .with(csrf())
                        .with(user(owner))
        ).andExpect(status().isNoContent());

        verify(playlistService).removeTrack(playlistId.toString(), entryId.toString(), owner);
    }

    @Test
    void removeTrackFromPlaylist_returnsNotFound_whenEntryDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Playlist entry '" + entryId + "' not found"))
                .when(playlistService).removeTrack(eq(playlistId.toString()), eq(entryId.toString()), any(User.class));

        mockMvc.perform(
                delete(
                        "/api/playlist/{playlistId}/track/{entryId}",
                        playlistId.toString(),
                        entryId.toString()
                )
                        .with(csrf())
                        .with(user(owner))
        ).andExpect(status().isNotFound());
    }

    // --- GET /api/playlist/{playlistId}/icon ---

    @Test
    void getPlaylistIcon_returnsJpegResource_whenPlaylistExists() throws Exception {
        when(playlistService.playlistExists(eq(playlistId.toString()), any(User.class))).thenReturn(true);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            // Force the fallback path to avoid UrlResource trying to resolve
            // a real file for Content-Length calculation.
            filesMock.when(() -> Files.exists(any(Path.class))).thenReturn(false);

            mockMvc.perform(get("/api/playlist/{playlistId}/icon", playlistId.toString())
                            .with(user(owner)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_JPEG));
        }
    }

    @Test
    void getPlaylistIcon_returnsNotFound_whenPlaylistDoesNotExist() throws Exception {
        when(playlistService.playlistExists(eq(playlistId.toString()), any(User.class))).thenReturn(false);

        mockMvc.perform(get("/api/playlist/{playlistId}/icon", playlistId.toString())
                        .with(user(owner)))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/playlist/{playlistId}/icon ---

    @Test
    void setPlaylistIcon_returnsNoContent_whenSuccessful() throws Exception {
        doNothing().when(playlistService).addIcon(any(), eq(playlistId.toString()), any(User.class));

        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "file",
                "icon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image-data".getBytes()
        );

        mockMvc.perform(multipart("/api/playlist/{playlistId}/icon", playlistId.toString())
                        .file(mockMultipartFile)
                        .with(csrf())
                        .with(user(owner)))
                .andExpect(status().isNoContent());

        verify(playlistService).addIcon(any(), eq(playlistId.toString()), any(User.class));
    }

    @Test
    void setPlaylistIcon_returnsGenericException_whenConversionFails() throws Exception {
        doThrow(new GenericException("Could not convert image to JPEG"))
                .when(playlistService).addIcon(any(), eq(playlistId.toString()), any(User.class));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "icon.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image-data".getBytes()
        );

        mockMvc.perform(multipart("/api/playlist/{playlistId}/icon", playlistId.toString())
                        .file(file)
                        .with(csrf())
                        .with(user(owner)))
                .andExpect(status().isInternalServerError());
    }

    // --- DELETE /api/playlist/{playlistId}/icon ---

    @Test
    void removePlaylistIcon_returnsNoContent_whenSuccessful() throws Exception {
        doNothing().when(playlistService).removeIcon(eq(playlistId.toString()), any(User.class));

        mockMvc.perform(delete("/api/playlist/{playlistId}/icon", playlistId.toString())
                        .with(csrf())
                        .with(user(owner)))
                .andExpect(status().isNoContent());

        verify(playlistService).removeIcon(playlistId.toString(), owner);
    }

    // --- DELETE /api/playlist/{playlistId} ---

    @Test
    void deletePlaylist_returnsNoContent_whenSuccessful() throws Exception {
        doNothing().when(playlistService).deletePlaylist(eq(playlistId.toString()), any(User.class));

        mockMvc.perform(delete("/api/playlist/{playlistId}", playlistId.toString())
                        .with(csrf())
                        .with(user(owner)))
                .andExpect(status().isNoContent());

        verify(playlistService).deletePlaylist(playlistId.toString(), owner);
    }

    @Test
    void deletePlaylist_returnsNotFound_whenPlaylistDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Playlist '" + playlistId + "' not found"))
                .when(playlistService).deletePlaylist(eq(playlistId.toString()), any(User.class));

        mockMvc.perform(delete("/api/playlist/{playlistId}", playlistId.toString())
                        .with(csrf())
                        .with(user(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePlaylist_returnsUnauthorized_whenRequesterIsNotOwner() throws Exception {
        doThrow(new GenericException("You do not own this playlist", HttpStatus.UNAUTHORIZED))
                .when(playlistService).deletePlaylist(eq(playlistId.toString()), any(User.class));

        mockMvc.perform(delete("/api/playlist/{playlistId}", playlistId.toString())
                        .with(csrf())
                        .with(user(owner)))
                .andExpect(status().isUnauthorized());
    }
}
