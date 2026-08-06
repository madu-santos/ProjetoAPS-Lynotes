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

window.playerTocar = function(url, titulo, artista, capaUrl, explicitYtId) {
    const displayTitle = (artista && titulo) ? `${artista} - ${titulo}` : (titulo || 'Música');
    
    // Expande o painel lateral do player se estiver recolhido
    if (playerState.collapsed) {
        playerTogglePanel();
    }

    // 1. Tentar ID explícito do YouTube (vindo do banco de dados)
    let videoId = null;
    if (explicitYtId && explicitYtId.trim() && !explicitYtId.startsWith('yt_') && (explicitYtId.length === 11 || explicitYtId.length > 11)) {
        videoId = explicitYtId.trim();
    }

    // 2. Tentar extrair ID da URL direta do YouTube (youtube.com/watch?v= ou youtu.be/ ou playlist?list=)
    if (!videoId && url && url.trim()) {
        const query = url.trim();
        if (query.includes('v=')) {
            let start = query.indexOf('v=') + 2;
            let end = query.indexOf('&', start);
            let extracted = end > 0 ? query.substring(start, end) : query.substring(start);
            if (extracted.length === 11 && !extracted.includes(' ') && !extracted.includes('/')) {
                videoId = extracted;
            }
        } else if (query.includes('youtu.be/')) {
            let start = query.indexOf('youtu.be/') + 9;
            let end = query.indexOf('?', start);
            let extracted = end > 0 ? query.substring(start, end) : query.substring(start);
            if (extracted.length === 11 && !extracted.includes(' ') && !extracted.includes('/')) {
                videoId = extracted;
            }
        }
    }

    // Se temos um ID de 11 caracteres válido do YouTube, executa imediatamente
    if (videoId) {
        playerLoad(videoId, displayTitle, null, 0);
        return;
    }

    // 3. Se a URL é uma busca do YouTube (youtube.com/results?search_query=...), extrair o termo
    let searchTerm = null;
    if (url && url.includes('search_query=')) {
        try {
            const urlObj = new URL(url);
            searchTerm = urlObj.searchParams.get('search_query');
        } catch(e) {
            // parse manual
            let start = url.indexOf('search_query=') + 13;
            let end = url.indexOf('&', start);
            let raw = end > 0 ? url.substring(start, end) : url.substring(start);
            searchTerm = decodeURIComponent(raw.replace(/\+/g, ' '));
        }
    }
    
    // Se não extraiu da URL, montar a partir do título e artista
    if (!searchTerm) {
        searchTerm = (artista && titulo) ? `${titulo} ${artista}` : (titulo || 'Música');
    }

    const { currentTitle } = playerElements();
    if (currentTitle) currentTitle.textContent = `Buscando: ${displayTitle}...`;

    // Agora o backend usa um scraper nativo infalível em vez do Piped API
    fetch('/api/youtube/search?q=' + encodeURIComponent(searchTerm) + '&tipo=MUSICA&realVideo=true')
        .then(r => r.json())
        .then(data => {
            if (data && data.length > 0 && data[0].youtubeId && data[0].youtubeId.length === 11 && !data[0].youtubeId.startsWith('yt_')) {
                playerLoad(data[0].youtubeId, displayTitle, null, 0);
            } else {
                if (currentTitle) currentTitle.textContent = 'Não foi possível encontrar o vídeo real no YouTube.';
            }
        })
        .catch(() => {
            if (currentTitle) currentTitle.textContent = 'Erro ao conectar com o servidor para buscar o vídeo.';
        });
};

window.playerTocarAlbum = async function(albumId, albumTitulo, albumCapa, startTrackId = null, shuffle = false) {
    if (playerState.collapsed) {
        playerTogglePanel();
    }
    
    const { currentTitle } = playerElements();
    if (currentTitle) currentTitle.textContent = `Carregando álbum: ${albumTitulo}...`;

    try {
        const res = await fetch(`/api/albums/${albumId}/faixas`);
        if (!res.ok) throw new Error('Falha ao buscar faixas do álbum');
        
        const faixas = await res.json();
        if (!faixas || faixas.length === 0) {
            if (currentTitle) currentTitle.textContent = 'Este álbum ainda não possui faixas importadas.';
            return;
        }

        // Converte as faixas no formato do player
        let items = faixas.map((f) => ({
            id: f.id,
            titulo: `${f.artista} - ${f.titulo}`,
            videoId: f.youtubeId || `yt_search_${f.artista}_${f.titulo}`,
            playlistId: albumId
        }));

        if (shuffle) {
            for (let i = items.length - 1; i > 0; i--) {
                const j = Math.floor(Math.random() * (i + 1));
                [items[i], items[j]] = [items[j], items[i]];
            }
        }

        playerState.playlistItems = items;
        playerState.currentPlaylistId = albumId;

        let startIdx = 0;
        if (startTrackId) {
            const foundIdx = items.findIndex(item => item.id === startTrackId);
            if (foundIdx !== -1) {
                startIdx = foundIdx;
            }
        }
        playerState.currentIndex = startIdx;

        playerRenderTracks();
        playerSaveState();
        
        // Toca a faixa selecionada/primeira
        playerLoadTrack(playerState.playlistItems[startIdx]);

    } catch (e) {
        if (currentTitle) currentTitle.textContent = 'Erro ao carregar faixas do álbum.';
        console.error(e);
    }
};

window.playerTocarAlbumEmOrdemAleatoria = function(albumId, albumTitulo, albumCapa) {
    return window.playerTocarAlbum(albumId, albumTitulo, albumCapa, null, true);
};

window.playerTocarAlbumApartirDe = function(albumId, albumTitulo, startTrackId, albumCapa) {
    return window.playerTocarAlbum(albumId, albumTitulo, albumCapa, startTrackId, false);
};

// --- Painel retrátil ---

function playerTogglePanel() {
    const { container } = playerElements();
    if (!container) return;
    playerState.collapsed = !playerState.collapsed;
    container.classList.toggle('collapsed', playerState.collapsed);

    document.querySelectorAll('.spotify-main').forEach(el => {
        el.classList.toggle('player-open', !playerState.collapsed);
    });

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
        
        // Anti-Rickroll: Limpa o estado se o cache estiver preso no Rickroll
        if (data.currentVideoId === 'dQw4w9WgXcQ' || 
            (data.playlistItems && data.playlistItems.some(i => i.videoId === 'dQw4w9WgXcQ'))) {
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
    if (!el || ytPlayer) return;

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

    // Se o ID for uma playlist do YouTube (como um álbum), carrega a playlist inteira
    if (videoId.length > 11 && (videoId.startsWith('PL') || videoId.startsWith('OL') || videoId.startsWith('RD'))) {
        ytPlayer.loadPlaylist({ list: videoId, index: 0, startSeconds: startSeconds || 0 });
    } else {
        ytPlayer.loadVideoById({ videoId, startSeconds: startSeconds || 0 });
    }
    
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
    
    // Se o videoId não for um ID válido do YouTube (ex: começa com itunes_track_), busca na API
    if (item.videoId && (item.videoId.startsWith('itunes_') || item.videoId.startsWith('yt_search_'))) {
        const { currentTitle } = playerElements();
        if (currentTitle) currentTitle.textContent = `Buscando faixa: ${item.titulo}...`;
        
        const searchTerm = encodeURIComponent(item.titulo);
        fetch('/api/youtube/search?q=' + searchTerm + '&tipo=MUSICA')
            .then(r => r.json())
            .then(data => {
                if (data && data.length > 0) {
                    const resolvedId = data[0].youtubeId;
                    item.videoId = resolvedId;
                    playerLoad(resolvedId, item.titulo, item.playlistId, 0);
                    
                    if (item.id) {
                        fetch(`/api/musicas/${item.id}/youtube-id`, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ youtubeId: resolvedId })
                        }).catch(e => console.error('Erro ao salvar youtubeId no banco:', e));
                    }
                } else {
                    if (currentTitle) currentTitle.textContent = 'Faixa não encontrada no YouTube.';
                    // Avança para a próxima faixa automaticamente após 3 segundos se falhar
                    setTimeout(playerNext, 3000);
                }
            })
            .catch(() => {
                if (currentTitle) currentTitle.textContent = 'Erro ao buscar faixa.';
                setTimeout(playerNext, 3000);
            });
    } else {
        playerLoad(item.videoId, item.titulo, item.playlistId, 0);
    }
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

async function playerFetchAlbum(albumId, autoplay) {
    const res = await fetch(`/api/albums/${albumId}/faixas`);
    if (!res.ok) { playerSetMessage('Falha ao carregar álbum.'); return; }

    const faixas = await res.json();
    playerState.currentPlaylistId = 'album_' + albumId;
    playerState.playlistItems     = faixas.map(f => ({
        id: f.id,
        videoId: f.youtubeId || `yt_search_${f.artista}_${f.titulo}`,
        url: f.youtubeUrl || '',
        titulo: f.titulo,
        playlistId: 'album_' + albumId
    }));
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

    if (window.playerModuleInitialized && ytPlayer) {
        return;
    }
    window.playerModuleInitialized = true;

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
    
    document.querySelectorAll('.spotify-main').forEach(el => {
        el.classList.toggle('player-open', !playerState.collapsed);
    });

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

