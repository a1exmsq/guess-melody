package com.guessmelody.service;

import com.guessmelody.model.entity.Playlist;
import com.guessmelody.model.entity.Track;
import com.guessmelody.repository.PlaylistRepository;
import com.neovisionaries.i18n.CountryCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Category;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyCategoryService {

    private final SpotifyPlaylistImporter playlistImporter;
    private final PlaylistRepository playlistRepository;
    private final SecureRandom random = new SecureRandom();

    private static final List<String> DEFAULT_CATEGORIES = Arrays.asList(
            "toplists", "pop", "rock", "hiphop", "edm_dance",
            "mood", "party", "workout", "focus", "decades"
    );

    public List<Category> getCategories(SpotifyApi api) throws Exception {
        Paging<Category> categories = api.getListOfCategories()
                .country(CountryCode.PL)
                .limit(20)
                .build()
                .execute();
        return Arrays.asList(categories.getItems());
    }

    public List<PlaylistSimplified> getCategoryPlaylists(SpotifyApi api, String categoryId) throws Exception {
        Paging<PlaylistSimplified> playlists = api.getCategorysPlaylists(categoryId)
                .country(CountryCode.PL)
                .limit(20)
                .build()
                .execute();
        return Arrays.asList(playlists.getItems());
    }

    @Transactional
    public Playlist importRandomPlaylist(SpotifyApi api) throws Exception {
        String categoryId = DEFAULT_CATEGORIES.get(random.nextInt(DEFAULT_CATEGORIES.size()));
        log.info("Picking a random playlist from category: {}", categoryId);

        List<PlaylistSimplified> playlists = getCategoryPlaylists(api, categoryId);
        if (playlists.isEmpty()) {
            throw new IllegalStateException("No playlists found in category: " + categoryId);
        }

        int index = random.nextInt(Math.max(1, playlists.size() - 2)) + 2;
        if (index >= playlists.size()) index = 0;

        PlaylistSimplified selected = playlists.get(index);
        String playlistId = selected.getId();
        log.info("Selected playlist: {} ({})", selected.getName(), playlistId);

        var existing = playlistRepository.findBySpotifyPlaylistId(playlistId);
        if (existing.isPresent()) {
            log.info("Playlist {} already imported, returning existing", playlistId);
            return existing.get();
        }

        Playlist playlist = playlistImporter.importPlaylist(api, playlistId);

        playlist.getTracks().forEach(track -> track.setPlaylist(playlist));
        Playlist saved = playlistRepository.save(playlist);
        log.info("Saved random playlist '{}' with {} tracks", saved.getName(), saved.getTracks().size());

        return saved;
    }
}
