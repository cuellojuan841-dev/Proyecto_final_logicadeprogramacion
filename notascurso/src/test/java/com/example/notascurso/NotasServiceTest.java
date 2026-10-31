package com.example.notascurso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NotasServiceTest {
    @TempDir
    Path directorioTemporal;

    @Test
    void guardaActualizaYBorraNotasEnArchivo() throws IOException {
        Path archivo = directorioTemporal.resolve("notas.txt");
        NotasService servicio = new NotasService(archivo);

        servicio.guardarNota("estudiante1", "Inglés", 1, 4.2);
        servicio.guardarNota("estudiante1", "Inglés", 1, 3.8);

        assertEquals(3.8, servicio.obtenerNota("estudiante1", "Inglés", 1));
        assertEquals(3.8, servicio.promedioActual("estudiante1", "Inglés"), 1e-9);
        assertEquals(3.8, new NotasService(archivo)
                .obtenerNota("estudiante1", "Inglés", 1));
        assertTrue(servicio.eliminarNota("estudiante1", "Inglés", 1));
        assertFalse(servicio.eliminarNota("estudiante1", "Inglés", 1));
        assertNull(servicio.promedioActual("estudiante1", "Inglés"));
    }

    @Test
    void calculaPromedioConPesosDePeriodosCalificados() throws IOException {
        NotasService servicio = new NotasService(directorioTemporal.resolve("notas.txt"));
        servicio.guardarNota("estudiante1", "Física", 1, 4.0);
        servicio.guardarNota("estudiante1", "Física", 3, 5.0);

        Double promedio = servicio.promedioActual("estudiante1", "Física");

        assertEquals((4.0 * 0.30 + 5.0 * 0.40) / 0.70, promedio, 1e-9);
        assertEquals(0.30, servicio.porcentajePendiente("estudiante1", "Física"), 1e-9);
        assertEquals(3.2, servicio.puntosAcumulados("estudiante1", "Física"), 1e-9);
    }

    @Test
    void rechazaNotasFueraDeRangoOMasDeUnDecimal() {
        NotasService servicio = new NotasService(directorioTemporal.resolve("notas.txt"));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarNota("estudiante1", "Inglés", 1, 5.1));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarNota("estudiante1", "Inglés", 1, 3.25));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarNota("estudiante1", "Inglés", 4, 3.0));
    }
}
