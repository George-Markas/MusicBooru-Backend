<script lang="ts">
    import { untrack } from "svelte";
    import { streamTrack } from "../../lib/api/track";

    let {trackId} = $props<string>();
    let objectURL = $state<string>('')

    async function getStream(id: string) {
        try {
            const response = await streamTrack(id);
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
        if (trackId === '') return;
        getStream(trackId);

        return () => {
            untrack( () => {
                URL.revokeObjectURL(objectURL);
            })
        }
    });
</script>

<div>
    <audio src={objectURL} controls autoplay></audio>
</div>