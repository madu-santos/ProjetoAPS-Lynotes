const PLAYER_STORAGE_KEY = 'lynotes_player_state';

const SVG_PLAY  = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M10.804 8 5 4.633v6.734L10.804 8zm.792-.696a.802.802 0 0 1 0 1.392l-6.363 3.692C4.713 12.69 4 12.345 4 11.692V4.308c0-.653.713-.998 1.233-.696l6.363 3.692z"/></svg>';
const SVG_PAUSE = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M5.5 3.5A1.5 1.5 0 0 1 7 5v6a1.5 1.5 0 0 1-3 0V5a1.5 1.5 0 0 1 1.5-1.5zm5 0A1.5 1.5 0 0 1 12 5v6a1.5 1.5 0 0 1-3 0V5a1.5 1.5 0 0 1 1.5-1.5z"/></svg>';

const playerState = {
    playlists: [],
    playlistItems: [],
    currentIndex: 0,
    playing: false,
    currentPlaylistId: null,
    currentVideoId: null,
    currentTitle: null,
    currentTime: 0,
    duration: 0,
    collapsed: false
};

let ytPlayer = null;
let ytReady = false;
let saveInterval = null;
let progressInterval = null;

// --- Elementos ---

function playerElements() {
    return {
        container:        document.getElementById('mini-player'),
        vinyl:            document.getElementById('player-vinyl'),
        currentTitle:     document.getElementById('player-current-title'),
        message:          document.getElementById('player-message'),
        playlistSelect:   document.getElementById('player-playlist-select'),
        urlInput:         document.getElementById('player-youtube-url'),
        newPlaylistInput: document.getElementById('player-new-playlist-name'),
        trackList:        document.getElementById('player-track-list'),
        progressFill:     document.getElementById('player-progress-fill'),
        progressThumb:    document.getElementById('player-progress-thumb'),
        progressBar:      document.getElementById('player-progress-bar'),
        timeCurrent:      document.getElementById('player-time-current'),
        timeTotal:        document.getElementById('player-time-total'),
        playBtn:          document.getElementById('player-play-btn'),
        toggleIcon:       document.querySelector('.player-toggle-icon')
    };
}

// --- Painel retrátil ---

function playerTogglePanel() {
    const { container } = playerElements();
    if (!container) return;
    playerState.collapsed = !playerState.collapsed;
    container.classList.toggle('collapsed', playerState.collapsed);

    // Recarrega playlists ao abrir o painel (pega qualquer nova playlist criada)
    if (!playerState.collapsed) {
        const currentSelected = playerState.currentPlaylistId;
        playerLoadPlaylists(currentSelected).catch(() => {});
    }

    playerSaveState();
}

// --- Mensagens ---

function playerSetMessage(message, isError = true) {
    const { message: msg } = playerElements();
    if (!msg) return;
    msg.textContent = message || '';
    msg.style.color = isError ? '#f8d7da' : '#86efac';
}

// --- Extrair videoId ---

function playerExtractVideoId(url) {
    if (!url || !url.trim()) throw new Error('Informe um link do YouTube.');
    const query = url.trim();
    let parsed;
    try {
        parsed = new URL(query.startsWith('http') ? query : `https://${query}`);
    } catch {
        throw new Error('Informe um link do YouTube válido.');
    }
    const host = parsed.hostname.replace(/^www\./, '');
    if (host === 'youtube.com' || host === 'm.youtube.com') {
        const v = parsed.searchParams.get('v');
        if (v) return v;
    }
    if (host === 'youtu.be') {
        const v = parsed.pathname.replace('/', '').split('/')[0];
        if (v) return v;
    }
    throw new Error('A URL precisa ser um link do YouTube válido.');
}

// --- Formatar tempo ---

function playerFormatTime(seconds) {
    if (!seconds || isNaN(seconds)) return '0:00';
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m}:${s.toString().padStart(2, '0')}`;
}

// --- Persistência ---

function playerSaveState() {
    try {
        let currentTime = playerState.currentTime;
        if (ytPlayer && typeof ytPlayer.getCurrentTime === 'function') {
            try { currentTime = ytPlayer.getCurrentTime() || 0; } catch {}
        }
        const data = {
            currentVideoId:   playerState.currentVideoId,
            currentTitle:     playerState.currentTitle,
            currentPlaylistId: playerState.currentPlaylistId,
            currentIndex:     playerState.currentIndex,
            playing:          playerState.playing,
            playlistItems:    playerState.playlistItems,
            currentTime,
            collapsed:        playerState.collapsed,
            timestamp:        Date.now()
        };
        localStorage.setItem(PLAYER_STORAGE_KEY, JSON.stringify(data));
    } catch {}
}

function playerRestoreState() {
    try {
        const raw = localStorage.getItem(PLAYER_STORAGE_KEY);
        if (!raw) return null;
        const data = JSON.parse(raw);
        if (Date.now() - data.timestamp > 4 * 60 * 60 * 1000) {
            localStorage.removeItem(PLAYER_STORAGE_KEY);
            return null;
        }
        return data;
    } catch {
        return null;
    }
}

// --- Atualizar barra de progresso ---

function playerUpdateProgress() {
    if (!ytPlayer || !ytReady) return;
    try {
        const current  = ytPlayer.getCurrentTime() || 0;
        const duration = ytPlayer.getDuration()    || 0;
        playerState.currentTime = current;
        playerState.duration    = duration;
        const pct = duration > 0 ? (current / duration) * 100 : 0;
        const { progressFill, progressThumb, timeCurrent, timeTotal } = playerElements();
        if (progressFill)  progressFill.style.width = `${pct}%`;
        if (progressThumb) progressThumb.style.left  = `${pct}%`;
        if (timeCurrent)   timeCurrent.textContent   = playerFormatTime(current);
        if (timeTotal)     timeTotal.textContent      = playerFormatTime(duration);
    } catch {}
}

// --- Seek (clique na barra) ---

function playerSeek(event) {
    if (!ytPlayer || !ytReady) return;
    try {
        const bar = event.currentTarget;
        const rect = bar.getBoundingClientRect();
        const pct = Math.max(0, Math.min(1, (event.clientX - rect.left) / rect.width));
        const duration = ytPlayer.getDuration() || 0;
        ytPlayer.seekTo(pct * duration, true);
        playerUpdateProgress();
    } catch {}
}

// --- YouTube IFrame API ---

function onYouTubeIframeAPIReady() {
    ytReady = true;
    const el = document.getElementById('youtube-player');
    if (!el) return;

    ytPlayer = new YT.Player('youtube-player', {
        height: '1',
        width:  '1',
        playerVars: { enablejsapi: 1, rel: 0, modestbranding: 1, playsinline: 1 },
        events: {
            onReady:       onPlayerReady,
            onStateChange: onPlayerStateChange
        }
    });
}

function onPlayerReady() {
    const saved = playerRestoreState();

    if (saved && saved.currentVideoId) {
        playerState.currentVideoId    = saved.currentVideoId;
        playerState.currentTitle      = saved.currentTitle;
        playerState.currentPlaylistId = saved.currentPlaylistId;
        playerState.currentIndex      = saved.currentIndex || 0;
        playerState.playlistItems     = saved.playlistItems || [];
        playerState.currentTime       = saved.currentTime  || 0;
        playerState.playing           = !!saved.playing;

        const { currentTitle } = playerElements();
        if (currentTitle) currentTitle.textContent = playerCleanTitle(playerState.currentTitle, playerState.currentVideoId);

        playerRenderTracks();

        if (playerState.playing) {
            ytPlayer.loadVideoById({ videoId: playerState.currentVideoId, startSeconds: playerState.currentTime });
        } else {
            ytPlayer.cueVideoById({ videoId: playerState.currentVideoId, startSeconds: playerState.currentTime });
        }
    }

    // Iniciar loop de progresso
    if (progressInterval) clearInterval(progressInterval);
    progressInterval = setInterval(playerUpdateProgress, 500);

    // Salvar posição periodicamente
    if (saveInterval) clearInterval(saveInterval);
    saveInterval = setInterval(playerSaveState, 1500);
}

function onPlayerStateChange(event) {
    const { vinyl, playBtn } = playerElements();

    if (event.data === YT.PlayerState.PLAYING) {
        playerState.playing = true;
        if (vinyl) vinyl.classList.add('spinning');
        if (playBtn) playBtn.innerHTML = SVG_PAUSE;
        playerRenderTracks();

    } else if (event.data === YT.PlayerState.PAUSED) {
        playerState.playing = false;
        if (vinyl) vinyl.classList.remove('spinning');
        if (playBtn) playBtn.innerHTML = SVG_PLAY;
        playerSaveState();

    } else if (event.data === YT.PlayerState.ENDED) {
        playerState.playing = false;
        if (vinyl) vinyl.classList.remove('spinning');
        if (playBtn) playBtn.innerHTML = SVG_PLAY;

        // Avançar automaticamente
        if (playerState.playlistItems.length > 0 &&
            playerState.currentIndex < playerState.playlistItems.length - 1) {
            playerState.currentIndex += 1;
            playerLoadTrack(playerState.playlistItems[playerState.currentIndex]);
        } else {
            playerRenderTracks();
            playerSaveState();
        }
    }
}

// --- Controles ---

function playerCleanTitle(title, videoId) {
    if (!title || title.startsWith('http') || title.startsWith('www')) {
        return `YouTube - ${videoId}`;
    }
    return title;
}

function playerLoad(videoId, title, playlistId, startSeconds) {
    if (!ytPlayer || !ytReady || !videoId) return;

    playerState.currentVideoId    = videoId;
    playerState.currentTitle      = playerCleanTitle(title, videoId);
    playerState.currentPlaylistId = playlistId || playerState.currentPlaylistId;
    playerState.playing           = true;
    playerState.currentTime       = startSeconds || 0;

    const { currentTitle } = playerElements();
    if (currentTitle) currentTitle.textContent = playerState.currentTitle;

    ytPlayer.loadVideoById({ videoId, startSeconds: startSeconds || 0 });
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
    if (!playlistSelect) return;
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
    if (!playerState.playlistItems.length) return;
    playerState.currentIndex = Math.max(0, playerState.currentIndex - 1);
    playerLoadTrack(playerState.playlistItems[playerState.currentIndex]);
}

function playerNext() {
    if (!playerState.playlistItems.length) return;
    if (playerState.currentIndex < playerState.playlistItems.length - 1) {
        playerState.currentIndex += 1;
    }
    playerLoadTrack(playerState.playlistItems[playerState.currentIndex]);
}

function playerPlayPause() {
    if (!ytPlayer || !ytReady) return;
    const state = ytPlayer.getPlayerState();
    if (state === -1 || state === YT.PlayerState.CUED) {
        if (playerState.currentVideoId) {
            ytPlayer.playVideo();
        } else if (playerState.playlistItems.length) {
            playerLoadFromSelectedPlaylist();
        }
        return;
    }
    if (playerState.playing) {
        ytPlayer.pauseVideo();
    } else {
        ytPlayer.playVideo();
    }
}

function playerLoadTrack(item) {
    if (!item) return;
    const idx = playerState.playlistItems.findIndex(t => t.id === item.id);
    playerState.currentIndex = idx === -1 ? 0 : idx;
    playerLoad(item.videoId, item.titulo, item.playlistId, 0);
}

function playerPlayUrlNow() {
    const { urlInput } = playerElements();
    try {
        const videoId = playerExtractVideoId(urlInput ? urlInput.value : '');
        playerLoad(videoId, `YouTube - ${videoId}`, null, 0);
    } catch (e) {
        playerSetMessage(e.message);
    }
}

// --- API Playlists ---

async function playerSaveUrl() {
    const { urlInput, newPlaylistInput, playlistSelect } = playerElements();
    const url            = urlInput         ? urlInput.value.trim()         : '';
    const newPlaylistName = newPlaylistInput ? newPlaylistInput.value.trim() : '';
    let   playlistId      = playlistSelect   ? playlistSelect.value          : '';

    try {
        playerExtractVideoId(url);
        playerSetMessage('');

        if (newPlaylistName) {
            const pl = await playerCreatePlaylist(newPlaylistName);
            playlistId = pl.id;
        } else if (!playlistId) {
            const pl = await playerCreatePlaylist('Minha playlist');
            playlistId = pl.id;
        }

        const item = await playerAddItem(playlistId, url);
        await playerLoadPlaylists(playlistId);
        await playerFetchPlaylist(playlistId, false);
        playerLoadTrack(item);

        if (urlInput)          urlInput.value = '';
        if (newPlaylistInput)  newPlaylistInput.value = '';
        playerSetMessage('Link salvo na playlist.', false);
    } catch (e) {
        playerSetMessage(e.message || 'Não foi possível salvar o link.');
    }
}

async function playerLoadPlaylists(selectedPlaylistId) {
    const { playlistSelect } = playerElements();
    if (!playlistSelect) return;

    const res = await fetch('/api/playlists');
    if (!res.ok) throw new Error('Falha ao carregar playlists.');

    const playlists = await res.json();
    playerState.playlists = playlists;
    playlistSelect.innerHTML = '<option value="">Selecione uma playlist</option>';
    playlists.forEach(pl => {
        const opt = document.createElement('option');
        opt.value = pl.id;
        opt.textContent = pl.nome;
        opt.selected = pl.id === selectedPlaylistId;
        playlistSelect.appendChild(opt);
    });
}

async function playerFetchPlaylist(playlistId, autoplay) {
    const res = await fetch(`/api/playlists/${playlistId}`);
    if (!res.ok) { playerSetMessage('Falha ao carregar playlist.'); return; }

    const payload = await res.json();
    playerState.currentPlaylistId = payload.playlist.id;
    playerState.playlistItems     = payload.items || [];
    playerState.currentIndex      = 0;
    playerRenderTracks();

    if (autoplay && playerState.playlistItems.length) {
        playerLoadTrack(playerState.playlistItems[0]);
    }
    playerSaveState();
}

async function playerCreatePlaylist(nome) {
    const res = await fetch('/api/playlists', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nome })
    });
    return playerReadJson(res, 'Não foi possível criar a playlist.');
}

async function playerAddItem(playlistId, url) {
    const res = await fetch(`/api/playlists/${playlistId}/items`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url })
    });
    return playerReadJson(res, 'Não foi possível salvar o link.');
}

async function playerReadJson(res, fallback) {
    const payload = await res.json();
    if (!res.ok) throw new Error(payload.erro || fallback);
    return payload;
}

// --- Render tracks ---

function playerRenderTracks() {
    const { trackList } = playerElements();
    if (!trackList) return;

    trackList.innerHTML = '';
    playerState.playlistItems.forEach((item, index) => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'player-track-button';
        if (index === playerState.currentIndex && playerState.playing) {
            btn.classList.add('active');
        }
        btn.textContent = playerCleanTitle(item.titulo, item.videoId);
        btn.addEventListener('click', () => playerLoadTrack(item));
        trackList.appendChild(btn);
    });
}

// --- Inicialização ---

async function initPlayerModule() {
    const { playlistSelect, container } = playerElements();
    if (!playlistSelect) return;

    // Restaurar estado do painel (retraído ou não)
    const saved = playerRestoreState();
    if (saved) {
        playerState.collapsed = !!saved.collapsed;
        if (container) container.classList.toggle('collapsed', playerState.collapsed);

        if (saved.currentPlaylistId) {
            await playerLoadPlaylists(saved.currentPlaylistId).catch(() =>
                playerSetMessage('Falha ao carregar playlists.')
            );
            if (saved.playlistItems && saved.playlistItems.length) {
                playerState.playlistItems     = saved.playlistItems;
                playerState.currentPlaylistId = saved.currentPlaylistId;
                playerState.currentIndex      = saved.currentIndex || 0;
            } else {
                await playerFetchPlaylist(saved.currentPlaylistId, false).catch(() => {});
                playerState.currentIndex = saved.currentIndex || 0;
            }
            if (saved.currentTitle) {
                const { currentTitle } = playerElements();
                if (currentTitle) currentTitle.textContent = saved.currentTitle;
            }
            playerRenderTracks();
        }
    } else {
        await playerLoadPlaylists().catch(() =>
            playerSetMessage('Falha ao carregar playlists.')
        );
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

window.addEventListener('beforeunload', playerSaveState);
window.addEventListener('DOMContentLoaded', initPlayerModule);

