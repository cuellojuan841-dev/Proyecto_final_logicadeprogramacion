package com.example.notascurso;

import java.util.List;

public class Estudiante {
    // El usuario permite buscar las notas guardadas para esta persona.
    private String usuario;
    private String nombre;
    // Son las materias que se usan para construir su historial académico.
    private List<String> asignaturas;

    // Agrupo los datos necesarios para mostrar el historial del estudiante.
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
