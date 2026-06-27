const playerState = {
    playlists: [],
    playlistItems: [],
    currentIndex: 0,
    playing: false,
    currentPlaylistId: null
};

function playerElements() {
    return {
        iframe: document.getElementById('youtube-player'),
        currentTitle: document.getElementById('player-current-title'),
        message: document.getElementById('player-message'),
        playlistSelect: document.getElementById('player-playlist-select'),
        urlInput: document.getElementById('player-youtube-url'),
        newPlaylistInput: document.getElementById('player-new-playlist-name'),
        trackList: document.getElementById('player-track-list')
    };
}

function playerSetMessage(message, isError = true) {
    const { message: messageElement } = playerElements();
    if (!messageElement) {
        return;
    }
    messageElement.textContent = message || '';
    messageElement.style.color = isError ? '#f8d7da' : '#badbcc';
}

function playerExtractVideoId(url) {
    if (!url || !url.trim()) {
        throw new Error('Informe um link do YouTube.');
    }

    const query = url.trim();
    let parsedUrl;
    try {
        parsedUrl = new URL(query.startsWith('http') ? query : `https://${query}`);
    } catch (error) {
        throw new Error('Informe um link do YouTube válido.');
    }

    const host = parsedUrl.hostname.replace(/^www\./, '');
    if (host === 'youtube.com' || host === 'm.youtube.com') {
        const videoId = parsedUrl.searchParams.get('v');
        if (videoId) {
            return videoId;
        }
    }

    if (host === 'youtu.be') {
        const videoId = parsedUrl.pathname.replace('/', '').split('/')[0];
        if (videoId) {
            return videoId;
        }
    }

    throw new Error('A URL precisa ser um link do YouTube válido.');
}

function playerLoad(videoId, title, playlistId) {
    const { iframe, currentTitle } = playerElements();
    if (!iframe || !currentTitle || !videoId) {
        return;
    }

    iframe.src = `https://www.youtube.com/embed/${videoId}?enablejsapi=1&rel=0&modestbranding=1&autoplay=1`;
    playerState.playing = true;
    playerState.currentPlaylistId = playlistId || playerState.currentPlaylistId;
    currentTitle.textContent = title || `YouTube - ${videoId}`;
    playerSetMessage('');
    playerRenderTracks();
}

function playerSendCommand(command) {
    const { iframe } = playerElements();
    if (!iframe || !iframe.contentWindow) {
        return;
    }

    iframe.contentWindow.postMessage(JSON.stringify({
        event: 'command',
        func: command,
        args: []
    }), '*');
}

function playerLoadFromSelectedPlaylist() {
    if (!playerState.playlistItems.length) {
        playerSetMessage('Selecione uma playlist com faixas.');
        return;
    }
    playerState.currentIndex = 0;
    playerLoadTrack(playerState.playlistItems[0]);
}

function playerSelectPlaylist() {
    const { playlistSelect } = playerElements();
    if (!playlistSelect) {
        return;
    }

    const playlistId = playlistSelect.value;
    if (!playlistId) {
        playerState.currentPlaylistId = null;
        playerState.playlistItems = [];
        playerRenderTracks();
        return;
    }

    playerFetchPlaylist(playlistId, true);
}

function playerPrev() {
    if (!playerState.playlistItems.length) {
        return;
    }
    playerState.currentIndex = Math.max(0, playerState.currentIndex - 1);
    playerLoadTrack(playerState.playlistItems[playerState.currentIndex]);
}

function playerNext() {
    if (!playerState.playlistItems.length) {
        return;
    }
    if (playerState.currentIndex < playerState.playlistItems.length - 1) {
        playerState.currentIndex += 1;
    }
    playerLoadTrack(playerState.playlistItems[playerState.currentIndex]);
}

function playerPlayPause() {
    const { iframe } = playerElements();
    if (!iframe || !iframe.src || iframe.src.endsWith('/embed/?enablejsapi=1&rel=0&modestbranding=1')) {
        playerLoadFromSelectedPlaylist();
        return;
    }

    if (playerState.playing) {
        playerSendCommand('pauseVideo');
        playerState.playing = false;
    } else {
        playerSendCommand('playVideo');
        playerState.playing = true;
    }
}

function playerLoadTrack(item) {
    if (!item) {
        return;
    }

    playerState.currentIndex = playerState.playlistItems.findIndex(track => track.id === item.id);
    if (playerState.currentIndex === -1) {
        playerState.currentIndex = 0;
    }
    playerLoad(item.videoId, item.titulo, item.playlistId);
}

function playerPlayUrlNow() {
    const { urlInput } = playerElements();
    try {
        const videoId = playerExtractVideoId(urlInput ? urlInput.value : '');
        playerLoad(videoId, `YouTube - ${videoId}`, null);
    } catch (error) {
        playerSetMessage(error.message);
    }
}

async function playerSaveUrl() {
    const { urlInput, newPlaylistInput, playlistSelect } = playerElements();
    const url = urlInput ? urlInput.value.trim() : '';
    const newPlaylistName = newPlaylistInput ? newPlaylistInput.value.trim() : '';
    let playlistId = playlistSelect ? playlistSelect.value : '';

    try {
        playerExtractVideoId(url);
        playerSetMessage('');

        if (newPlaylistName) {
            const playlist = await playerCreatePlaylist(newPlaylistName);
            playlistId = playlist.id;
        } else if (!playlistId) {
            const playlist = await playerCreatePlaylist('Minha playlist');
            playlistId = playlist.id;
        }

        const item = await playerAddItem(playlistId, url);
        await playerLoadPlaylists(playlistId);
        await playerFetchPlaylist(playlistId, false);
        playerLoadTrack(item);

        if (urlInput) {
            urlInput.value = '';
        }
        if (newPlaylistInput) {
            newPlaylistInput.value = '';
        }
        playerSetMessage('Link salvo na playlist.', false);
    } catch (error) {
        playerSetMessage(error.message || 'Não foi possível salvar o link.');
    }
}

async function playerLoadPlaylists(selectedPlaylistId) {
    const { playlistSelect } = playerElements();
    if (!playlistSelect) {
        return;
    }

    const response = await fetch('/api/playlists');
    if (!response.ok) {
        throw new Error('Falha ao carregar playlists.');
    }

    const playlists = await response.json();
    playerState.playlists = playlists;
    playlistSelect.innerHTML = '<option value="">Selecione uma playlist</option>';
    playlists.forEach(playlist => {
        const option = document.createElement('option');
        option.value = playlist.id;
        option.textContent = playlist.nome;
        option.selected = playlist.id === selectedPlaylistId;
        playlistSelect.appendChild(option);
    });
}

async function playerFetchPlaylist(playlistId, autoplay) {
    const response = await fetch(`/api/playlists/${playlistId}`);
    if (!response.ok) {
        playerSetMessage('Falha ao carregar playlist.');
        return;
    }

    const payload = await response.json();
    playerState.currentPlaylistId = payload.playlist.id;
    playerState.playlistItems = payload.items || [];
    playerState.currentIndex = 0;
    playerRenderTracks();

    if (autoplay && playerState.playlistItems.length) {
        playerLoadTrack(playerState.playlistItems[0]);
    }
}

async function playerCreatePlaylist(nome) {
    const response = await fetch('/api/playlists', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ nome })
    });
    return playerReadJsonResponse(response, 'Não foi possível criar a playlist.');
}

async function playerAddItem(playlistId, url) {
    const response = await fetch(`/api/playlists/${playlistId}/items`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ url })
    });
    return playerReadJsonResponse(response, 'Não foi possível salvar o link.');
}

async function playerReadJsonResponse(response, fallbackMessage) {
    const payload = await response.json();
    if (!response.ok) {
        throw new Error(payload.erro || fallbackMessage);
    }
    return payload;
}

function playerRenderTracks() {
    const { trackList } = playerElements();
    if (!trackList) {
        return;
    }

    trackList.innerHTML = '';
    playerState.playlistItems.forEach((item, index) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'player-track-button';
        if (index === playerState.currentIndex && playerState.playing) {
            button.classList.add('active');
        }
        button.textContent = item.titulo || `YouTube - ${item.videoId}`;
        button.addEventListener('click', () => playerLoadTrack(item));
        trackList.appendChild(button);
    });
}

function initPlayerModule() {
    const { playlistSelect } = playerElements();
    if (!playlistSelect) {
        return;
    }

    playerLoadPlaylists().catch(() => {
        playerSetMessage('Falha ao carregar playlists.');
    });
}

window.addEventListener('DOMContentLoaded', initPlayerModule);
