<script lang="ts">
    import type { ViewMode } from "../../lib/api/common";
    import type { Track } from "../../lib/api/track";
    import TrackEntity from "./TrackEntity.svelte";

    let { data, mode } = $props<{
        mode: ViewMode
        data: Track[]
    }>()

</script>

{#if mode === 'card'}
    <div style="display: flex; justify-content: flex-start;" class="track-list">
        {#each data as track (track.id)}
            <TrackEntity trackData={track}/>
        {/each}
    </div>

{:else}
    {#each data as track (track.id)}
        <TrackEntity trackData={track}/>
    {/each}
{/if}


<style>
    .track-list {
        display: flex;
        flex-wrap: wrap;
        gap: 16px;
    }
</style>