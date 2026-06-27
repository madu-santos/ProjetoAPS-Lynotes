const PLAYER_STORAGE_KEY = 'lynotes_player_state';

const playerState = {
    playlists: [],
    playlistItems: [],
    currentIndex: 0,
    playing: false,
    currentPlaylistId: null,
    currentVideoId: null,
    currentTitle: null,
    currentTime: 0
};

let ytPlayer = null;
let ytReady = false;
let saveInterval = null;

function playerElements() {
    return {
        container: document.getElementById('youtube-player'),
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

// --- Persistência via localStorage ---

function playerSaveState() {
    try {
        let currentTime = 0;
        if (ytPlayer && typeof ytPlayer.getCurrentTime === 'function') {
            try {
                currentTime = ytPlayer.getCurrentTime() || 0;
            } catch (e) {
                currentTime = playerState.currentTime || 0;
            }
        }
        const data = {
            currentVideoId: playerState.currentVideoId,
            currentTitle: playerState.currentTitle,
            currentPlaylistId: playerState.currentPlaylistId,
            currentIndex: playerState.currentIndex,
            playing: playerState.playing,
            playlistItems: playerState.playlistItems,
            currentTime: currentTime,
            timestamp: Date.now()
        };
        localStorage.setItem(PLAYER_STORAGE_KEY, JSON.stringify(data));
    } catch (e) {
        // localStorage indisponível
    }
}

function playerRestoreState() {
    try {
        const raw = localStorage.getItem(PLAYER_STORAGE_KEY);
        if (!raw) {
            return null;
        }
        const data = JSON.parse(raw);
        // Expirar após 4 horas
        if (Date.now() - data.timestamp > 4 * 60 * 60 * 1000) {
            localStorage.removeItem(PLAYER_STORAGE_KEY);
            return null;
        }
        return data;
    } catch (e) {
        return null;
    }
}

// --- YouTube IFrame API ---

function onYouTubeIframeAPIReady() {
    ytReady = true;
    const { container } = playerElements();
    if (!container) {
        return;
    }

    ytPlayer = new YT.Player('youtube-player', {
        height: '100%',
        width: '100%',
        playerVars: {
            enablejsapi: 1,
            rel: 0,
            modestbranding: 1,
            playsinline: 1
        },
        events: {
            onReady: onPlayerReady,
            onStateChange: onPlayerStateChange
        }
    });
}

function onPlayerReady() {
    // Restaurar estado salvo após o player estar pronto
    const savedState = playerRestoreState();
    if (savedState && savedState.currentVideoId && savedState.playing) {
        playerState.currentVideoId = savedState.currentVideoId;
        playerState.currentTitle = savedState.currentTitle;
        playerState.currentPlaylistId = savedState.currentPlaylistId;
        playerState.currentIndex = savedState.currentIndex || 0;
        playerState.playlistItems = savedState.playlistItems || [];
        playerState.currentTime = savedState.currentTime || 0;
        playerState.playing = true;

        const { currentTitle } = playerElements();
        if (currentTitle) {
            currentTitle.textContent = playerState.currentTitle || 'Nenhum item carregado';
        }

        // Carregar o vídeo a partir da posição salva
        ytPlayer.loadVideoById({
            videoId: playerState.currentVideoId,
            startSeconds: playerState.currentTime
        });

        playerRenderTracks();
    } else if (savedState && savedState.currentVideoId) {
        // Estava pausado: mostrar info mas não tocar
        playerState.currentVideoId = savedState.currentVideoId;
        playerState.currentTitle = savedState.currentTitle;
        playerState.currentPlaylistId = savedState.currentPlaylistId;
        playerState.currentIndex = savedState.currentIndex || 0;
        playerState.playlistItems = savedState.playlistItems || [];
        playerState.currentTime = savedState.currentTime || 0;
        playerState.playing = false;

        const { currentTitle } = playerElements();
        if (currentTitle) {
            currentTitle.textContent = playerState.currentTitle || 'Nenhum item carregado';
        }

        // Carregar o vídeo pausado (cueVideoById não auto-reproduz)
        ytPlayer.cueVideoById({
            videoId: playerState.currentVideoId,
            startSeconds: playerState.currentTime
        });

        playerRenderTracks();
    }

    // Salvar posição a cada segundo
    if (saveInterval) {
        clearInterval(saveInterval);
    }
    saveInterval = setInterval(playerSaveState, 1000);
}

function onPlayerStateChange(event) {
    if (event.data === YT.PlayerState.PLAYING) {
        playerState.playing = true;
        playerRenderTracks();
    } else if (event.data === YT.PlayerState.PAUSED) {
        playerState.playing = false;
        playerSaveState();
    } else if (event.data === YT.PlayerState.ENDED) {
        // Tocar próxima faixa automaticamente
        playerState.playing = false;
        if (playerState.playlistItems.length > 0 && playerState.currentIndex < playerState.playlistItems.length - 1) {
            playerState.currentIndex += 1;
            playerLoadTrack(playerState.playlistItems[playerState.currentIndex]);
        } else {
            // Fim da playlist
            playerRenderTracks();
            playerSaveState();
        }
    }
}

// --- Controles do Player ---

function playerLoad(videoId, title, playlistId, startSeconds) {
    if (!ytPlayer || !ytReady || !videoId) {
        return;
    }

    playerState.currentVideoId = videoId;
    playerState.currentTitle = title || `YouTube - ${videoId}`;
    playerState.currentPlaylistId = playlistId || playerState.currentPlaylistId;
    playerState.playing = true;
    playerState.currentTime = startSeconds || 0;

    const { currentTitle } = playerElements();
    if (currentTitle) {
        currentTitle.textContent = playerState.currentTitle;
    }

    ytPlayer.loadVideoById({
        videoId: videoId,
        startSeconds: startSeconds || 0
    });

    playerSetMessage('');
    playerRenderTracks();
    playerSaveState();
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
        playerSaveState();
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
    if (!ytPlayer || !ytReady) {
        return;
    }

    const state = ytPlayer.getPlayerState();

    // Se não há vídeo carregado, carregar da playlist
    if (state === -1 || state === YT.PlayerState.CUED) {
        if (!playerState.currentVideoId && playerState.playlistItems.length) {
            playerLoadFromSelectedPlaylist();
            return;
        }
        ytPlayer.playVideo();
        playerState.playing = true;
        return;
    }

    if (playerState.playing) {
        ytPlayer.pauseVideo();
        playerState.playing = false;
    } else {
        ytPlayer.playVideo();
        playerState.playing = true;
    }
    playerSaveState();
}

function playerLoadTrack(item) {
    if (!item) {
        return;
    }

    playerState.currentIndex = playerState.playlistItems.findIndex(track => track.id === item.id);
    if (playerState.currentIndex === -1) {
        playerState.currentIndex = 0;
    }
    playerLoad(item.videoId, item.titulo, item.playlistId, 0);
}

function playerPlayUrlNow() {
    const { urlInput } = playerElements();
    try {
        const videoId = playerExtractVideoId(urlInput ? urlInput.value : '');
        playerLoad(videoId, `YouTube - ${videoId}`, null, 0);
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

// --- API de Playlists ---

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

    playerSaveState();
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

// --- Inicialização ---

async function initPlayerModule() {
    const { playlistSelect } = playerElements();
    if (!playlistSelect) {
        return;
    }

    const savedState = playerRestoreState();

    if (savedState && savedState.currentPlaylistId) {
        await playerLoadPlaylists(savedState.currentPlaylistId).catch(() => {
            playerSetMessage('Falha ao carregar playlists.');
        });

        // Restaurar itens da playlist
        if (savedState.playlistItems && savedState.playlistItems.length) {
            playerState.playlistItems = savedState.playlistItems;
            playerState.currentPlaylistId = savedState.currentPlaylistId;
            playerState.currentIndex = savedState.currentIndex || 0;
        } else {
            await playerFetchPlaylist(savedState.currentPlaylistId, false).catch(() => {});
            playerState.currentIndex = savedState.currentIndex || 0;
        }

        // Mostrar título enquanto o YT API carrega
        if (savedState.currentTitle) {
            const { currentTitle } = playerElements();
            if (currentTitle) {
                currentTitle.textContent = savedState.currentTitle;
            }
        }

        playerRenderTracks();
    } else {
        await playerLoadPlaylists().catch(() => {
            playerSetMessage('Falha ao carregar playlists.');
        });
    }

    // Carregar YouTube IFrame API
    if (!window.YT || !window.YT.Player) {
        const tag = document.createElement('script');
        tag.src = 'https://www.youtube.com/iframe_api';
        document.head.appendChild(tag);
    } else {
        onYouTubeIframeAPIReady();
    }
}

// Salvar estado antes de sair da página
window.addEventListener('beforeunload', playerSaveState);

window.addEventListener('DOMContentLoaded', initPlayerModule);
