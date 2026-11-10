-- Cuentas con personas, pagos fijos del mes y metas de ahorro.

-- Personas con las que hay plata de por medio: no son usuarias de la app,
-- solo un nombre para agrupar lo que se debe.
CREATE TABLE personas (
    id          UUID         PRIMARY KEY,
    usuario_id  UUID         NOT NULL REFERENCES usuarios (id),
    nombre      VARCHAR(60)  NOT NULL,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_personas_nombre ON personas (usuario_id, lower(nombre));

-- ME_DEBEN: presté plata o pagué por alguien. DEBO: alguien me prestó.
CREATE TABLE deudas (
    id           UUID          PRIMARY KEY,
    usuario_id   UUID          NOT NULL REFERENCES usuarios (id),
    persona_id   UUID          NOT NULL REFERENCES personas (id),
    direccion    VARCHAR(10)   NOT NULL CHECK (direccion IN ('ME_DEBEN', 'DEBO')),
    monto        NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    descripcion  VARCHAR(200)  NOT NULL,
    fecha        DATE          NOT NULL,
    saldada_en   TIMESTAMPTZ,
    creado_en    TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_deudas_usuario ON deudas (usuario_id, persona_id);

-- Pagos parciales. Que no superen lo pendiente lo garantiza el servicio,
-- bloqueando la deuda mientras se registra el abono.
CREATE TABLE abonos (
    id         UUID          PRIMARY KEY,
    deuda_id   UUID          NOT NULL REFERENCES deudas (id) ON DELETE CASCADE,
    monto      NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    fecha      DATE          NOT NULL,
    creado_en  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_abonos_deuda ON abonos (deuda_id);

-- Gastos que se repiten cada mes (arriendo, servicios, plan de celular).
CREATE TABLE pagos_fijos (
    id            UUID          PRIMARY KEY,
    usuario_id    UUID          NOT NULL REFERENCES usuarios (id),
    nombre        VARCHAR(60)   NOT NULL,
    monto         NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    dia_del_mes   SMALLINT      NOT NULL CHECK (dia_del_mes BETWEEN 1 AND 31),
    categoria_id  UUID          NOT NULL REFERENCES categorias (id),
    cuenta_id     UUID          NOT NULL REFERENCES cuentas (id),
    activo        BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en     TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_pagos_fijos_usuario ON pagos_fijos (usuario_id);

-- Pagar un pago fijo crea un movimiento normal que recuerda de qué pago y qué
-- mes es. El índice único impide pagar dos veces el mismo mes.
ALTER TABLE movimientos ADD COLUMN pago_fijo_id UUID REFERENCES pagos_fijos (id);
ALTER TABLE movimientos ADD COLUMN periodo CHAR(7);
ALTER TABLE movimientos ADD CONSTRAINT chk_pago_fijo_con_periodo CHECK ((pago_fijo_id IS NULL) = (periodo IS NULL));
CREATE UNIQUE INDEX uq_pago_fijo_periodo ON movimientos (pago_fijo_id, periodo) WHERE pago_fijo_id IS NOT NULL;

-- Metas de ahorro: un sobre para algo que se quiere (viaje, computador, colchón).
CREATE TABLE metas (
    id              UUID          PRIMARY KEY,
    usuario_id      UUID          NOT NULL REFERENCES usuarios (id),
    nombre          VARCHAR(60)   NOT NULL,
    monto_objetivo  NUMERIC(14,2) NOT NULL CHECK (monto_objetivo > 0),
    fecha_objetivo  DATE,
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_metas_usuario ON metas (usuario_id);

-- Aportes positivos y retiros negativos; lo ahorrado nunca queda en negativo.
CREATE TABLE aportes (
    id         UUID          PRIMARY KEY,
    meta_id    UUID          NOT NULL REFERENCES metas (id) ON DELETE CASCADE,
    monto      NUMERIC(14,2) NOT NULL CHECK (monto <> 0),
    fecha      DATE          NOT NULL,
    creado_en  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_aportes_meta ON aportes (meta_id);
