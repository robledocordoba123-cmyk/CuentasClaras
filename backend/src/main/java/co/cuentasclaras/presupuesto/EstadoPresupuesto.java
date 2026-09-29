package co.cuentasclaras.presupuesto;

import java.math.BigDecimal;

/** HU-07: semáforo del presupuesto según el porcentaje gastado. */
public enum EstadoPresupuesto {
	/** Menos del 80 %. */
	EN_CONTROL,
	/** Desde el 80 % hasta antes del 100 %: todavía hay margen, pero poco. */
	ALERTA,
	/** 100 % o más: se llegó o se pasó del tope. */
	EXCEDIDO;

	static final BigDecimal UMBRAL_ALERTA = new BigDecimal("80");
	static final BigDecimal UMBRAL_EXCEDIDO = new BigDecimal("100");

	static EstadoPresupuesto segun(BigDecimal porcentajeUsado) {
		if (porcentajeUsado.compareTo(UMBRAL_EXCEDIDO) >= 0) {
			return EXCEDIDO;
		}
		if (porcentajeUsado.compareTo(UMBRAL_ALERTA) >= 0) {
			return ALERTA;
		}
		return EN_CONTROL;
	}
}
