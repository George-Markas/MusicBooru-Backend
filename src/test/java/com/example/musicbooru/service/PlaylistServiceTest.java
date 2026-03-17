package com.example.musicbooru.service;

import com.example.musicbooru.dto.PlaylistResponse;
import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.model.*;
import com.example.musicbooru.repository.PlaylistEntryRepository;
import com.example.musicbooru.repository.PlaylistRepository;
import com.example.musicbooru.repository.TrackRepository;
import com.example.musicbooru.util.ContentUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.musicbooru.util.Commons.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlaylistServiceTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PlaylistEntryRepository playlistEntryRepository;

    @Mock
    private TrackRepository trackRepository;

    @InjectMocks
    private PlaylistService playlistService;

    private final UUID playlistId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID trackId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID entryId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private final UUID ownerId = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private final UUID otherId = UUID.fromString("00000000-0000-0000-0000-000000000005");


    private User owner;
    private User otherUser;
    private Track track;
    private Playlist playlist;
    private PlaylistEntry entry;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(ownerId);
        owner.setRole(Role.USER);

        // A different user who does not own the playlist
        otherUser = new User();
        otherUser.setId(otherId);
        otherUser.setRole(Role.USER);

        track = Track.builder()
                .id(trackId)
                .title("Test Title")
                .artist("Test Artist")
                .album("Test Album")
                .genre("Test Genre")
                .year("1970")
                .fileName("Test Artist - Test Title" + AUDIO_EXTENSION)
                .build();

        playlist = Playlist.builder()
                .id(playlistId)
                .name("Test Playlist")
                .owner(owner)
                .entries(new ArrayList<>())
                .build();

        entry = PlaylistEntry.builder()
                .id(entryId)
                .playlist(playlist)
                .track(track)
                .addedOn(Instant.now())
                .build();
    }

    // --- playlistExists ---

    @Test
    void playlistExists_returnsTrue_whenRequesterIsOwner() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        boolean result = playlistService.playlistExists(playlistId.toString(), owner);

        assertThat(result).isTrue();
    }

    @Test
    void playlistExists_returnsTrue_whenRequesterIsAdmin() {
        User admin = new User();
        admin.setId(otherId);
        admin.setRole(Role.ADMIN);

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        assertThat(playlistService.playlistExists(playlistId.toString(), admin)).isTrue();
    }

    @Test
    void playlistExists_returnsFalse_whenPlaylistNotFound() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThat(playlistService.playlistExists(playlistId.toString(), owner)).isFalse();
    }

    @Test
    void playlistExists_returnsFalse_whenRequesterIsNotOwnerAndNotAdmin() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        assertThat(playlistService.playlistExists(playlistId.toString(), otherUser)).isFalse();
    }

    // --- getPlaylistsByOwner ---

    @Test
    void getPlaylistsByOwner_returnsMappedResponses() {
        when(playlistRepository.findByOwner(owner)).thenReturn(List.of(playlist));

        List<PlaylistResponse> result = playlistService.getPlaylistsByOwner(owner);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Test Playlist");
    }

    @Test
    void getPlaylistsByOwner_returnsEmpty_whenOwnerHasNoPlaylists() {
        when(playlistRepository.findByOwner(owner)).thenReturn(List.of());

        List<PlaylistResponse> result = playlistService.getPlaylistsByOwner(owner);

        assertThat(result).isEmpty();
    }

    // --- createPlaylist ---

    @Test
    void createPlaylist_savesAndReturnsPlaylist() {
        when(playlistRepository.save(any(Playlist.class))).thenAnswer(inv -> inv.getArgument(0));

        Playlist result = playlistService.createPlaylist(owner, "My Playlist");

        assertThat(result.getName()).isEqualTo("My Playlist");
        assertThat(result.getOwner()).isEqualTo(owner);
        verify(playlistRepository).save(any(Playlist.class));
    }

    // --- addTrack ---

    @Test
    void addTrack_addsEntryToPlaylistAndReturnsResponse() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(trackRepository.findById(trackId)).thenReturn(Optional.of(track));
        when(playlistRepository.save(any(Playlist.class))).thenAnswer(inv -> {
            Playlist saved = inv.getArgument(0);

            // Simulate the JPA generating an ID for the newly created entry to avoid a NullPointerException
            saved.getEntries().forEach(e -> {
                if (e.getId() == null) {
                    e.setId(UUID.randomUUID());
                }
            });

            return saved;
        });

        PlaylistResponse result = playlistService.addTrack(playlistId.toString(), trackId.toString(), owner);

        assertThat(playlist.getEntries()).hasSize(1);
        assertThat(playlist.getEntries().getFirst().getTrack()).isEqualTo(track);
        assertThat(result.name()).isEqualTo("Test Playlist");
        assertThat(result.ownerId()).isEqualTo(ownerId.toString());
        assertThat(result.entries()).hasSize(1);
        assertThat(result.entries().getFirst().trackId()).isEqualTo(trackId.toString());
        verify(playlistRepository).save(playlist);
    }

    @Test
    void addTrack_throwsResourceNotFoundException_whenPlaylistNotFound() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.addTrack(playlistId.toString(), trackId.toString(), owner))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(playlistId.toString());

        verify(trackRepository, never()).findById(any());
    }

    @Test
    void addTrack_throwsGenericException_whenRequesterIsNotOwner() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.addTrack(playlistId.toString(), trackId.toString(), otherUser))
                .isInstanceOf(GenericException.class);

        verify(trackRepository, never()).findById(any());
    }

    @Test
    void addTrack_throwsResourceNotFoundException_whenTrackNotFound() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(trackRepository.findById(trackId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.addTrack(playlistId.toString(), trackId.toString(), owner))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(trackId.toString());
    }

    // --- removeTrack ---

    @Test
    void removeTrack_removesEntryFromPlaylistAndDeletesIt() {
        playlist.getEntries().add(entry);

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(playlistEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        playlistService.removeTrack(playlistId.toString(), entryId.toString(), owner);

        assertThat(playlist.getEntries()).isEmpty();
        verify(playlistEntryRepository).delete(entry);
        verify(playlistRepository).save(playlist);
    }

    @Test
    void removeTrack_throwsResourceNotFoundException_whenPlaylistNotFound() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.removeTrack(playlistId.toString(), entryId.toString(), owner))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(playlistId.toString());

        verify(playlistEntryRepository, never()).findById(any());
    }

    @Test
    void removeTrack_throwsResourceNotFoundException_whenEntryNotFound() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(playlistEntryRepository.findById(entryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.removeTrack(playlistId.toString(), entryId.toString(), owner))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(entryId.toString());

        verify(playlistEntryRepository, never()).delete(any());
    }

    // --- addIcon ---

    @Test
    void addIcon_writesImageFile_whenPlaylistExistsAndRequesterIsOwner() {
        MultipartFile file = mock(MultipartFile.class);
        BufferedImage image = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        try (
                MockedStatic<ContentUtils> contentUtilsMock = mockStatic(ContentUtils.class);
                MockedStatic<ImageIO> imageIOMock = mockStatic(ImageIO.class)
        ) {

            contentUtilsMock.when(() -> ContentUtils.convertToJpeg(file)).thenReturn(image);
            // We don't use the boolean returned by ImageIO.write, defaulting to false is fine
            imageIOMock.when(() -> ImageIO.write(eq(image), eq("jpg"), any(File.class))).thenReturn(false);

            playlistService.addIcon(file, playlistId.toString(), owner);

            // Confirm the image conversion and write both actually occurred
            contentUtilsMock.verify(() -> ContentUtils.convertToJpeg(file));
            imageIOMock.verify(() -> ImageIO.write(eq(image), eq("jpg"), any(File.class)));
        }
    }

    @Test
    void addIcon_throwsGenericException_whenPlaylistDoesNotExist() {
        MultipartFile file = mock(MultipartFile.class);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.addIcon(file, playlistId.toString(), owner))
                .isInstanceOf(GenericException.class)
                .hasMessageContaining("Could not set icon");
    }

    @Test
    void addIcon_throwsGenericException_onIOException() {
        MultipartFile file = mock(MultipartFile.class);
        BufferedImage image = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        try (
                MockedStatic<ContentUtils> contentUtilsMock = mockStatic(ContentUtils.class);
                MockedStatic<ImageIO> imageIOMock = mockStatic(ImageIO.class)
        ) {

            contentUtilsMock.when(() -> ContentUtils.convertToJpeg(file)).thenReturn(image);
            imageIOMock.when(() -> ImageIO.write(any(), any(), any(File.class)))
                    .thenThrow(new IOException("Write failed"));

            assertThatThrownBy(() -> playlistService.addIcon(file, playlistId.toString(), owner))
                    .isInstanceOf(GenericException.class)
                    .hasMessageContaining("Image I/O error");
        }
    }

    @Test
    void addIcon_throwsGenericException_whenConversionFails() {
        MultipartFile file = mock(MultipartFile.class);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        try (MockedStatic<ContentUtils> contentUtilsMock = mockStatic(ContentUtils.class)) {
            contentUtilsMock.when(() -> ContentUtils.convertToJpeg(file))
                    .thenThrow(new GenericException("Could not convert image to JPEG"));

            assertThatThrownBy(() -> playlistService.addIcon(file, playlistId.toString(), owner))
                    .isInstanceOf(GenericException.class)
                    .hasMessageContaining("Could not convert image to JPEG");
        }
    }

    // --- removeIcon ---

    @Test
    void removeIcon_deletesIconFile_whenPlaylistExists() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.deleteIfExists(any())).thenReturn(true);

            playlistService.removeIcon(playlistId.toString(), owner);

            filesMock.verify(() -> Files.deleteIfExists(
                    Paths.get(ICON + playlistId + ICON_EXTENSION)));
        }
    }

    @Test
    void removeIcon_doesNothing_whenPlaylistDoesNotExist() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            playlistService.removeIcon(playlistId.toString(), owner);

            // If playlistExists returns false, deleteIfExists must never be called
            filesMock.verify(() -> Files.deleteIfExists(any()), never());
        }
    }

    @Test
    void removeIcon_throwsGenericException_onIOException() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.deleteIfExists(any()))
                    .thenThrow(new IOException("permission denied"));

            assertThatThrownBy(() -> playlistService.removeIcon(playlistId.toString(), owner))
                    .isInstanceOf(GenericException.class)
                    .hasMessageContaining("Could not delete playlist icon");
        }
    }

    // --- deletePlaylist ---

    @Test
    void deletePlaylist_deletesPlaylistAndIcon() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.deleteIfExists(any())).thenReturn(true);

            playlistService.deletePlaylist(playlistId.toString(), owner);

            verify(playlistRepository).delete(playlist);
            filesMock.verify(() -> Files.deleteIfExists(
                    Paths.get(ICON + playlistId + ICON_EXTENSION)));
        }
    }

    @Test
    void deletePlaylist_throwsResourceNotFoundException_whenPlaylistNotFound() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.deletePlaylist(playlistId.toString(), owner))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(playlistRepository, never()).delete(any());
    }

    @Test
    void deletePlaylist_throwsGenericException_whenRequesterIsNotOwner() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.deletePlaylist(playlistId.toString(), otherUser))
                .isInstanceOf(GenericException.class);

        verify(playlistRepository, never()).delete(any());
    }
}
