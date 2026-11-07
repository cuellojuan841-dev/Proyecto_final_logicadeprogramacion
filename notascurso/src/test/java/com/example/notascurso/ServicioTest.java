package com.example.notascurso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServicioTest {
    @Test
    void leeUsuariosSeparadosPorBarrasYMateriasSeparadasPorComas() throws IOException {
        String datos = "profesor1|1234|Daniel Castillo|PROFESOR|Matemáticas, Historia\n"
                + "estudiante1|1234|Juan Andres|ESTUDIANTE|Matemáticas";

        List<cuentaUsuario> usuarios = servicio.leerUsuarios(
                new BufferedReader(new StringReader(datos)));

        assertEquals(2, usuarios.size());
        assertEquals("Daniel Castillo", usuarios.get(0).getNombre());
        assertEquals(List.of("Matemáticas", "Historia"), usuarios.get(0).getObjetos());
        assertEquals("ESTUDIANTE", usuarios.get(1).getRole());
    }

    @Test
    void rechazaLineaConCantidadIncorrectaDeCamposYReportaSuNumero() {
        String datos = "profesor1|1234|Daniel Castillo|PROFESOR|Matemáticas\n"
                + "estudiante1|1234|Juan Andres|ESTUDIANTE";

        IOException error = assertThrows(IOException.class, () -> servicio.leerUsuarios(
                new BufferedReader(new StringReader(datos))));

        assertTrue(error.getMessage().contains("línea 2"));
    }

    @Test
    void rechazaCamposObligatoriosVaciosRolesInvalidosYMateriasVacias() {
        assertThrows(IOException.class, () -> leer("usuario||Nombre|PROFESOR|Matemáticas"));
        assertThrows(IOException.class, () -> leer("usuario|1234|Nombre|ADMIN|Matemáticas"));
        assertThrows(IOException.class, () -> leer("usuario|1234|Nombre|ESTUDIANTE|Matemáticas,"));
    }

    private static List<cuentaUsuario> leer(String datos) throws IOException {
        return servicio.leerUsuarios(new BufferedReader(new StringReader(datos)));
    }
}
