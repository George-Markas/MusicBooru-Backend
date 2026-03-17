package com.example.musicbooru.service;

import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.model.Track;
import com.example.musicbooru.repository.TrackRepository;
import com.example.musicbooru.util.MetadataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.musicbooru.util.Commons.QUERY_CHARACTER_LIMIT;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.example.musicbooru.util.Commons.AUDIO_EXTENSION;

@ExtendWith(MockitoExtension.class)
class TrackServiceTest {

    @Mock
    private TrackRepository trackRepository;

    // Inject TrackRepository mocks into TrackService instance
    @InjectMocks
    private TrackService trackService;

    private Track track;
    private final UUID trackId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        track = Track.builder()
                .id(trackId)
                .title("Test Title")
                .artist("Test Artist")
                .album("Test Album")
                .genre("Test Genre")
                .year("2026")
                .fileName("Test Artist - Test Title" + AUDIO_EXTENSION)
                .build();
    }

    // --- trackExists ---

    @Test
    void trackExists_returnsTrue_whenTrackIsPresent() {
        when(trackRepository.existsById(trackId)).thenReturn(true);

        boolean result = trackService.trackExists(trackId.toString());

        assertThat(result).isTrue();

        verify(trackRepository, times(1)).existsById(trackId);
    }

    @Test
    void trackExists_returnsFalse_whenTrackIsAbsent() {
        when(trackRepository.existsById(trackId)).thenReturn(false);

        assertThat(trackService.trackExists(trackId.toString())).isFalse();
    }

    // --- getTracks ---

    @Test
    void getTracks_returnsAllTracks() {
        when(trackRepository.findAll()).thenReturn(List.of(track));

        List<Track> result = trackService.getTracks();

        assertThat(result).containsExactly(track);
        verify(trackRepository).findAll();
    }

    @Test
    void getTracks_withField_sortsByFieldAscending() {
        // Construct a Sort object matching what the service will produce
        Sort expectedSort = Sort.by(Sort.Direction.ASC, "title");
        when(trackRepository.findAll(expectedSort)).thenReturn(List.of(track));

        List<Track> result = trackService.getTracks("title");

        assertThat(result).containsExactly(track);

        verify(trackRepository).findAll(expectedSort);
    }

    // --- getTrackById ---

    @Test
    void getTrackById_returnsTrack_whenFound() {
        when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));

        Optional<Track> result = trackService.getTrackById(trackId.toString());

        assertThat(result).isPresent().contains(track);
    }

    @Test
    void getTrackById_returnsEmpty_whenNotFound() {
        when(trackRepository.findById(trackId)).thenReturn(Optional.empty());

        Optional<Track> result = trackService.getTrackById(trackId.toString());

        assertThat(result).isEmpty();
    }

    // --- getFileName ---

    @Test
    void getFileName_returnsProjection_whenTrackExists() {
        TrackRepository.FileNameOnly projection = () -> "Test Artist - Test Title" + AUDIO_EXTENSION;
        when(trackRepository.findProjectedById(trackId)).thenReturn(Optional.of(projection));

        TrackRepository.FileNameOnly result = trackService.getFileName(trackId.toString());

        assertThat(result.getFileName()).isEqualTo("Test Artist - Test Title" + AUDIO_EXTENSION);
    }

    @Test
    void getFileName_throwsResourceNotFoundException_whenTrackNotFound() {
        when(trackRepository.findProjectedById(trackId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackService.getFileName(trackId.toString()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(trackId.toString());
    }

    // --- addTrack ---

    @Test
    @SuppressWarnings("unused")
    void addTrack_generatesFileNameFromMetadata_whenSufficientMetadataIsAvailable() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Path temp = Paths.get("/tmp/test" + AUDIO_EXTENSION);
        String expectedFileName = "Test Artist - Test Title" + AUDIO_EXTENSION;

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        try (
                MockedConstruction<MetadataUtils> metadataUtilsMock = mockConstruction(MetadataUtils.class,
                        (mock, context) -> {
                            when(mock.generateFileName()).thenReturn(expectedFileName);
                            when(mock.getTitle()).thenReturn("Test Title");
                            when(mock.getArtist()).thenReturn("Test Artist");
                            when(mock.getAlbum()).thenReturn("Test Album");
                            when(mock.getGenre()).thenReturn("Rock");
                            when(mock.getYear()).thenReturn("2026");
                        });

                MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)
        ) {
            filesMock.when(() -> Files.createTempFile(isNull(), eq(AUDIO_EXTENSION))).thenReturn(temp);
            filesMock.when(() -> Files.copy(any(Path.class), eq(temp), eq(StandardCopyOption.REPLACE_EXISTING)))
                    .thenReturn(temp);

            // Have the existsByFilename stub return false as to not use the UUID as the filename
            when(trackRepository.existsByFileName(expectedFileName)).thenReturn(false);

            // Have Files.exists return false as to avoid the overwrite warning
            filesMock.when(() -> Files.exists(any(Path.class))).thenReturn(false);
            filesMock.when(() -> Files.move(any(), any(), eq(StandardCopyOption.REPLACE_EXISTING))).thenReturn(temp);

            when(trackRepository.save(any(Track.class))).thenAnswer(inv -> inv.getArgument(0));

            Track result = trackService.addTrack(file);

            assertThat(result.getFileName()).isEqualTo(expectedFileName);
            assertThat(result.getTitle()).isEqualTo("Test Title");
            verify(trackRepository).save(any(Track.class));
        }
    }

    // When generateFileName() returns null (insufficient metadata),
    // the service should fall back to using the track's UUID as the filename.
    @Test
    @SuppressWarnings("unused")
    void addTrack_usesUuidAsFileName_whenSufficientMetadataIsNotAvailable() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Path temp = Paths.get("/tmp/test" + AUDIO_EXTENSION);

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        try (
                MockedConstruction<MetadataUtils> metadataUtilsMock = mockConstruction(MetadataUtils.class,
                        (mock, context) -> {
                            when(mock.generateFileName()).thenReturn(null);
                            when(mock.getTitle()).thenReturn(null);
                            when(mock.getArtist()).thenReturn(null);
                            when(mock.getAlbum()).thenReturn(null);
                            when(mock.getGenre()).thenReturn(null);
                            when(mock.getYear()).thenReturn(null);
                        });

                MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)
        ) {
            filesMock.when(() -> Files.createTempFile(isNull(), eq(AUDIO_EXTENSION))).thenReturn(temp);
            filesMock.when(() -> Files.copy(any(Path.class), eq(temp), eq(StandardCopyOption.REPLACE_EXISTING)))
                    .thenReturn(temp);

            // Have Files.exists return false as to avoid the overwrite warning
            filesMock.when(() -> Files.exists(any(Path.class))).thenReturn(false);
            filesMock.when(() -> Files.move(any(), any(), eq(StandardCopyOption.REPLACE_EXISTING)))
                    .thenReturn(temp);

            when(trackRepository.save(any(Track.class))).thenAnswer(inv -> inv.getArgument(0));

            Track result = trackService.addTrack(file);

            // While we can't know the exact UUID, we know the expected filename format,
            // i.e., it should end with the proper extension and not be 'null'.
            assertThat(result.getFileName()).endsWith(AUDIO_EXTENSION);
            assertThat(result.getFileName()).doesNotContain("null");
        }
    }

    // When a track with the same metadata-derived filename already exists,
    // the service should fall back to using the track's UUID as the filename.
    @Test
    @SuppressWarnings("unused")
    void addTrack_usesUuidAsFileName_whenGeneratedFilenameAlreadyExists() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        Path temp = Paths.get("/tmp/test" + AUDIO_EXTENSION);
        String duplicateFileName = "Test Artist - Test Title" + AUDIO_EXTENSION;

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        try (
                MockedConstruction<MetadataUtils> metadataUtilsMock = mockConstruction(MetadataUtils.class,
                        (mock, context) -> {
                            when(mock.generateFileName()).thenReturn(duplicateFileName);
                            when(mock.getTitle()).thenReturn("Test Title");
                            when(mock.getArtist()).thenReturn("Test Artist");
                            when(mock.getAlbum()).thenReturn("Test Album");
                            when(mock.getGenre()).thenReturn("Rock");
                            when(mock.getYear()).thenReturn("2026");
                        });

                MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)
        ) {
            filesMock.when(() -> Files.createTempFile(isNull(), eq(AUDIO_EXTENSION))).thenReturn(temp);
            filesMock.when(() -> Files.copy(any(Path.class), eq(temp), eq(StandardCopyOption.REPLACE_EXISTING)))
                    .thenReturn(temp);

            // Have the existsByFilename stub return false as to not use the UUID as the filename
            when(trackRepository.existsByFileName(duplicateFileName)).thenReturn(true);

            // Have Files.exists return false as to avoid the overwrite warning
            filesMock.when(() -> Files.exists(any(Path.class))).thenReturn(false);
            filesMock.when(() -> Files.move(any(), any(), eq(StandardCopyOption.REPLACE_EXISTING))).thenReturn(temp);

            when(trackRepository.save(any(Track.class))).thenAnswer(inv -> inv.getArgument(0));

            Track result = trackService.addTrack(file);

            assertThat(result.getFileName()).endsWith(AUDIO_EXTENSION);
            assertThat(result.getFileName()).isNotEqualTo(duplicateFileName);
        }
    }

    // If getInputStream() throws, Files.copy will never be reached and the IOException
    // propagates up. The service must catch it and rethrow it as GenericException.
    @Test
    void addTrack_throwsGenericException_onIOException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("I/O error"));

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            Path tempPath = Paths.get("/tmp/test.m4a");
            filesMock.when(() -> Files.createTempFile(isNull(), eq(AUDIO_EXTENSION))).thenReturn(tempPath);

            assertThatThrownBy(() -> trackService.addTrack(file))
                    .isInstanceOf(GenericException.class)
                    .hasMessageContaining("Could not add track");
        }
    }

    // --- removeTrack ---

    @Test
    void removeTrack_deletesFilesAndDatabaseRecord_whenTrackExists() {
        when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            // We don't use the boolean returned by deleteIfExists, defaulting to false is fine
            filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(false);

            trackService.removeTrack(trackId.toString());

            // Confirm deleteIfExists was called exactly twice: once for artwork, once for audio
            filesMock.verify(() -> Files.deleteIfExists(any(Path.class)), times(2));

            // Confirm the database record was deleted
            verify(trackRepository).delete(track);
        }
    }

    @Test
    void removeTrack_throwsResourceNotFoundException_whenTrackNotFound() {
        when(trackRepository.findById(trackId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackService.removeTrack(trackId.toString()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(trackId.toString());

        // The repository's delete method must never be called if the track wasn't found
        verify(trackRepository, never()).delete(any());
    }

    @Test
    void removeTrack_throwsGenericException_onIOException() {
        when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.deleteIfExists(any(Path.class)))
                    .thenThrow(new IOException("I/O error"));

            assertThatThrownBy(() -> trackService.removeTrack(trackId.toString()))
                    .isInstanceOf(GenericException.class)
                    .hasMessageContaining("Could not delete track");

            // If deletion throws, the repository delete must not be called either
            verify(trackRepository, never()).delete(any());
        }
    }

    // --- searchTracks ---
    @Test
    void searchTracks_returnsResults_forValidQuery() {
        when(trackRepository.searchTracks("test", QUERY_CHARACTER_LIMIT)).thenReturn(List.of(track));

        List<Track> result = trackService.searchTracks("test");

        assertThat(result).containsExactly(track);
    }

    @Test
    void searchTracks_returnsEmpty_forNullQuery() {
        List<Track> result = trackService.searchTracks(null);

        assertThat(result).isEmpty();

        // The repository must never be called for a null or blank query
        verifyNoInteractions(trackRepository);
    }

    @Test
    void searchTracks_returnsEmpty_forBlankQuery() {
        List<Track> result = trackService.searchTracks("   ");

        assertThat(result).isEmpty();
        verifyNoInteractions(trackRepository);
    }

    // Verifies that the query is trimmed before being passed to the repository
    @Test
    void searchTracks_trimsQueryBeforePassingToRepository() {
        when(trackRepository.searchTracks("test", QUERY_CHARACTER_LIMIT)).thenReturn(List.of(track));

        trackService.searchTracks("  test  ");

        verify(trackRepository).searchTracks("test", QUERY_CHARACTER_LIMIT);
    }
}