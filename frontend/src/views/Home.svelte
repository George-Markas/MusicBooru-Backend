<script lang="ts">
    import { type AppState, type ViewMode } from "../lib/api/common";
    import { type Track, get_tracks, sort_tracks, stream_track} from "../lib/api/track";

    import { getContext, setContext, onMount } from "svelte";
    import { logout } from "../lib/api/auth"

    import TrackList from "./components/TrackList.svelte";
    import TrackPlayer from "./components/TrackPlayer.svelte";
    import SearchBar from "./components/SearchBar.svelte";

    const app = getContext<{page: AppState }>('app');

    let tracks = $state({list: [] as Track[]})
    setContext('tracklist', tracks);

    let streamTrack = $state({id: ''});
    setContext('stream', streamTrack);

    let view = $state({mode: 'card' as ViewMode});
    setContext('view', view);
    
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
                tracks.list = response.data
            } else {
                app.page = 'login';
            }

        } catch (error) {
            console.error(error);
        }
    type ViewMode = 'card' | 'list'
    });

</script>

<p>88 == Welcome to musicbooru == 88</p>

<TrackList data={tracks.list} mode={view.mode}/>
<TrackPlayer track_id={streamTrack.id}/>
<SearchBar/>
<button onclick={handleLogout}>Logout</button>
