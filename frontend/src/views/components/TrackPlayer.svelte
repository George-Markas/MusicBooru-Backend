<script lang="ts">
    import { untrack } from "svelte";
    import { stream_track } from "../../lib/api/track";

    let {track_id} = $props<string>();
    let objectURL = $state<string>('')

    async function getStream(id: string) {
        try {
            const response = await stream_track(id);
            if (response.ok) {
                untrack(() => {
                    objectURL = URL.createObjectURL(response.data);
                })
            }
        } catch (error) {
            console.error(error);
        }
    }

    $effect(() => {
        getStream(track_id);

        return () => {
            untrack( () => {
                URL.revokeObjectURL(objectURL);
            })
        }
    });
</script>

<div>
    <audio src={objectURL} controls></audio>
</div>