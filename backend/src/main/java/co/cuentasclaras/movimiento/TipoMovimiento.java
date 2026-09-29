package co.cuentasclaras.movimiento;

/**
 * Lo que entra suma al saldo (INGRESO, TRANSFERENCIA_ENTRADA) y lo que sale
 * resta (GASTO, TRANSFERENCIA_SALIDA). Las transferencias no son ingreso ni
 * gasto: solo mueven plata entre cuentas propias (RN-06).
 */
public enum TipoMovimiento {
	INGRESO,
	GASTO,
	TRANSFERENCIA_SALIDA,
	TRANSFERENCIA_ENTRADA;

	public boolean esTransferencia() {
		return this == TRANSFERENCIA_SALIDA || this == TRANSFERENCIA_ENTRADA;
	}
}
