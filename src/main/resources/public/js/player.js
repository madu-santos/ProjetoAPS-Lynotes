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
    collapsed: false,
    shuffle: false,
    loop: false
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

window.playerTocar = function(url, titulo, artista, capaUrl, explicitYtId, keepLoop = false) {
    if (!keepLoop && playerState.loop) {
        playerToggleLoop();
    }
    const displayTitle = (artista && titulo) ? `${artista} - ${titulo}` : (titulo || 'Música');
    
    // Reseta fila para apenas uma música
    playerState.playlistItems = [];
    playerState.currentPlaylistId = null;
    playerState.currentIndex = 0;

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
        playerState.playlistItems = [{
            id: 'single_' + videoId,
            titulo: displayTitle,
            artista: artista || '',
            capa: capaUrl || '',
            videoId: videoId,
            playlistId: null
        }];
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
        searchTerm = (artista && titulo) ? `${artista} ${titulo}` : (titulo || 'Música');
    }

    const { currentTitle } = playerElements();
    if (currentTitle) currentTitle.textContent = `Buscando: ${displayTitle}...`;

    // Agora o backend usa um scraper nativo infalível em vez do Piped API
    fetch('/api/youtube/search?q=' + encodeURIComponent(searchTerm) + '&tipo=MUSICA&realVideo=true')
        .then(r => r.json())
        .then(data => {
            if (data && data.length > 0 && data[0].youtubeId && data[0].youtubeId.length === 11 && !data[0].youtubeId.startsWith('yt_')) {
                const resolvedId = data[0].youtubeId;
                playerState.playlistItems = [{
                    id: 'single_' + resolvedId,
                    titulo: displayTitle,
                    artista: artista || '',
                    capa: capaUrl || '',
                    videoId: resolvedId,
                    playlistId: null
                }];
                playerLoad(resolvedId, displayTitle, null, 0);
            } else {
                if (currentTitle) currentTitle.textContent = 'Não foi possível encontrar o vídeo real no YouTube.';
            }
        })
        .catch(() => {
            if (currentTitle) currentTitle.textContent = 'Erro ao conectar com o servidor para buscar o vídeo.';
        });
};

window.playerTocarAlbum = async function(albumId, albumTitulo, albumCapa, startTrackId = null, shuffle = false, keepLoop = false) {
    if (!keepLoop && playerState.loop) {
        playerToggleLoop();
    }
    if (playerState.collapsed) {
        playerTogglePanel();
    }
    
    const { currentTitle, playlistSelect } = playerElements();
    if (currentTitle) currentTitle.textContent = `Carregando álbum: ${albumTitulo}...`;

    try {
        const res = await fetch(`/api/albums/${albumId}/faixas`);
        if (!res.ok) throw new Error('Falha ao buscar faixas do álbum');
        
        const faixas = await res.json();
        if (!faixas || faixas.length === 0) {
            if (currentTitle) currentTitle.textContent = 'Este álbum ainda não possui faixas importadas.';
            return;
        }

        const faixasAtivas = faixas.filter(f => !f.ocultaDaBiblioteca && !f.oculta_da_biblioteca);
        if (faixasAtivas.length === 0) {
            if (currentTitle) currentTitle.textContent = 'Todas as faixas deste álbum estão ocultas/desativadas.';
            return;
        }

        // Converte apenas as faixas ativas no formato do player com título completo e capa
        let items = faixasAtivas.map((f) => {
            const displayTitle = (f.artista && f.titulo && !f.titulo.includes(f.artista)) ? `${f.artista} - ${f.titulo}` : (f.titulo || 'Faixa');
            return {
                id: f.id,
                titulo: displayTitle,
                artista: f.artista,
                videoId: f.youtubeId || `yt_search_${f.artista}_${f.titulo}`,
                capa: f.capaUrl || albumCapa || '',
                playlistId: 'album_' + albumId
            };
        });

        if (shuffle) {
            for (let i = items.length - 1; i > 0; i--) {
                const j = Math.floor(Math.random() * (i + 1));
                [items[i], items[j]] = [items[j], items[i]];
            }
        }

        playerState.playlistItems = items;
        playerState.currentPlaylistId = 'album_' + albumId;

        // Atualizar o select de playlist no mini-player com o nome do álbum
        if (playlistSelect) {
            let opt = playlistSelect.querySelector(`option[value="album_${albumId}"]`);
            if (!opt) {
                opt = document.createElement('option');
                opt.value = 'album_' + albumId;
                playlistSelect.appendChild(opt);
            }
            opt.textContent = `Álbum: ${albumTitulo}`;
            opt.selected = true;
        }

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

window.playerTocarAlbumEmLoop = function(albumId, albumTitulo, albumCapa) {
    if (!playerState.loop) {
        playerToggleLoop();
    }
    return window.playerTocarAlbum(albumId, albumTitulo, albumCapa, null, false, true);
};

window.playerTocarAlbumApartirDe = function(albumId, albumTitulo, startTrackId, albumCapa) {
    return window.playerTocarAlbum(albumId, albumTitulo, albumCapa, startTrackId, false);
};

// --- Painel retrátil ---

function playerTogglePanel() {
    const { container } = playerElements();
    if (!container) return;

    if (container.classList.contains('player-floating')) {
        window.playerResetPosition();
    }

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
            shuffle:          playerState.shuffle,
            loop:             playerState.loop,
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

function playerUpdateToggleButtonsUI() {
    const loopBtn = document.getElementById('player-loop-btn');
    if (loopBtn) {
        if (playerState.loop) {
            loopBtn.classList.remove('btn-outline-secondary');
            loopBtn.classList.add('btn-purple', 'text-white', 'shadow');
        } else {
            loopBtn.classList.remove('btn-purple', 'text-white', 'shadow');
            loopBtn.classList.add('btn-outline-secondary');
        }
    }
    const shuffleBtn = document.getElementById('player-shuffle-btn');
    if (shuffleBtn) {
        if (playerState.shuffle) {
            shuffleBtn.classList.remove('btn-outline-secondary');
            shuffleBtn.classList.add('btn-purple', 'text-white', 'shadow');
        } else {
            shuffleBtn.classList.remove('btn-purple', 'text-white', 'shadow');
            shuffleBtn.classList.add('btn-outline-secondary');
        }
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

    if (saved) {
        if (saved.loop !== undefined) playerState.loop = !!saved.loop;
        if (saved.shuffle !== undefined) playerState.shuffle = !!saved.shuffle;
        playerUpdateToggleButtonsUI();
    }

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
        playerUpdateVinylCover();

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
        playerUpdateVinylCover();

    } else if (event.data === YT.PlayerState.PAUSED) {
        playerState.playing = false;
        if (vinyl) vinyl.classList.remove('spinning');
        if (playBtn) playBtn.innerHTML = SVG_PLAY;
        playerSaveState();

    } else if (event.data === YT.PlayerState.ENDED) {
        playerState.playing = false;
        if (vinyl) vinyl.classList.remove('spinning');
        if (playBtn) playBtn.innerHTML = SVG_PLAY;

        // Se Loop (Repetir) estiver ativo
        if (playerState.loop) {
            if (!playerState.playlistItems || playerState.playlistItems.length <= 1) {
                if (playerState.currentVideoId) {
                    ytPlayer.seekTo(0);
                    ytPlayer.playVideo();
                    return;
                }
            } else {
                let nextIdx = (playerState.currentIndex + 1) % playerState.playlistItems.length;
                playerState.currentIndex = nextIdx;
                playerLoadTrack(playerState.playlistItems[nextIdx]);
                return;
            }
        }

        // Se Shuffle estiver ativo
        if (playerState.shuffle && playerState.playlistItems && playerState.playlistItems.length > 1) {
            let nextIdx = Math.floor(Math.random() * playerState.playlistItems.length);
            if (nextIdx === playerState.currentIndex) {
                nextIdx = (playerState.currentIndex + 1) % playerState.playlistItems.length;
            }
            playerState.currentIndex = nextIdx;
            playerLoadTrack(playerState.playlistItems[nextIdx]);
            return;
        }

        // Avançar sequencialmente (sem loop)
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

function playerToggleShuffle() {
    playerState.shuffle = !playerState.shuffle;
    const btn = document.getElementById('player-shuffle-btn');
    if (btn) {
        if (playerState.shuffle) {
            btn.classList.remove('btn-outline-secondary');
            btn.classList.add('btn-purple', 'text-white', 'shadow');
        } else {
            btn.classList.remove('btn-purple', 'text-white', 'shadow');
            btn.classList.add('btn-outline-secondary');
        }
    }
    playerSaveState();
}

function playerToggleLoop() {
    playerState.loop = !playerState.loop;
    const btn = document.getElementById('player-loop-btn');
    if (btn) {
        if (playerState.loop) {
            btn.classList.remove('btn-outline-secondary');
            btn.classList.add('btn-purple', 'text-white', 'shadow');
        } else {
            btn.classList.remove('btn-purple', 'text-white', 'shadow');
            btn.classList.add('btn-outline-secondary');
        }
    }
    playerSaveState();
}

  window.checarESubmeterPlaylist = async function(formElement, playlistId, videoId, url) {
    const doSubmit = function() {
        if (window.htmx) {
            htmx.ajax('POST', formElement.action, {
                values: new FormData(formElement),
                target: '#app-content',
                select: '#app-content',
                pushUrl: true
            });
        } else if (typeof formElement.requestSubmit === 'function') {
            formElement.requestSubmit();
        } else {
            formElement.submit();
        }
    };

    if (!playlistId || playlistId === '' || playlistId === 'NEW_TEMP') {
        doSubmit();
        return;
    }

    try {
        const queryParams = new URLSearchParams({ videoId: videoId || '', url: url || '' });
        const res = await fetch(`/api/playlists/${playlistId}/check-duplicate?` + queryParams);
        if (res.ok) {
            const data = await res.json();
            if (data.duplicada) {
                const modalEl = document.getElementById('modalConfirmarDuplicado');
                if (modalEl) {
                    if (modalEl.parentNode !== document.body) {
                        document.body.appendChild(modalEl);
                    }
                    const bsModal = bootstrap.Modal.getOrCreateInstance(modalEl);
                    const btnSim = document.getElementById('btnConfirmarDuplicado');

                    const handleSim = function() {
                        btnSim.removeEventListener('click', handleSim);
                        bsModal.hide();
                        doSubmit();
                    };

                    btnSim.addEventListener('click', handleSim);
                    bsModal.show();
                    return false;
                } else if (confirm("Essa música já existe na playlist, deseja continuar?")) {
                    doSubmit();
                    return true;
                } else {
                    return false;
                }
            }
        }
    } catch (e) {
        console.error('Erro ao checar duplicata:', e);
    }
    doSubmit();
};

function playerUpdateVinylCover() {
    const vinylLabel = document.querySelector('.player-vinyl-label');
    if (!vinylLabel) return;
    const currentItem = (playerState.playlistItems && playerState.playlistItems[playerState.currentIndex]);
    if (currentItem && currentItem.capa) {
        vinylLabel.style.backgroundImage = `url('${currentItem.capa}')`;
        vinylLabel.style.backgroundSize = 'cover';
        vinylLabel.style.backgroundPosition = 'center';
    } else {
        vinylLabel.style.backgroundImage = '';
    }
}

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
    playerState.currentPlaylistId = playlistId || null;
    playerState.playing           = true;
    playerState.currentTime       = startSeconds || 0;

    const { currentTitle } = playerElements();
    if (currentTitle) currentTitle.textContent = playerState.currentTitle;

    playerUpdateVinylCover();

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
    if (playerState.currentIndex > 0) {
        playerState.currentIndex -= 1;
    } else if (playerState.loop) {
        playerState.currentIndex = playerState.playlistItems.length - 1;
    }
    playerLoadTrack(playerState.playlistItems[playerState.currentIndex]);
}

function playerNext() {
    if (!playerState.playlistItems.length) return;
    if (playerState.currentIndex < playerState.playlistItems.length - 1) {
        playerState.currentIndex += 1;
    } else if (playerState.loop) {
        playerState.currentIndex = 0;
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
    
    let vId = item.videoId;
    if ((!vId || vId.length !== 11 || vId.startsWith('itunes_') || vId.startsWith('yt_') || vId.startsWith('track_')) && item.url) {
        try {
            const extracted = playerExtractVideoId(item.url);
            if (extracted && extracted.length === 11) {
                vId = extracted;
                item.videoId = extracted;
            }
        } catch (e) {}
    }

    if (!vId || vId.length !== 11 || vId.startsWith('itunes_') || vId.startsWith('yt_') || vId.startsWith('track_')) {
        const { currentTitle } = playerElements();
        let queryTitle = (item.artista && item.titulo) ? `${item.artista} ${item.titulo}` : (item.titulo || '');
        if (queryTitle.startsWith('YouTube - ')) {
            queryTitle = queryTitle.replace('YouTube - ', '');
        }
        if (currentTitle) currentTitle.textContent = `Buscando faixa: ${queryTitle}...`;
        
        const searchTerm = encodeURIComponent(queryTitle);
        fetch('/api/youtube/search?q=' + searchTerm + '&tipo=MUSICA&realVideo=true')
            .then(r => r.json())
            .then(data => {
                if (data && data.length > 0 && data[0].youtubeId && data[0].youtubeId.length === 11 && !data[0].youtubeId.startsWith('yt_')) {
                    const resolvedId = data[0].youtubeId;
                    item.videoId = resolvedId;
                    const trackDisplayTitle = (item.artista && item.titulo && !item.titulo.includes(item.artista)) ? `${item.artista} - ${item.titulo}` : item.titulo;
                    playerLoad(resolvedId, trackDisplayTitle, item.playlistId, 0);
                    
                    if (item.id) {
                        fetch(`/api/musicas/${item.id}/youtube-id`, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ youtubeId: resolvedId })
                        }).catch(e => console.error('Erro ao salvar youtubeId no banco:', e));
                    }
                } else {
                    if (currentTitle) currentTitle.textContent = 'Faixa não encontrada no YouTube.';
                    setTimeout(playerNext, 3000);
                }
            })
            .catch(() => {
                if (currentTitle) currentTitle.textContent = 'Erro ao buscar faixa.';
                setTimeout(playerNext, 3000);
            });
    } else {
        const trackDisplayTitle = (item.artista && item.titulo && !item.titulo.includes(item.artista)) ? `${item.artista} - ${item.titulo}` : item.titulo;
        playerLoad(vId, trackDisplayTitle, item.playlistId, 0);
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

async function playerFetchPlaylist(playlistId, autoplay, keepLoop = false) {
    if (!keepLoop && playerState.loop) {
        playerToggleLoop();
    }
    if (playerState.collapsed) {
        playerTogglePanel();
    }
    const res = await fetch(`/api/playlists/${playlistId}`);
    if (!res.ok) { playerSetMessage('Falha ao carregar playlist.'); return; }

    const payload = await res.json();
    if (!payload || !payload.playlist) {
        playerSetMessage('Playlist não encontrada.');
        return;
    }
    playerState.currentPlaylistId = payload.playlist.id;
    const { playlistSelect } = playerElements();
    if (playlistSelect) {
        playlistSelect.value = playlistId;
    }
    const items = payload.items || [];
    const itemsAtivos = items.filter(item => !item.oculta && !item.ocultaDaBiblioteca);
    playerState.playlistItems = itemsAtivos.map(item => {
        let vId = item.videoId;
        if ((!vId || vId.length !== 11 || vId.startsWith('yt_') || vId.startsWith('track_')) && item.url) {
            try {
                vId = playerExtractVideoId(item.url);
            } catch (e) {
                // Not a direct YT URL
            }
        }
        return {
            id: item.id,
            videoId: vId || item.videoId || `yt_search_${item.artista ? item.artista + '_' : ''}${item.titulo}`,
            url: item.url || '',
            titulo: item.titulo,
            artista: item.artista,
            capa: item.capaUrl || '',
            playlistId: playlistId
        };
    });

    if (playerState.shuffle && playerState.playlistItems.length > 1) {
        for (let i = playerState.playlistItems.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [playerState.playlistItems[i], playerState.playlistItems[j]] = [playerState.playlistItems[j], playerState.playlistItems[i]];
        }
    }

    playerState.currentIndex      = 0;
    playerRenderTracks();

    if (autoplay && playerState.playlistItems.length) {
        playerLoadTrack(playerState.playlistItems[0]);
    }
    playerSaveState();
}

window.playerTocarPlaylistEmOrdemAleatoria = function(playlistId) {
    if (!playerState.shuffle) {
        playerToggleShuffle();
    }
    return playerFetchPlaylist(playlistId, true);
};

window.playerTocarPlaylistEmLoop = function(playlistId) {
    if (!playerState.loop) {
        playerToggleLoop();
    }
    return playerFetchPlaylist(playlistId, true, true);
};

async function playerFetchAlbum(albumId, autoplay) {
    const res = await fetch(`/api/albums/${albumId}/faixas`);
    if (!res.ok) { playerSetMessage('Falha ao carregar álbum.'); return; }

    const faixas = await res.json();
    const faixasAtivas = (faixas || []).filter(f => !f.ocultaDaBiblioteca && !f.oculta_da_biblioteca);
    playerState.currentPlaylistId = 'album_' + albumId;
    playerState.playlistItems     = faixasAtivas.map(f => ({
        id: f.id,
        videoId: f.youtubeId || `yt_search_${f.artista}_${f.titulo}`,
        url: f.youtubeUrl || '',
        titulo: f.titulo,
        artista: f.artista,
        capa: f.capaUrl || '',
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

    const toggleBtn = document.getElementById('player-toggle-tracklist-btn');
    const isAlbumOrPlaylist = playerState.currentPlaylistId !== null;

    if (toggleBtn) {
        if (isAlbumOrPlaylist && playerState.playlistItems && playerState.playlistItems.length > 0) {
            toggleBtn.classList.remove('d-none');
        } else {
            toggleBtn.classList.add('d-none');
            // Força a esconder a lista
            if (window.playerToggleTrackList) {
                window.playerToggleTrackList(true);
            }
        }
    }

    trackList.innerHTML = '';
    if (isAlbumOrPlaylist) {
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
    // Força o painel a iniciar fechado sempre, independente do estado salvo
    playerState.collapsed = true;
    if (container) container.classList.add('collapsed');

    const saved = playerRestoreState();
    if (saved) {
        if (saved.loop !== undefined) playerState.loop = !!saved.loop;
        if (saved.shuffle !== undefined) playerState.shuffle = !!saved.shuffle;
        playerUpdateToggleButtonsUI();

        if (saved.currentPlaylistId) {
            await playerLoadPlaylists(saved.currentPlaylistId).catch(() =>
                playerSetMessage('Falha ao carregar playlists.')
            );
            if (saved.playlistItems && saved.playlistItems.length) {
                playerState.playlistItems     = saved.playlistItems;
                playerState.currentPlaylistId = saved.currentPlaylistId;
                playerState.currentIndex      = saved.currentIndex || 0;
            } else {
                await playerFetchPlaylist(saved.currentPlaylistId, false, true).catch(() => {});
                playerState.currentIndex = saved.currentIndex || 0;
            }
            if (saved.currentTitle) {
                const { currentTitle } = playerElements();
                if (currentTitle) currentTitle.textContent = saved.currentTitle;
            }
            playerRenderTracks();
            playerUpdateVinylCover();
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

window.playerToggleTrackList = function(forceHide) {
    const trackList = document.getElementById('player-track-list');
    const toggleBtn = document.getElementById('player-toggle-tracklist-btn');
    if (!trackList || !toggleBtn) return;

    if (forceHide === true || !trackList.classList.contains('hidden')) {
        trackList.classList.add('hidden');
        toggleBtn.innerHTML = 'Faixas &#9660;';
    } else {
        trackList.classList.remove('hidden');
        toggleBtn.innerHTML = 'Faixas &#9650;';
    }
};

window.playerResetPosition = function() {
    const player = document.getElementById('mini-player');
    const panel = document.querySelector('.player-panel');
    const resetBtn = document.getElementById('player-reset-pos-btn');
    if (!player || !panel) return;
    
    // Desativa transições temporariamente para que o retorno à lateral seja instantâneo (teleporte)
    player.style.transition = 'none';
    
    player.classList.remove('player-floating');
    player.style.top = '';
    player.style.left = '';
    player.style.right = '';
    
    panel.style.setProperty('--player-scale', 1);
    
    if (resetBtn) resetBtn.classList.add('d-none');
    
    // Força reflow para renderizar a remoção de classes e estilos instantaneamente
    player.offsetHeight;
    
    // Reativa a transição suave logo em seguida
    setTimeout(() => {
        player.style.transition = '';
    }, 50);
};

// --- UX Improvements (Drag and Drop & Resize) ---
// --- UX Improvements (Drag and Drop & Resize) ---
function initPlayerUX() {
    const player = document.getElementById('mini-player');
    const panel = document.querySelector('.player-panel');
    const dragHandle = document.getElementById('player-drag-handle');
    if (!player || !panel || !dragHandle) return;

    let isDragging = false;
    let dragStartX = 0;
    let dragStartY = 0;
    let initialLeft = 0;
    let initialTop = 0;

    const onMouseMove = (e) => {
        if (!isDragging) return;
        const dx = e.clientX - dragStartX;
        const dy = e.clientY - dragStartY;
        player.style.left = (initialLeft + dx) + 'px';
        player.style.top = (initialTop + dy) + 'px';
    };

    const onMouseUp = () => {
        if (isDragging) {
            isDragging = false;
            document.removeEventListener('mousemove', onMouseMove);
            document.removeEventListener('mouseup', onMouseUp);
        }
    };

    dragHandle.addEventListener('mousedown', (e) => {
        isDragging = true;
        
        // Detach player from sidebar if not already floating
        if (!player.classList.contains('player-floating')) {
            const rect = player.getBoundingClientRect();
            player.style.right = 'auto';
            player.style.left = rect.left + 'px';
            player.style.top = rect.top + 'px';
            player.classList.add('player-floating');
            const resetBtn = document.getElementById('player-reset-pos-btn');
            if (resetBtn) resetBtn.classList.remove('d-none');
        }
        
        if (window.playerToggleTrackList) {
            window.playerToggleTrackList(true);
        }
        
        dragStartX = e.clientX;
        dragStartY = e.clientY;
        initialLeft = player.getBoundingClientRect().left;
        initialTop = player.getBoundingClientRect().top;
        
        document.addEventListener('mousemove', onMouseMove);
        document.addEventListener('mouseup', onMouseUp);
        
        e.preventDefault();
    });

    // Resize Logic (Edge Handles)
    const resizeEdges = document.querySelectorAll('[data-resize]');
    let isResizing = false;
    let initialDistance = 0;
    let initialScale = 1;
    let centerX = 0;
    let centerY = 0;
    let originalHeight = 0;

    const onResizeMouseMove = (e) => {
        if (!isResizing) return;
        const newDistance = Math.hypot(e.clientX - centerX, e.clientY - centerY);
        let newScale = initialScale * (newDistance / initialDistance);
        newScale = Math.max(0.4, Math.min(newScale, 1.0));
        
        panel.style.setProperty('--player-scale', newScale);
        
        // Adjust position to keep center fixed for a smooth resizing experience from any edge
        const newWidth = 280 * newScale;
        const currentScaledHeight = originalHeight * newScale;
        player.style.left = (centerX - (newWidth / 2)) + 'px';
        player.style.top = (centerY - (currentScaledHeight / 2)) + 'px';
    };

    const onResizeMouseUp = () => {
        if (isResizing) {
            isResizing = false;
            document.removeEventListener('mousemove', onResizeMouseMove);
            document.removeEventListener('mouseup', onResizeMouseUp);
        }
    };

    resizeEdges.forEach(edge => {
        edge.addEventListener('mousedown', (e) => {
            isResizing = true;
            initialScale = parseFloat(panel.style.getPropertyValue('--player-scale') || 1);
            
            const rect = player.getBoundingClientRect();
            // Since zoom changes rect, we can calculate original dimensions
            originalHeight = rect.height / initialScale; 
            centerX = rect.left + (rect.width / 2);
            centerY = rect.top + (rect.height / 2);
            
            initialDistance = Math.hypot(e.clientX - centerX, e.clientY - centerY);
            
            document.addEventListener('mousemove', onResizeMouseMove);
            document.addEventListener('mouseup', onResizeMouseUp);
            
            e.preventDefault();
            e.stopPropagation();
        });
    });
    
    // Mostra o botão reset se o player estiver flutuando ao recarregar a página
    if (player.classList.contains('player-floating')) {
        const resetBtn = document.getElementById('player-reset-pos-btn');
        if (resetBtn) resetBtn.classList.remove('d-none');
    }
}

window.addEventListener('beforeunload', playerSaveState);
window.addEventListener('DOMContentLoaded', initPlayerModule);
window.addEventListener('DOMContentLoaded', initPlayerUX);

