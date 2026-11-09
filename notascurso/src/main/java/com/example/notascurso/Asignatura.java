package com.example.notascurso;

import java.util.ArrayList;
import java.util.List;

public class Asignatura {
    private String nombre;
    private List<ActividadEvaluativa> actividades = new ArrayList<>();

    public Asignatura(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public List<ActividadEvaluativa> getActividades() {
        return actividades;
    }

    public void setActividades(List<ActividadEvaluativa> actividades) {
        this.actividades = actividades;
    }
}
