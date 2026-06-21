const playerState = {
    playlistItems: [],
    currentIndex: 0,
    playing: false,
    currentPlaylistId: null
};

function playerLoad(videoId, title, playlistId) {
    const iframe = document.getElementById('youtube-player');
    const currentTitle = document.getElementById('player-current-title');
    if (!iframe || !currentTitle) {
        return;
    }

    iframe.src = `https://www.youtube.com/embed/${videoId}?enablejsapi=1&rel=0&modestbranding=1&autoplay=1`;
    playerState.playing = true;
    playerState.currentPlaylistId = playlistId || playerState.currentPlaylistId;
    currentTitle.textContent = title || `YouTube - ${videoId}`;
}

function playerLoadFromSelectedPlaylist() {
    if (!playerState.playlistItems.length) {
        return;
    }
    playerState.currentIndex = 0;
    const item = playerState.playlistItems[0];
    playerLoad(item.videoId, item.titulo, item.playlistId);
}

function playerSelectPlaylist() {
    const select = document.getElementById('player-playlist-select');
    if (!select) {
        return;
    }
    const playlistId = select.value;
    if (!playlistId) {
        return;
    }
    fetch(`/api/playlists/${playlistId}`)
        .then(response => response.json())
        .then(payload => {
            playerState.playlistItems = payload.items || [];
            playerState.currentIndex = 0;
            if (playerState.playlistItems.length) {
                const item = playerState.playlistItems[0];
                playerLoad(item.videoId, item.titulo, payload.playlist.id);
            }
        })
        .catch(() => {
            console.error('Falha ao carregar playlist');
        });
}

function playerPrev() {
    if (!playerState.playlistItems.length) {
        return;
    }
    playerState.currentIndex = Math.max(0, playerState.currentIndex - 1);
    const item = playerState.playlistItems[playerState.currentIndex];
    playerLoadTrack(item);
}

function playerNext() {
    if (!playerState.playlistItems.length) {
        return;
    }
    if (playerState.currentIndex < playerState.playlistItems.length - 1) {
        playerState.currentIndex += 1;
    }
    const item = playerState.playlistItems[playerState.currentIndex];
    playerLoadTrack(item);
}

function playerPlayPause() {
    const iframe = document.getElementById('youtube-player');
    if (!iframe) {
        return;
    }
    if (iframe.src.includes('autoplay=1')) {
        iframe.src = iframe.src.replace('&autoplay=1', '');
        playerState.playing = false;
    } else {
        iframe.src = iframe.src.includes('?') ? iframe.src + '&autoplay=1' : iframe.src + '?autoplay=1';
        playerState.playing = true;
    }
}

function playerLoadTrack(item) {
    playerState.currentIndex = playerState.playlistItems.findIndex(i => i.id === item.id);
    if (playerState.currentIndex === -1) {
        playerState.currentIndex = 0;
    }
    playerLoad(item.videoId, item.titulo, item.playlistId);
}

function initPlayerModule() {
    const select = document.getElementById('player-playlist-select');
    if (!select) {
        return;
    }
    fetch('/api/playlists')
        .then(response => response.json())
        .then(playlists => {
            select.innerHTML = '<option value="">Selecione uma playlist</option>';
            playlists.forEach(playlist => {
                const option = document.createElement('option');
                option.value = playlist.id;
                option.textContent = playlist.nome;
                select.appendChild(option);
            });
        })
        .catch(() => {
            console.error('Falha ao carregar playlists');
        });
}

window.addEventListener('DOMContentLoaded', initPlayerModule);
