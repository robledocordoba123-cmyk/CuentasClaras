package co.cuentasclaras.comun;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Pesos colombianos para los mensajes: "$ 1.234.500", igual que en la pantalla. */
public final class Pesos {

	private static final DecimalFormatSymbols SIMBOLOS = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-CO"));

	private Pesos() {
	}

	public static String formato(BigDecimal valor) {
		DecimalFormat formato = new DecimalFormat("#,##0", SIMBOLOS);
		formato.setRoundingMode(RoundingMode.HALF_UP);
		return "$ " + formato.format(valor);
	}

}
