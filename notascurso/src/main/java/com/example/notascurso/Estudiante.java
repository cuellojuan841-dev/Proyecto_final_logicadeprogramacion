package com.example.notascurso;

import java.util.List;

public class Estudiante {
    private String usuario;
    private String nombre;
    private List<String> asignaturas;

    public Estudiante(String usuario, String nombre, List<String> asignaturas) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.asignaturas = asignaturas;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getNombre() {
        return nombre;
    }

    public List<String> getAsignaturas() {
        return asignaturas;
    }
}
