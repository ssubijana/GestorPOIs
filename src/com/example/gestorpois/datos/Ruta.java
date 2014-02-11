package com.example.gestorpois.datos;

public class Ruta {
	
	private long id;
	private String titulo;
	
	public Ruta(long id, String titulo) {
		super();
		this.id = id;
		this.titulo = titulo;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	@Override
	public String toString() {
		return titulo + " [id=" + id + "]";
	}
	
	

}
