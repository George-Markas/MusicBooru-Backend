<script lang="ts">
    import type { Track } from "../../lib/api/track";
    import TrackEntity from "./TrackEntity.svelte";

    let { data } = $props<{data: Track[]}>()

</script>

<div style="display: flex; justify-content: flex-start;" class="track-list">
    {#each data as track }
        <TrackEntity trackData={track}/>
    {/each}
</div>

<style>
    .track-list {
        display: flex;
        flex-wrap: wrap;
        gap: 16px;
    }
</style>