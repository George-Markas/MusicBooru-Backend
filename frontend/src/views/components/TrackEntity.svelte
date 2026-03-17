<script lang="ts">
    import { getContext, onDestroy, onMount } from "svelte";
    import { type Track, get_track_art, stream_track } from "../../lib/api/track";
    import type { ViewMode } from "../../lib/api/common";


    let cover = $state<string>('');
    let { trackData, mode } = $props<{ 
        mode: ViewMode
        trackData: Track 
    }>();

    async function loadArt() {
        try {
            const response = await get_track_art(trackData.id)
            if (response.ok) {
                cover = URL.createObjectURL(response.data);
            }
        } catch (error) {
            console.log(error);
        }
    }

    const track = getContext<{id: string}>('stream');
    onMount(async () => {
        if (mode === 'card') {
            loadArt();
        }
    })

    onDestroy(async () => {
        if (cover) {
            console.log("Track gone")
            URL.revokeObjectURL(cover);
        }});
</script>

<button class:button={mode === 'card'} class:list={mode === 'list'} onclick={() => track.id = trackData.id}>    
    {#if mode === 'card'}
        <img src={cover} alt="cover"/>
        <span>{trackData.title}</span>
    {:else}
        <p class="track-title">{trackData.title}</p>
    {/if}
    
</button>

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

    .list {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    width: 100%;
    padding: 0.5rem 0.75rem;
    border: none;
    border-bottom: 1px solid #eee;
    background: none;
    cursor: pointer;
    text-align: left;
    font-size: 1rem;
}


</style>