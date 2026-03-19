package com.example.musicbooru.controller;

import com.example.musicbooru.auth.JwtService;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.repository.TrackRepository;
import com.example.musicbooru.service.TrackService;
import com.example.musicbooru.util.HeaderUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.example.musicbooru.util.Commons.AUDIO_EXTENSION;
import static com.example.musicbooru.util.Commons.LIBRARY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
@WebMvcTest(StreamController.class)
public class StreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackService trackService;

    @MockitoBean
    private JwtService jwtService;

    private final UUID trackId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final String fileName = "Test Artist - Test Title" + AUDIO_EXTENSION;
    private final String eTag = "\"testETag\"";
    private final Instant lastModified = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        TrackRepository.FileNameOnly fileNameOnly = () -> fileName;
        when(trackService.getFileName(trackId.toString())).thenReturn(fileNameOnly);
    }

    // Helper that sets up the common file system and HeaderUtils stubs used by
    // most tests. Tests that need different behavior override individual stubs.
    private void stubFileSystem(MockedStatic<Files> filesMock, MockedStatic<HeaderUtils> headersUtilsMock) {
        Path filePath = Path.of(LIBRARY + fileName);

        filesMock.when(() -> Files.exists(filePath)).thenReturn(true);
        filesMock.when(() -> Files.getLastModifiedTime(filePath)).thenReturn(FileTime.from(lastModified));

        headersUtilsMock.when(() -> HeaderUtils.generateETag(filePath)).thenReturn(eTag);
        headersUtilsMock.when(() -> HeaderUtils.matches(any(), eq(eTag))).thenReturn(false);
    }

    @Test
    void streamTrack_returnsOkWithAudioContent() throws Exception {
        try (
                MockedStatic<Files> filesMock = mockStatic(Files.class);
                MockedStatic<HeaderUtils> headerUtilsMock = mockStatic(HeaderUtils.class)
        ) {
            stubFileSystem(filesMock, headerUtilsMock);

            mockMvc.perform(get("/api/stream/{trackId}", trackId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                    .andExpect(header().exists(HttpHeaders.ETAG))
                    .andExpect(header().exists(HttpHeaders.LAST_MODIFIED));
        }
    }

    @Test
    void streamTrack_returnsNotFound_whenFileDoesNotExist() throws Exception {
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.notExists(any(Path.class))).thenReturn(true);

            mockMvc.perform(get("/api/stream/{trackId}", trackId.toString()))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void streamTrack_returnsNotFound_whenTrackDoesNotExist() throws Exception {
        when(trackService.getFileName(trackId.toString()))
                .thenThrow(new ResourceNotFoundException("Track '" + trackId + "' not found"));

        mockMvc.perform(get("/api/stream/{trackId}", trackId.toString()))
                .andExpect(status().isNotFound());
    }

    // --- If-None-Match ---

    @Test
    void streamTrack_returns304_whenIfNoneMatchHeaderMatchesETag() throws Exception {
        try (
                MockedStatic<Files> filesMock = mockStatic(Files.class);
                MockedStatic<HeaderUtils> headerUtilsMock = mockStatic(HeaderUtils.class)
        ) {
            stubFileSystem(filesMock, headerUtilsMock);
            headerUtilsMock.when(() -> HeaderUtils.matches(eTag, eTag)).thenReturn(true);

            mockMvc.perform(get("/api/stream/{trackId}", trackId.toString())
                            .header(HttpHeaders.IF_NONE_MATCH, eTag))
                    .andExpect(status().isNotModified());
        }
    }

    @Test
    void streamTrack_returns200_whenIfNoneMatchHeaderDoesNotMatchETag() throws Exception {
        try (
                MockedStatic<Files> filesMock = mockStatic(Files.class);
                MockedStatic<HeaderUtils> headerUtilsMock = mockStatic(HeaderUtils.class)
        ) {
            stubFileSystem(filesMock, headerUtilsMock);
            headerUtilsMock.when(() -> HeaderUtils.matches("\"differentETag\"", eTag)).thenReturn(false);

            mockMvc.perform(get("/api/stream/{trackId}", trackId.toString())
                            .header(HttpHeaders.IF_NONE_MATCH, "\"differentETag\""))
                    .andExpect(status().isOk());
        }
    }

    // --- If-Modified-Since ---

    @Test
    void streamTrack_returns304_whenIfModifiedSinceIsAfterLastModified() throws Exception {
        try (
                MockedStatic<Files> filesMock = mockStatic(Files.class);
                MockedStatic<HeaderUtils> headerUtilsMock = mockStatic(HeaderUtils.class)
        ) {
            stubFileSystem(filesMock, headerUtilsMock);

            Instant clientTimestamp = lastModified.plus(1, ChronoUnit.DAYS);
            headerUtilsMock.when(() -> HeaderUtils.parseHttpDate(clientTimestamp.toString()))
                    .thenReturn(clientTimestamp);

            mockMvc.perform(get("/api/stream/{trackId}", trackId.toString())
                            .header(HttpHeaders.IF_MODIFIED_SINCE, clientTimestamp.toString()))
                    .andExpect(status().isNotModified());
        }
    }

    @Test
    void streamTrack_returns200_whenIfModifiedSinceIsBeforeLastModified() throws Exception {
        try (
                MockedStatic<Files> filesMock = mockStatic(Files.class);
                MockedStatic<HeaderUtils> headerUtilsMock = mockStatic(HeaderUtils.class)
        ) {
            stubFileSystem(filesMock, headerUtilsMock);

            Instant clientTimestamp = lastModified.minus(1, ChronoUnit.DAYS);
            headerUtilsMock.when(() -> HeaderUtils.parseHttpDate(clientTimestamp.toString()))
                    .thenReturn(clientTimestamp);

            mockMvc.perform(get("/api/stream/{trackId}", trackId.toString())
                            .header(HttpHeaders.IF_MODIFIED_SINCE, clientTimestamp.toString()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void streamTrack_ignoresIfModifiedSince_whenIfNoneMatchIsAlsoPresent() throws Exception {
        try (
                MockedStatic<Files> filesMock = mockStatic(Files.class);
                MockedStatic<HeaderUtils> headerUtilsMock = mockStatic(HeaderUtils.class)
        ) {
            stubFileSystem(filesMock, headerUtilsMock);

            headerUtilsMock.when(() -> HeaderUtils.matches("\"differentETag\"", eTag)).thenReturn(false);
            mockMvc.perform(get("/api/stream/{trackId}", trackId.toString())
                            .header(HttpHeaders.IF_NONE_MATCH, "\"differentETag\"")
                            .header(
                                    HttpHeaders.IF_MODIFIED_SINCE,
                                    lastModified.minus(1, ChronoUnit.DAYS).toString()
                            )
                    )
                    .andExpect(status().isOk());
        }
    }

    // --- If-Match ---

    @Test
    void streamTrack_returns412_whenIfMatchHeaderDoesNotMatchETag() throws Exception {
        try (
                MockedStatic<Files> filesMock = mockStatic(Files.class);
                MockedStatic<HeaderUtils> headersMock = mockStatic(HeaderUtils.class)
        ) {
            stubFileSystem(filesMock, headersMock);
            headersMock.when(() -> HeaderUtils.matches("\"differentETag\"", eTag)).thenReturn(false);

            mockMvc.perform(get("/api/stream/{trackId}", trackId.toString())
                            .header(HttpHeaders.IF_MATCH, "\"differentETag\""))
                    .andExpect(status().isPreconditionFailed());
        }
    }

    @Test
    void streamTrack_returns200_whenIfMatchHeaderMatchesETag() throws Exception {
        try (
                MockedStatic<Files> filesMock = mockStatic(Files.class);
                MockedStatic<HeaderUtils> headerUtilsMock = mockStatic(HeaderUtils.class)
        ) {
            stubFileSystem(filesMock, headerUtilsMock);
            headerUtilsMock.when(() -> HeaderUtils.matches(eTag, eTag)).thenReturn(true);

            mockMvc.perform(get("/api/stream/{trackId}", trackId.toString())
                            .header(HttpHeaders.IF_MATCH, eTag))
                    .andExpect(status().isOk());
        }
    }

    // --- If-Unmodified-Since ---

    @Test
    void streamTrack_returns412_whenIfUnmodifiedSinceIsBeforeLastModified() throws Exception {
        try (
                MockedStatic<Files> filesMock = mockStatic(Files.class);
                MockedStatic<HeaderUtils> headerUtilsMock = mockStatic(HeaderUtils.class)
        ) {
            stubFileSystem(filesMock, headerUtilsMock);

            Instant clientTimestamp = lastModified.minus(1, ChronoUnit.DAYS);
            headerUtilsMock.when(() -> HeaderUtils.parseHttpDate(clientTimestamp.toString()))
                    .thenReturn(clientTimestamp);

            mockMvc.perform(get("/api/stream/{trackId}", trackId.toString())
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, clientTimestamp.toString()))
                    .andExpect(status().isPreconditionFailed());
        }
    }

    @Test
    void streamTrack_returns200_whenIfUnmodifiedSinceIsAfterLastModified() throws Exception {
        try (
                MockedStatic<Files> filesMock = mockStatic(Files.class);
                MockedStatic<HeaderUtils> headerUtilsMock = mockStatic(HeaderUtils.class)
        ) {
            stubFileSystem(filesMock, headerUtilsMock);

            Instant clientTimestamp = lastModified.plus(1, ChronoUnit.DAYS);
            headerUtilsMock.when(() -> HeaderUtils.parseHttpDate(clientTimestamp.toString()))
                    .thenReturn(clientTimestamp);

            mockMvc.perform(get("/api/stream/{trackId}", trackId.toString())
                            .header(HttpHeaders.IF_UNMODIFIED_SINCE, clientTimestamp.toString()))
                    .andExpect(status().isOk());
        }
    }

    // --- IOException handling ---

    @Test
    @SuppressWarnings("unused")
    void streamTrack_throwsGenericException_onIOException() throws Exception {
        try (
                MockedStatic<Files> filesMock = mockStatic(Files.class);
                MockedStatic<HeaderUtils> headerUtilsMock = mockStatic(HeaderUtils.class)
        ) {

            Path filePath = Path.of(LIBRARY + fileName);
            filesMock.when(() -> Files.notExists(filePath)).thenReturn(false);
            filesMock.when(() -> Files.getLastModifiedTime(filePath))
                    .thenThrow(new IOException("I/O error"));

            mockMvc.perform(get("/api/stream/{trackId}", trackId.toString()))
                    .andExpect(status().isInternalServerError());
        }
    }
}
