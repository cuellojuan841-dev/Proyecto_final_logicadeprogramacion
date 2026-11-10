package com.example.notascurso;

// Guarda el nombre de una actividad y qué parte de la nota final representa.
public class ActividadEvaluativa {
    // Por ejemplo, un examen puede valer el 40% del curso.
    private String nombre;
    private double porcentaje;

    // Recibe los datos que luego se muestran y usan para ponderar la nota.
    public ActividadEvaluativa(String nombre, double porcentaje) {
        this.nombre = nombre;
        this.porcentaje = porcentaje;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(double porcentaje) {
        this.porcentaje = porcentaje;
    }
}
