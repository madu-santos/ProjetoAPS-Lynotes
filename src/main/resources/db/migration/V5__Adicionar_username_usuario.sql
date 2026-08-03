-- V5__Adicionar_username_usuario.sql
ALTER TABLE usuario ADD COLUMN username VARCHAR(100);
UPDATE usuario SET username = split_part(email, '@', 1);
UPDATE usuario SET username = id WHERE username IS NULL OR username = '';
ALTER TABLE usuario ALTER COLUMN username SET NOT NULL;
ALTER TABLE usuario ADD CONSTRAINT usuario_username_key UNIQUE (username);
