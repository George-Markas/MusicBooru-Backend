<script lang="ts">
    import type { SortMode, ViewMode } from "../../lib/api/common";
    import { searchTracks, sortTracks, type Track } from "../../lib/api/track";

    import { getContext } from "svelte";

    let debounceTimer: ReturnType<typeof setTimeout>;

    const tracks = getContext<{ list: Track[] }>('tracklist');
    const view = getContext<{mode: ViewMode}>('view');

    async function handleSearch(query: string) {
        try {
            const response = await searchTracks(query);
            if (response.ok) {
                tracks.list = response.data;
            }
        } catch (error) {
            console.error(error);
        }
    }

    async function handleSort(by: SortMode) {
        tracks.list = [...tracks.list].sort((a, b) =>
        a[by].localeCompare(b[by], undefined, { sensitivity: 'base' }))
    }

    function handleInputMulti(e: Event) {
        const query = (e.target as HTMLInputElement).value as SortMode;

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
        oninput={handleInputSearch}
    />

    <label for="options">Sort by:</label>
    <select id="options" onchange={handleInputMulti}>
        {#if view.mode === 'Track'}           
            <option value="title">  Title  </option>
        {/if}
        <option value="album">  Album  </option>
        <option value="artist"> Artist </option>
    </select>

    <button onclick={() => { 
        if (view.mode !== 'Album') {
            view.mode='Album'; 
            handleSort('album');
        }
        }}>Album</button>
    <button onclick={() => view.mode='Track'}>Track</button>
</div>