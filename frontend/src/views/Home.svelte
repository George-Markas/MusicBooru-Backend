<script lang="ts">
    import { getContext, onMount } from "svelte";
    import { type AppState } from "../lib/api/common";
    import { logout } from "../lib/api/auth"
    import { type Track, get_tracks, search_tracks, sort_tracks} from "../lib/api/track";

    import TrackEntity from "./components/TrackEntity.svelte";

    const app = getContext<{page: AppState }>('app');

    async function handleLogout() {
        try {
            const response = await logout();
            if (response.ok) {app.page = 'login'; console.log(response.data);}
        } catch (error) {
            console.error(error);
        }
    }

    let tracklist = $state<Track[]>([])

    onMount( async () => {
        try {
            const response = await sort_tracks('artist');
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

<style>
    .track-list {
        display: flex;
        flex-wrap: wrap;
        gap: 16px;
    }
</style>