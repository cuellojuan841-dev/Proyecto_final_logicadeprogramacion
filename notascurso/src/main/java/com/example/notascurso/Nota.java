package com.example.notascurso;

public class Nota {
    private final String usuarioEstudiante;
    private final String asignatura;
    private final int periodo;
    private final double valor;

    public Nota(String usuarioEstudiante, String asignatura, int periodo, double valor) {
        this.usuarioEstudiante = usuarioEstudiante;
        this.asignatura = asignatura;
        this.periodo = periodo;
        this.valor = valor;
    }

    public String getUsuarioEstudiante() {
        return usuarioEstudiante;
    }

    public String getAsignatura() {
        return asignatura;
    }

    public int getPeriodo() {
        return periodo;
    }

    public double getValor() {
        return valor;
    }
}
