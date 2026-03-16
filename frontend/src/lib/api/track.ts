import { api, type Result } from "./common"

export type Track = {
    id: string,
    title: string,
    artist: string,
    album: string,
    genre: string, 
    year: string,
    filename: string
}

export async function get_tracks(): Promise<Result<Track[]>> {
    const path = `track`

    const response = await api<Track[]>(path, {
        method: 'GET'
    });

    return response;
}

export async function search_tracks(query: string): Promise<Result<Track[]>> {
    const path = `track/search`

    const response = await api<Track[]>(path, {
        method: 'GET'
    }, {params: query});

    return response;
}

export async function sort_tracks(sortOption: string): Promise<Result<Track[]>> {
    const path = `track/sort/${sortOption}`

    const response = await api<Track[]>(path, {
        method: 'GET'
    });

    return response;
}

export async function get_track(id: string): Promise<Result<Track>> {
    const path = `track/${id}`

    const response = await api<Track>(path, {
        method: 'GET'
    });

    return response;
}

export async function get_track_art(id: string): Promise<Result<Blob>> {
    const path = `track/art/${id}`

    const response = await api<Blob>(path, {
        method: 'GET',
        headers: {'Accept': 'image/jpeg'}
    }, {params: '', as: 'blob'})

    return response;
}

export async function stream_track(id: string): Promise<Result<Blob>> {
    const path = `stream/${id}`

    const response = await api<Blob>(path, {
        method: 'GET',
    }, {params: '', as: 'blob'});

    return response;
}