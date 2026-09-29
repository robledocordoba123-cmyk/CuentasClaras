-- Esquema inicial de CuentasClaras (ver docs/01-diseno.md, sección 5).
-- Los montos son NUMERIC(14,2), nunca de punto flotante (RN-02).

CREATE TABLE usuarios (
    id             UUID         PRIMARY KEY,
    nombre         VARCHAR(100) NOT NULL,
    email          VARCHAR(160) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE cuentas (
    id             UUID          PRIMARY KEY,
    usuario_id     UUID          NOT NULL REFERENCES usuarios (id),
    nombre         VARCHAR(60)   NOT NULL,
    tipo           VARCHAR(20)   NOT NULL CHECK (tipo IN ('EFECTIVO', 'BILLETERA_DIGITAL', 'BANCO')),
    saldo_inicial  NUMERIC(14,2) NOT NULL DEFAULT 0,
    archivada      BOOLEAN       NOT NULL DEFAULT FALSE,
    creado_en      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_cuentas_usuario ON cuentas (usuario_id);

-- usuario_id NULL = categoría por defecto, visible para todas las personas.
CREATE TABLE categorias (
    id          UUID        PRIMARY KEY,
    usuario_id  UUID        REFERENCES usuarios (id),
    nombre      VARCHAR(60) NOT NULL,
    tipo        VARCHAR(10) NOT NULL CHECK (tipo IN ('INGRESO', 'GASTO')),
    color       VARCHAR(7)  NOT NULL DEFAULT '#6366F1',
    archivada   BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_categorias_usuario ON categorias (usuario_id);

CREATE TABLE movimientos (
    id                UUID          PRIMARY KEY,
    usuario_id        UUID          NOT NULL REFERENCES usuarios (id),
    cuenta_id         UUID          NOT NULL REFERENCES cuentas (id),
    categoria_id      UUID          REFERENCES categorias (id),
    tipo              VARCHAR(25)   NOT NULL CHECK (tipo IN ('INGRESO', 'GASTO', 'TRANSFERENCIA_SALIDA', 'TRANSFERENCIA_ENTRADA')),
    monto             NUMERIC(14,2) NOT NULL CHECK (monto > 0),                 -- RN-03
    fecha             DATE          NOT NULL,
    descripcion       VARCHAR(200),
    transferencia_id  UUID,
    creado_en         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- Ingresos y gastos siempre llevan categoría; las transferencias nunca.
    CONSTRAINT chk_categoria_segun_tipo CHECK (
        (tipo IN ('INGRESO', 'GASTO') AND categoria_id IS NOT NULL AND transferencia_id IS NULL)
        OR
        (tipo IN ('TRANSFERENCIA_SALIDA', 'TRANSFERENCIA_ENTRADA') AND categoria_id IS NULL AND transferencia_id IS NOT NULL)
    )
);
CREATE INDEX idx_movimientos_usuario_fecha ON movimientos (usuario_id, fecha);
CREATE INDEX idx_movimientos_cuenta ON movimientos (cuenta_id);

CREATE TABLE presupuestos (
    id            UUID          PRIMARY KEY,
    usuario_id    UUID          NOT NULL REFERENCES usuarios (id),
    categoria_id  UUID          NOT NULL REFERENCES categorias (id),
    mes           CHAR(7)       NOT NULL CHECK (mes ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    monto_limite  NUMERIC(14,2) NOT NULL CHECK (monto_limite > 0),
    CONSTRAINT uq_presupuesto_categoria_mes UNIQUE (usuario_id, categoria_id, mes)  -- RN-08
);

-- Categorías por defecto, pensadas para los gastos de una persona en Colombia.
INSERT INTO categorias (id, usuario_id, nombre, tipo, color) VALUES
    ('00000000-0000-0000-0000-000000000101', NULL, 'Salario',            'INGRESO', '#16A34A'),
    ('00000000-0000-0000-0000-000000000102', NULL, 'Trabajos extra',     'INGRESO', '#22C55E'),
    ('00000000-0000-0000-0000-000000000103', NULL, 'Otros ingresos',     'INGRESO', '#4ADE80'),
    ('00000000-0000-0000-0000-000000000201', NULL, 'Mercado',            'GASTO',   '#F97316'),
    ('00000000-0000-0000-0000-000000000202', NULL, 'Transporte',         'GASTO',   '#0EA5E9'),
    ('00000000-0000-0000-0000-000000000203', NULL, 'Arriendo',           'GASTO',   '#8B5CF6'),
    ('00000000-0000-0000-0000-000000000204', NULL, 'Servicios públicos', 'GASTO',   '#14B8A6'),
    ('00000000-0000-0000-0000-000000000205', NULL, 'Domicilios',         'GASTO',   '#EF4444'),
    ('00000000-0000-0000-0000-000000000206', NULL, 'Salud',              'GASTO',   '#EC4899'),
    ('00000000-0000-0000-0000-000000000207', NULL, 'Educación',          'GASTO',   '#6366F1'),
    ('00000000-0000-0000-0000-000000000208', NULL, 'Entretenimiento',    'GASTO',   '#EAB308'),
    ('00000000-0000-0000-0000-000000000209', NULL, 'Otros gastos',       'GASTO',   '#64748B');
