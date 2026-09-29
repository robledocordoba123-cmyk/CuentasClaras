package co.cuentasclaras.categoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Clasifica ingresos o gastos. Si usuarioId es null es una categoría por
 * defecto (Mercado, Transporte…): la ven todas las personas y nadie la edita.
 */
@Entity
@Table(name = "categorias")
public class Categoria {

	@Id
	private UUID id;

	@Column(name = "usuario_id", updatable = false)
	private UUID usuarioId;

	@Column(nullable = false, length = 60)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TipoCategoria tipo;

	@Column(nullable = false, length = 7)
	private String color;

	@Column(nullable = false)
	private boolean archivada;

	protected Categoria() {
	}

	public Categoria(UUID usuarioId, String nombre, TipoCategoria tipo, String color) {
		this.id = UUID.randomUUID();
		this.usuarioId = usuarioId;
		actualizar(nombre, tipo, color);
	}

	public void actualizar(String nombre, TipoCategoria tipo, String color) {
		this.nombre = nombre;
		this.tipo = tipo;
		this.color = color;
	}

	public boolean esPorDefecto() {
		return usuarioId == null;
	}

	public void archivar() {
		this.archivada = true;
	}

	public void restaurar() {
		this.archivada = false;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUsuarioId() {
		return usuarioId;
	}

	public String getNombre() {
		return nombre;
	}

	public TipoCategoria getTipo() {
		return tipo;
	}

	public String getColor() {
		return color;
	}

	public boolean isArchivada() {
		return archivada;
	}

}
