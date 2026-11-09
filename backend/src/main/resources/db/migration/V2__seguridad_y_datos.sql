-- Seguridad de la sesión y Ley 1581 de 2012 (protección de datos personales).

-- Cada token lleva la versión de sesión con la que se emitió. Cambiar la
-- contraseña sube la versión y deja sin valor todos los tokens anteriores.
ALTER TABLE usuarios ADD COLUMN version_sesion INTEGER NOT NULL DEFAULT 0;

-- Prueba de la autorización de datos: cuándo y bajo qué versión de la
-- política. Las cuentas creadas antes de esta migración quedan en NULL.
ALTER TABLE usuarios ADD COLUMN autorizo_datos_en TIMESTAMPTZ;
ALTER TABLE usuarios ADD COLUMN version_politica VARCHAR(10);
