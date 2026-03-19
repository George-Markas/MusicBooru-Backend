<script lang="ts">
    import Login from './views/Login.svelte'
    import { getTracks } from './lib/api/track';
    import { type AppState } from './lib/api/common';

    import { onMount, setContext } from 'svelte';
    import Home from './views/Home.svelte';

    const app = $state({ page: 'loading' as AppState });
    setContext('app', app);

    $inspect(app.page)

    onMount( async () => {
        try {
            const response = await getTracks();
            if (response.ok) {app.page = 'home'}
            else {app.page = 'login'; console.log("You are not the father!");}
        } catch (error) {
            console.error(error);
        }
    });

</script>

{#if app.page === 'login'}
    <Login/>
{:else if app.page === 'home' }
    <Home/>
{/if}