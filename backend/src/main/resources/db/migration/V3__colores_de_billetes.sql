-- Nueva identidad: las categorías por defecto toman los colores de los billetes
-- colombianos (verde 100 mil, naranja 20 mil, turquesa 2 mil, violeta 50 mil,
-- ocre 5 mil, carmín 10 mil) y tonos vecinos. Las categorías propias no cambian.
UPDATE categorias SET color = '#2D7D55' WHERE id = '00000000-0000-0000-0000-000000000101'; -- Salario
UPDATE categorias SET color = '#3F9B6C' WHERE id = '00000000-0000-0000-0000-000000000102'; -- Trabajos extra
UPDATE categorias SET color = '#6BB48C' WHERE id = '00000000-0000-0000-0000-000000000103'; -- Otros ingresos
UPDATE categorias SET color = '#D9622B' WHERE id = '00000000-0000-0000-0000-000000000201'; -- Mercado
UPDATE categorias SET color = '#138A9C' WHERE id = '00000000-0000-0000-0000-000000000202'; -- Transporte
UPDATE categorias SET color = '#6A4FC2' WHERE id = '00000000-0000-0000-0000-000000000203'; -- Arriendo
UPDATE categorias SET color = '#B7791F' WHERE id = '00000000-0000-0000-0000-000000000204'; -- Servicios públicos
UPDATE categorias SET color = '#C23B5A' WHERE id = '00000000-0000-0000-0000-000000000205'; -- Domicilios
UPDATE categorias SET color = '#A8447F' WHERE id = '00000000-0000-0000-0000-000000000206'; -- Salud
UPDATE categorias SET color = '#3C6FB0' WHERE id = '00000000-0000-0000-0000-000000000207'; -- Educación
UPDATE categorias SET color = '#C99A1B' WHERE id = '00000000-0000-0000-0000-000000000208'; -- Entretenimiento
UPDATE categorias SET color = '#6B7280' WHERE id = '00000000-0000-0000-0000-000000000209'; -- Otros gastos
