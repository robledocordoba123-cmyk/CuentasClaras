package co.cuentasclaras.demo;

import co.cuentasclaras.categoria.Categoria;
import co.cuentasclaras.categoria.CategoriaRepository;
import co.cuentasclaras.categoria.TipoCategoria;
import co.cuentasclaras.comun.Meses;
import co.cuentasclaras.cuenta.Cuenta;
import co.cuentasclaras.cuenta.CuentaRepository;
import co.cuentasclaras.cuenta.TipoCuenta;
import co.cuentasclaras.movimiento.Movimiento;
import co.cuentasclaras.movimiento.MovimientoRepository;
import co.cuentasclaras.movimiento.TipoMovimiento;
import co.cuentasclaras.presupuesto.Presupuesto;
import co.cuentasclaras.presupuesto.PresupuestoRepository;
import co.cuentasclaras.usuario.Usuario;
import co.cuentasclaras.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Random;
import java.util.UUID;

/**
 * Cuenta de demostración para la demo pública: solo se activa con el perfil
 * "demo". En cada arranque borra los datos de esa cuenta y los vuelve a crear,
 * así quien visite siempre encuentra tres meses de movimientos hasta el día de
 * hoy, sin importar lo que hayan cambiado los visitantes anteriores.
 *
 * <p>No toca los datos de ninguna otra persona registrada.
 */
@Component
@Profile("demo")
public class DatosDeDemo implements ApplicationRunner {

	public static final String EMAIL = "demo@cuentasclaras.co";
	public static final String PASSWORD = "Demo2026!";

	private static final Logger log = LoggerFactory.getLogger(DatosDeDemo.class);
	private static final UUID MERCADO = UUID.fromString("00000000-0000-0000-0000-000000000201");
	private static final UUID TRANSPORTE = UUID.fromString("00000000-0000-0000-0000-000000000202");
	private static final UUID ARRIENDO = UUID.fromString("00000000-0000-0000-0000-000000000203");
	private static final UUID SERVICIOS = UUID.fromString("00000000-0000-0000-0000-000000000204");
	private static final UUID DOMICILIOS = UUID.fromString("00000000-0000-0000-0000-000000000205");
	private static final UUID SALUD = UUID.fromString("00000000-0000-0000-0000-000000000206");
	private static final UUID ENTRETENIMIENTO = UUID.fromString("00000000-0000-0000-0000-000000000208");
	private static final UUID SALARIO = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID EXTRAS = UUID.fromString("00000000-0000-0000-0000-000000000102");

	private final UsuarioRepository usuarioRepository;
	private final CuentaRepository cuentaRepository;
	private final CategoriaRepository categoriaRepository;
	private final MovimientoRepository movimientoRepository;
	private final PresupuestoRepository presupuestoRepository;
	private final PasswordEncoder passwordEncoder;
	private final JdbcTemplate jdbc;
	private final co.cuentasclaras.usuario.BorradoDeDatos borradoDeDatos;
	private final Meses meses;
	private final Clock reloj;

	public DatosDeDemo(UsuarioRepository usuarioRepository, CuentaRepository cuentaRepository,
			CategoriaRepository categoriaRepository, MovimientoRepository movimientoRepository,
			PresupuestoRepository presupuestoRepository, PasswordEncoder passwordEncoder, JdbcTemplate jdbc, co.cuentasclaras.usuario.BorradoDeDatos borradoDeDatos,
			Meses meses, Clock reloj) {
		this.usuarioRepository = usuarioRepository;
		this.cuentaRepository = cuentaRepository;
		this.categoriaRepository = categoriaRepository;
		this.movimientoRepository = movimientoRepository;
		this.presupuestoRepository = presupuestoRepository;
		this.passwordEncoder = passwordEncoder;
		this.jdbc = jdbc;
		this.borradoDeDatos = borradoDeDatos;
		this.meses = meses;
		this.reloj = reloj;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		borrarDemoAnterior();
		Usuario usuario = usuarioRepository.save(new Usuario("Camila (demo)", EMAIL, passwordEncoder.encode(PASSWORD)));
		UUID u = usuario.getId();

		Cuenta banco = cuentaRepository.save(new Cuenta(u, "Bancolombia", TipoCuenta.BANCO, new BigDecimal("800000")));
		Cuenta nequi = cuentaRepository.save(new Cuenta(u, "Nequi", TipoCuenta.BILLETERA_DIGITAL, new BigDecimal("150000")));
		Cuenta efectivo = cuentaRepository.save(new Cuenta(u, "Efectivo", TipoCuenta.EFECTIVO, new BigDecimal("60000")));
		Categoria mascotas = categoriaRepository.save(new Categoria(u, "Mascotas", TipoCategoria.GASTO, "#8C5A3C"));

		// Semilla fija: la demo sale igual en cada arranque (con fechas al día).
		Random azar = new Random(2026);
		LocalDate hoy = LocalDate.now(reloj.withZone(ZoneId.of("America/Bogota")));
		YearMonth actual = meses.actual();

		for (YearMonth mes = actual.minusMonths(2); !mes.isAfter(actual); mes = mes.plusMonths(1)) {
			Registro r = new Registro(u, hoy, mes, azar);
			r.movimiento(banco, SALARIO, TipoMovimiento.INGRESO, 2_800_000, 1, "Sueldo");
			r.movimiento(banco, ARRIENDO, TipoMovimiento.GASTO, 950_000, 3, "Arriendo apartamento");
			r.movimiento(banco, SERVICIOS, TipoMovimiento.GASTO, 180_000 + r.variar(60_000), 8, "Agua, luz y gas");
			r.movimiento(nequi, SERVICIOS, TipoMovimiento.GASTO, 65_000, 12, "Plan de celular");
			if (mes.getMonthValue() % 2 == 0) {
				r.movimiento(nequi, EXTRAS, TipoMovimiento.INGRESO, 350_000, 18, "Diseño de un logo (freelance)");
			}
			for (int dia : new int[] { 4, 11, 18, 25 }) {
				r.movimiento(dia % 2 == 0 ? nequi : efectivo, MERCADO, TipoMovimiento.GASTO, 85_000 + r.variar(70_000),
						dia, dia == 4 ? "Mercado grande del mes" : "Mercado de la semana");
			}
			for (int dia = 2; dia <= 28; dia += 3) {
				r.movimiento(efectivo, TRANSPORTE, TipoMovimiento.GASTO, 5_900 + r.variar(12_000), dia, "Metro y bus");
			}
			// El primer domicilio y el streaming caen el día 1: así, cualquier día del mes,
			// los presupuestos muestran los tres estados (en control, alerta y excedido).
			for (int dia : new int[] { 1, 6, 13, 20, 24, 27 }) {
				r.movimiento(nequi, DOMICILIOS, TipoMovimiento.GASTO, 24_000 + r.variar(22_000), dia, "Domicilio de comida");
			}
			r.movimiento(nequi, ENTRETENIMIENTO, TipoMovimiento.GASTO, 38_900, 1, "Suscripciones de streaming");
			r.movimiento(efectivo, ENTRETENIMIENTO, TipoMovimiento.GASTO, 45_000 + r.variar(40_000), 21, "Salida con amigos");
			r.movimiento(nequi, mascotas.getId(), TipoMovimiento.GASTO, 72_000, 15, "Concentrado para Luna");
			if (mes.equals(actual.minusMonths(1))) {
				r.movimiento(banco, SALUD, TipoMovimiento.GASTO, 95_000, 22, "Cita médica particular");
			}
			// Lo que se pasa a Nequi y al efectivo alcanza para sus gastos del mes:
			// ninguna cuenta de la demo queda con saldo negativo.
			r.transferencia(banco, efectivo, 450_000, 2, "Retiro en cajero");
			r.transferencia(banco, nequi, 500_000, 1, "Recarga Nequi");
		}

		definirPresupuestos(u, actual, mascotas.getId());
		personasPagosFijosYMetas(u, hoy, actual, banco, nequi);
		log.info("Datos de demostración listos: {} / {}", EMAIL, PASSWORD);
	}

	/**
	 * Presupuestos del mes actual calculados sobre lo ya gastado, para que la
	 * demo siempre muestre los tres estados: en control, alerta y excedido.
	 */
	private void definirPresupuestos(UUID u, YearMonth mes, UUID mascotas) {
		presupuesto(u, mes, MERCADO, 0.55);
		presupuesto(u, mes, DOMICILIOS, 1.12);
		presupuesto(u, mes, ENTRETENIMIENTO, 0.86);
		presupuesto(u, mes, TRANSPORTE, 0.40);
		presupuesto(u, mes, mascotas, 0.70);
	}

	private void presupuesto(UUID u, YearMonth mes, UUID categoria, double fraccionUsada) {
		BigDecimal gastado = movimientoRepository.gastosPorCategoria(u, mes.atDay(1), mes.atEndOfMonth()).stream()
				.filter(t -> t.getCategoriaId().equals(categoria)).map(MovimientoRepository.TotalPorCategoria::getTotal)
				.findFirst().orElse(BigDecimal.ZERO);
		BigDecimal limite = gastado.signum() == 0 ? new BigDecimal("100000")
				: gastado.divide(BigDecimal.valueOf(fraccionUsada), -3, RoundingMode.HALF_UP); // redondeado a miles
		presupuestoRepository.save(new Presupuesto(u, categoria, mes, limite));
	}

	/**
	 * Cuentas con personas, pagos fijos y metas, con fechas relativas a hoy. Los
	 * arriendos y planes de celular que ya trae la demo quedan ligados a su pago fijo.
	 */
	private void personasPagosFijosYMetas(UUID u, LocalDate hoy, YearMonth actual, Cuenta banco, Cuenta nequi) {
		UUID valentina = persona(u, "Valentina");
		UUID andres = persona(u, "Andrés");
		UUID mama = persona(u, "Mamá");
		UUID almuerzo = deuda(u, valentina, "ME_DEBEN", 60_000, "Mitad del almuerzo y el cine", hoy.minusDays(10));
		abono(almuerzo, 20_000, hoy.minusDays(3));
		deuda(u, andres, "DEBO", 150_000, "Me prestó para el pasaje a Bogotá", hoy.minusDays(20));
		UUID mercado = deuda(u, mama, "ME_DEBEN", 45_000, "Mercado que le hice", hoy.minusDays(30));
		abono(mercado, 45_000, hoy.minusDays(25));
		jdbc.update("UPDATE deudas SET saldada_en = now() WHERE id = ?", mercado);

		UUID arriendo = pagoFijo(u, "Arriendo", 950_000, 3, ARRIENDO, banco.getId());
		UUID celular = pagoFijo(u, "Plan de celular", 65_000, 12, SERVICIOS, nequi.getId());
		pagoFijo(u, "Internet", 89_900, 20, SERVICIOS, banco.getId());
		jdbc.update("UPDATE movimientos SET pago_fijo_id = ?, periodo = to_char(fecha, 'YYYY-MM') WHERE usuario_id = ? AND descripcion = 'Arriendo apartamento'", arriendo, u);
		jdbc.update("UPDATE movimientos SET pago_fijo_id = ?, periodo = to_char(fecha, 'YYYY-MM') WHERE usuario_id = ? AND descripcion = 'Plan de celular'", celular, u);

		UUID viaje = meta(u, "Viaje a Cartagena", 1_800_000, actual.plusMonths(5).atEndOfMonth());
		UUID emergencia = meta(u, "Fondo de emergencia", 3_000_000, null);
		for (int i = 2; i >= 0; i--) {
			LocalDate dia = actual.minusMonths(i).atDay(2);
			if (!dia.isAfter(hoy)) {
				aporte(viaje, 200_000, dia);
				aporte(emergencia, 150_000, dia);
			}
		}
	}

	private UUID persona(UUID u, String nombre) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO personas (id, usuario_id, nombre) VALUES (?, ?, ?)", id, u, nombre);
		return id;
	}

	private UUID deuda(UUID u, UUID persona, String direccion, int monto, String descripcion, LocalDate fecha) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO deudas (id, usuario_id, persona_id, direccion, monto, descripcion, fecha) VALUES (?, ?, ?, ?, ?, ?, ?)",
				id, u, persona, direccion, BigDecimal.valueOf(monto), descripcion, java.sql.Date.valueOf(fecha));
		return id;
	}

	private void abono(UUID deuda, int monto, LocalDate fecha) {
		jdbc.update("INSERT INTO abonos (id, deuda_id, monto, fecha) VALUES (?, ?, ?, ?)", UUID.randomUUID(), deuda,
				BigDecimal.valueOf(monto), java.sql.Date.valueOf(fecha));
	}

	private UUID pagoFijo(UUID u, String nombre, int monto, int dia, UUID categoria, UUID cuenta) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO pagos_fijos (id, usuario_id, nombre, monto, dia_del_mes, categoria_id, cuenta_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
				id, u, nombre, BigDecimal.valueOf(monto), dia, categoria, cuenta);
		return id;
	}

	private UUID meta(UUID u, String nombre, int objetivo, LocalDate fecha) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO metas (id, usuario_id, nombre, monto_objetivo, fecha_objetivo) VALUES (?, ?, ?, ?, ?)",
				id, u, nombre, BigDecimal.valueOf(objetivo), fecha == null ? null : java.sql.Date.valueOf(fecha));
		return id;
	}

	private void aporte(UUID meta, int monto, LocalDate fecha) {
		jdbc.update("INSERT INTO aportes (id, meta_id, monto, fecha) VALUES (?, ?, ?, ?)", UUID.randomUUID(), meta,
				BigDecimal.valueOf(monto), java.sql.Date.valueOf(fecha));
	}

	private void borrarDemoAnterior() {
		usuarioRepository.findByEmail(EMAIL).ifPresent(anterior -> borradoDeDatos.borrarTodoDe(anterior.getId()));
	}

	/** Ayudante para crear movimientos de un mes sin pasarse de hoy. */
	private final class Registro {

		private final UUID usuarioId;
		private final LocalDate hoy;
		private final YearMonth mes;
		private final Random azar;

		Registro(UUID usuarioId, LocalDate hoy, YearMonth mes, Random azar) {
			this.usuarioId = usuarioId;
			this.hoy = hoy;
			this.mes = mes;
			this.azar = azar;
		}

		int variar(int maximo) {
			return azar.nextInt(maximo / 100) * 100;
		}

		private LocalDate fecha(int dia) {
			return mes.atDay(Math.min(dia, mes.lengthOfMonth()));
		}

		void movimiento(Cuenta cuenta, UUID categoria, TipoMovimiento tipo, int monto, int dia, String descripcion) {
			LocalDate fecha = fecha(dia);
			if (fecha.isAfter(hoy)) {
				return;
			}
			movimientoRepository.save(Movimiento.ingresoOGasto(usuarioId, cuenta.getId(), categoria, tipo,
					BigDecimal.valueOf(monto), fecha, descripcion));
		}

		void transferencia(Cuenta origen, Cuenta destino, int monto, int dia, String descripcion) {
			LocalDate fecha = fecha(dia);
			if (fecha.isAfter(hoy)) {
				return;
			}
			UUID id = UUID.randomUUID();
			movimientoRepository.save(Movimiento.parteDeTransferencia(usuarioId, origen.getId(),
					TipoMovimiento.TRANSFERENCIA_SALIDA, BigDecimal.valueOf(monto), fecha, descripcion, id));
			movimientoRepository.save(Movimiento.parteDeTransferencia(usuarioId, destino.getId(),
					TipoMovimiento.TRANSFERENCIA_ENTRADA, BigDecimal.valueOf(monto), fecha, descripcion, id));
		}

	}

}
