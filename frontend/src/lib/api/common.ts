export const BASE_URL = 'http://localhost:8080/api';

export type SortMode = 'album' | 'title' | 'artist';
export type ViewMode = 'Album' | 'Track'; 
export type AppState = 'login' | 'home' | 'playlists' | 'error' | 'loading';
export type Result<T> = {ok: true, status: number, data: T} | {ok: false, status: number};
 
export async function api<T>(
    url: string, 
    options: RequestInit = {},
    extras?: { params?: string, as?: 'json' | 'blob' }
) : Promise<Result<T>> {

    const hasExtras = extras?.params !== undefined; 
    const query = hasExtras ? `?query=${encodeURIComponent(extras.params as string)}` : '';

    const response = await fetch(`${BASE_URL}/${url}${query}`, {
            credentials: 'include',
            ...options,
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            }
        }
    );

    if (!response.ok) {
        return {ok: false, status: response.status};
    }

    const data = extras?.as === 'blob'
    ? await response.blob()
    : await response.json();

    return {ok: true, status: response.status, data};
}