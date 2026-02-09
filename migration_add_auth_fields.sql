-- Script de migración para agregar autenticación a la tabla usuario
-- Fecha: 2026-02-07
-- Descripción: Agrega las columnas password y role para el sistema de autenticación JWT

-- Agregar columna password (puede ser NULL para usuarios existentes sin autenticación)
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS password VARCHAR(255);

-- Agregar columna role con valor por defecto 'USER'
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Crear índice para búsquedas por rol
CREATE INDEX IF NOT EXISTS idx_user_role ON usuario(role);

-- Comentarios para documentación
COMMENT ON COLUMN usuario.password IS 'Contraseña encriptada del usuario (BCrypt)';
COMMENT ON COLUMN usuario.role IS 'Rol del usuario: ADMIN o USER';

-- Verificación: Mostrar la estructura de la tabla
-- SELECT column_name, data_type, is_nullable, column_default
-- FROM information_schema.columns
-- WHERE table_name = 'usuario';

