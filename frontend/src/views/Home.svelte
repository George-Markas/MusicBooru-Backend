<script lang="ts">
    import { type AppState } from "../lib/api/common";
    import { type Track, get_tracks, search_tracks, sort_tracks, stream_track} from "../lib/api/track";

    import { getContext, setContext, onMount } from "svelte";
    import { logout } from "../lib/api/auth"

    import TrackList from "./components/TrackList.svelte";
    import TrackPlayer from "./components/TrackPlayer.svelte";

    const app = getContext<{page: AppState }>('app');

    let tracklist = $state<Track[]>([])
    let streamTrack = $state({id: ''});
    setContext('stream', streamTrack);

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

<TrackList data={tracklist}/>
<TrackPlayer track_id={streamTrack.id}/>
<button onclick={handleLogout}>Logout</button>
