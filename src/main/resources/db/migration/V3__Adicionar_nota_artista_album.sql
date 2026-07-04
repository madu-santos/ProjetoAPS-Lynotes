-- V3__Adicionar_nota_artista_album.sql
ALTER TABLE artista ADD COLUMN IF NOT EXISTS nota INT;
ALTER TABLE album ADD COLUMN IF NOT EXISTS nota INT;
