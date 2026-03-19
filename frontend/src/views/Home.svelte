<script lang="ts">
    import { type AppState, type ViewMode } from "../lib/api/common";
    import { type Track, getTracks} from "../lib/api/track";

    import { getContext, setContext, onMount } from "svelte";
    import { logout } from "../lib/api/auth"

    import TrackList from "./components/TrackList.svelte";
    import TrackPlayer from "./components/TrackPlayer.svelte";
    import SearchBar from "./components/SearchBar.svelte";
    import AlbumList from "./components/AlbumList.svelte";

    const app = getContext<{page: AppState }>('app');

    let tracks = $state({list: [] as Track[]})
    setContext('tracklist', tracks);

    let albums = $derived(
        Object.values(
            tracks.list.reduce((acc, track) => {
                (acc[track.album] ??= []).push(track);
                return acc;
            }, {} as Record<string, Track[]>)
        )
    );

    let streamTrack = $state({id: ''});
    setContext('stream', streamTrack);

    let view = $state({mode: 'Track' as ViewMode});
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
            const response = await getTracks();
            if (response.ok) {
                tracks.list = response.data
            } else {
                app.page = 'login';
            }

        } catch (error) {
            console.error(error);
        }
    });

</script>

<p>88 == Welcome to musicbooru == 88</p>
<SearchBar/>
<button onclick={handleLogout}>Logout</button>
{#if view.mode === 'Album'}
    <AlbumList albums={albums}/>
{:else}   
    <TrackList data={tracks.list} mode={view.mode}/>
{/if}

<TrackPlayer trackId={streamTrack.id}/>
