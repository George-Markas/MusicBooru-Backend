<script lang="ts">
    import { getTrackArt, type Track } from "../../lib/api/track";
    import { getContext, onDestroy, onMount } from "svelte";
    import TrackList from "./TrackList.svelte";
    
    let { tracks } = $props<{tracks: Track[]}>();
    let cover = $state<string>('');
    
    let open = getContext<{name: string}>('openAlbum');
    let isOpen = $derived(open.name === tracks[0].album);

    async function loadArt() {
        try {
            const response = await getTrackArt(tracks[0].id)
            if (response.ok) {
                cover = URL.createObjectURL(response.data);
            }
        } catch (error) {
            console.log(error);
        }
    }

    onMount(async () => {
        loadArt();
    })

    onDestroy(async () => {
        if (cover) {
            console.log("Track gone")
            URL.revokeObjectURL(cover);
        }}
    );
</script>

<div class="album">
    <button class="button" onclick={() => open.name = isOpen ? null : tracks[0].album }>    
        <img src={cover} alt="cover"/>
        <span>{tracks[0].album}</span>
    </button>

    <div class="popup" class:open={isOpen}>
        <TrackList data={tracks} mode={'Track'}/>
    </div>
</div>

<style>
    .button {
        position: relative;
        padding: 0;
        border: none;
        cursor: pointer;
        width: 140px;
        height: 140px;
        overflow: hidden;
    }

    img {
        width: 100%;
        height: 100%;
        object-fit: cover;
    }

    span {
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        padding: 4px;
        background: rgba(0, 0, 0, 0.5);
        color: white;
        text-align: center;
    }

    .album {
        display: flex-start;
        flex-direction: column;
    }

    .popup {
        max-height: 0;
        overflow: hidden;
    }

    .popup.open {
        max-height: 300px;
    }
</style>