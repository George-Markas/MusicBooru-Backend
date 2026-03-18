<script lang="ts">
    import type { ViewMode } from "../../lib/api/common";
    import { search_tracks, sort_tracks, type Track } from "../../lib/api/track";

    import { getContext } from "svelte";

    let query_input = $state('');
    let debounceTimer: ReturnType<typeof setTimeout>;

    const tracks = getContext<{ list: Track[] }>('tracklist');
    const view = getContext<{mode: ViewMode}>('view');

    async function handleSearch(query: string) {
        try {
            const response = await search_tracks(query);
            if (response.ok) {
                tracks.list = response.data;
            }
        } catch (error) {
            console.error(error);
        }
    }

    async function handleSort(by: string) {
        try {
            const response = await sort_tracks(by);
            if (response.ok) {
                tracks.list = response.data;
            }
        } catch (error) {
            console.error(error);
        }
    }

    function handleInputMulti(e: Event) {
        const query = (e.target as HTMLInputElement).value;

        if (query === 'title') {
            view.mode = 'Track';
        }

        handleSort(query);
    }

    function handleInputSearch(e: Event) {
        const query = (e.target as HTMLInputElement).value;
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout( () => {handleSearch(query)}, 300);
    }

</script>

<div>
    <input
        type="search"
        placeholder="Search from all tracks..."
        value={query_input}
        oninput={handleInputSearch}
    />

    <label for="options">Sort by:</label>
    <select id="options" onchange={handleInputMulti}>
        <option value="title">  Title  </option>
        <option value="artist"> Artist </option>
        <option value="album">  Album  </option>
    </select>

    <button onclick={() => {view.mode='Album'; handleSort('album')}}>Album</button>
    <button onclick={() => view.mode='Track'}>Track</button>
</div>