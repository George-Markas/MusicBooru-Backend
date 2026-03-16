<script lang="ts">
    import { onMount } from "svelte";
    import { type Track, get_track_art } from "../../lib/api/track";


    let cover = $state<string>('');
    let { trackData } = $props<{ trackData: Track }>();

    onMount(async () => {
        try {
            const response = await get_track_art(trackData.id)
            if (response.ok) {
                cover = URL.createObjectURL(response.data);
            }
        } catch (error) {
            console.log(error);
        }


    })

</script>

<button>
    <img src={cover} alt="cover"/>
    <span>{trackData.title}</span>
</button>

<style>
    button {
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
</style>