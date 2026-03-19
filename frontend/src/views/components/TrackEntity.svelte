<script lang="ts">
    import { getContext} from "svelte";
    import { type Track} from "../../lib/api/track";

    let { trackData} = $props<{ 
        trackData: Track 
    }>();

    const track = getContext<{id: string}>('stream');

</script>

<button class='list' ondblclick={() => track.id = trackData.id}>    
    <p class="track-title">{trackData.title}</p>
</button>

<style>
    .list {
        display: flex;
        align-items: left;
        gap: 0.1rem;
        width: 100%;
        padding: 0.1rem 0.1rem;
        border: none;
        border-bottom: 1px solid #eee;
        background: none;
        cursor: pointer;
        text-align: left;
        font-size: 0.8rem;
    }
</style>