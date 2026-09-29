package co.cuentasclaras.movimiento;

import co.cuentasclaras.comun.Meses;
import co.cuentasclaras.comun.PaginaResponse;
import co.cuentasclaras.comun.UsuarioActual;
import co.cuentasclaras.movimiento.MovimientoDtos.MovimientoRequest;
import co.cuentasclaras.movimiento.MovimientoDtos.MovimientoResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoController {

	private final MovimientoService movimientoService;
	private final ExportadorCsv exportadorCsv;
	private final Meses meses;

	public MovimientoController(MovimientoService movimientoService, ExportadorCsv exportadorCsv, Meses meses) {
		this.movimientoService = movimientoService;
		this.exportadorCsv = exportadorCsv;
		this.meses = meses;
	}

	/** Ej.: /api/movimientos?desde=2026-09-01&hasta=2026-09-30&tipo=GASTO&pagina=0&tamano=20 */
	@GetMapping
	public PaginaResponse<MovimientoResponse> listar(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(required = false) UUID cuentaId,
			@RequestParam(required = false) UUID categoriaId,
			@RequestParam(required = false) TipoMovimiento tipo,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamano) {
		return movimientoService.listar(UsuarioActual.id(jwt), desde, hasta, cuentaId, categoriaId, tipo, pagina,
				tamano);
	}

	/** HU-09: descarga cuentasclaras-2026-09.csv. ?mes=2026-09 (por defecto, el actual). */
	@GetMapping("/exportar")
	public ResponseEntity<String> exportar(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) String mes) {
		YearMonth periodo = meses.parsearOActual(mes);
		String csv = exportadorCsv.exportar(movimientoService.delMes(UsuarioActual.id(jwt), periodo));
		return ResponseEntity.ok()
				.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename("cuentasclaras-" + periodo + ".csv").build().toString())
				.body(csv);
	}

	@GetMapping("/{id}")
	public MovimientoResponse obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return movimientoService.obtener(UsuarioActual.id(jwt), id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MovimientoResponse crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MovimientoRequest solicitud) {
		return movimientoService.crear(UsuarioActual.id(jwt), solicitud);
	}

	@PutMapping("/{id}")
	public MovimientoResponse actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@Valid @RequestBody MovimientoRequest solicitud) {
		return movimientoService.actualizar(UsuarioActual.id(jwt), id, solicitud);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		movimientoService.eliminar(UsuarioActual.id(jwt), id);
	}

}
