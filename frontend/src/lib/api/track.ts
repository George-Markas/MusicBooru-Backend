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

export async function getTracks(): Promise<Result<Track[]>> {
    const path = `track`

    const response = await api<Track[]>(path, {
        method: 'GET'
    });

    return response;
}

export async function searchTracks(query: string): Promise<Result<Track[]>> {
    const path = `track/search`

    const response = await api<Track[]>(path, {
        method: 'GET'
    }, {params: query});

    return response;
}

export async function sortTracks(sortOption: string): Promise<Result<Track[]>> {
    const path = `track/sort/${sortOption}`

    const response = await api<Track[]>(path, {
        method: 'GET'
    });

    return response;
}

export async function getTrack(id: string): Promise<Result<Track>> {
    const path = `track/${id}`

    const response = await api<Track>(path, {
        method: 'GET'
    });

    return response;
}

export async function getTrackArt(id: string): Promise<Result<Blob>> {
    const path = `track/${id}/art`

    const response = await api<Blob>(path, {
        method: 'GET',
        headers: {'Accept': 'image/jpeg'}
    }, {params: '', as: 'blob'})

    return response;
}

export async function streamTrack(id: string): Promise<Result<Blob>> {
    const path = `stream/${id}`

    const response = await api<Blob>(path, {
        method: 'GET',
    }, {params: '', as: 'blob'});

    return response;
}