package co.cuentasclaras.movimiento;

import co.cuentasclaras.movimiento.MovimientoDtos.MovimientoResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * HU-09: movimientos en CSV listo para abrir con doble clic en Excel en
 * español (Colombia):
 * <ul>
 *   <li>Separador ";" y decimales con coma: con configuración regional
 *       colombiana, Excel usa la coma como separador decimal.</li>
 *   <li>BOM al inicio: sin él, Excel muestra mal las tildes y la ñ.</li>
 *   <li>Gastos y salidas con signo negativo, para poder sumar la columna.</li>
 * </ul>
 */
@Component
public class ExportadorCsv {

	private static final String BOM = "﻿";
	private static final String SEPARADOR = ";";

	public String exportar(List<MovimientoResponse> movimientos) {
		StringBuilder csv = new StringBuilder(BOM)
				.append(String.join(SEPARADOR, "Fecha", "Tipo", "Cuenta", "Categoría", "Descripción", "Monto"))
				.append("\r\n");
		for (MovimientoResponse m : movimientos) {
			csv.append(String.join(SEPARADOR,
					m.fecha().toString(),
					nombreTipo(m.tipo()),
					celda(m.cuenta().nombre()),
					celda(m.categoria() == null ? "Transferencia" : m.categoria().nombre()),
					celda(m.descripcion()),
					monto(m)))
					.append("\r\n");
		}
		return csv.toString();
	}

	private static String monto(MovimientoResponse m) {
		BigDecimal conSigno = (m.tipo() == TipoMovimiento.GASTO || m.tipo() == TipoMovimiento.TRANSFERENCIA_SALIDA)
				? m.monto().negate() : m.monto();
		return conSigno.setScale(2).toPlainString().replace('.', ',');
	}

	private static String nombreTipo(TipoMovimiento tipo) {
		return switch (tipo) {
			case INGRESO -> "Ingreso";
			case GASTO -> "Gasto";
			case TRANSFERENCIA_SALIDA -> "Transferencia (salida)";
			case TRANSFERENCIA_ENTRADA -> "Transferencia (entrada)";
		};
	}

	/**
	 * Texto escrito por la persona. Dos protecciones:
	 * <ol>
	 *   <li>Si empieza por = + - @, Excel lo ejecutaría como fórmula (inyección
	 *       de fórmulas en CSV); se antepone un apóstrofo para que sea texto.</li>
	 *   <li>Si tiene ; comillas o saltos de línea, va entre comillas dobles y las
	 *       comillas internas se duplican, como indica el formato CSV.</li>
	 * </ol>
	 */
	static String celda(String texto) {
		if (texto == null) {
			return "";
		}
		String valor = texto;
		if (!valor.isEmpty() && "=+-@".indexOf(valor.charAt(0)) >= 0) {
			valor = "'" + valor;
		}
		if (valor.contains(SEPARADOR) || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
			valor = "\"" + valor.replace("\"", "\"\"") + "\"";
		}
		return valor;
	}

}
