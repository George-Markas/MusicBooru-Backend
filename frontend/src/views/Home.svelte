<script lang="ts">
    import { getContext, onMount, setContext } from "svelte";
    import { type AppState } from "../lib/api/common";
    import { logout } from "../lib/api/auth"
    import { type Track, get_tracks, search_tracks, sort_tracks, stream_track} from "../lib/api/track";
    import TrackEntity from "./components/TrackEntity.svelte";

    const app = getContext<{page: AppState }>('app');

    let tracklist = $state<Track[]>([])

    let stream = $state({objectUrl: '' as string});
    setContext('stream', stream);

    async function handleLogout() {
        try {
            const response = await logout();
            if (response.ok) {app.page = 'login'; console.log(response.data);}
        } catch (error) {
            console.error(error);
        }
    }

    onMount( async () => {
        try {
            const response = await get_tracks();
            if (response.ok) {
                tracklist = response.data
            } else {
                app.page = 'login';
            }

        } catch (error) {
            console.error(error);
        }
    });

</script>

<p>88 == Welcome to musicbooru == 88</p>

<div style="display: flex; justify-content: flex-start;" class="track-list">
    {#each tracklist as track }
        <TrackEntity trackData={track}/>
    {/each}
</div>

<button onclick={handleLogout}>Logout</button>

<div>
    <audio src={stream.objectUrl} controls ></audio>
</div>

<style>
    .track-list {
        display: flex;
        flex-wrap: wrap;
        gap: 16px;
    }
</style>