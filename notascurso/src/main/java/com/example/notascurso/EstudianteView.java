package com.example.notascurso;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.util.List;

@Route(value = "estudiante", layout = DashboardLayout.class)
@PageTitle("Notas del curso | Estudiante")
public class EstudianteView extends VerticalLayout {
     static final List<String> ASIGNATURAS = List.of(
            "Inglés",
            "Lógica de programación",
            "Cálculo integral",
            "Física",
            "Redes de datos"
    );

     final servicio usuarioService = new servicio();
     final NotasService notasService = new NotasService();

    public EstudianteView() {
        setWidthFull();
        setPadding(true);
        add(new H2("Mi historial académico"));

        cuentaUsuario estudiante = usuarioService.getUsuarioActual();
        if (estudiante == null || !esEstudiante(estudiante)) {
            add(new Paragraph("Inicia sesión con una cuenta de estudiante para consultar tus notas."));
            return;
        }

        add(new H3("Estudiante: " + estudiante.getNombre()));
        Accordion historial = new Accordion();
        historial.setWidthFull();

        for (String asignatura : ASIGNATURAS) {
            historial.add(crearPanel(estudiante, asignatura));
        }
        add(historial);
    }

    private AccordionPanel crearPanel(cuentaUsuario estudiante, String asignatura) {
        VerticalLayout contenido = new VerticalLayout();
        contenido.setPadding(true);
        contenido.setSpacing(true);

        try {
            HorizontalLayout filaPeriodos = new HorizontalLayout();
            filaPeriodos.setWidthFull();
            filaPeriodos.setSpacing(true);
            filaPeriodos.getStyle().set("overflow-x", "auto");

            for (int periodo = 1; periodo <= 3; periodo++) {
                Double valor = notasService.obtenerNota(
                        estudiante.getUsuario(), asignatura, periodo);
                VerticalLayout tarjetaPeriodo = new VerticalLayout(
                        new Paragraph("Período " + periodo),
                        new H3(pesoPeriodo(periodo) + "%"),
                        new Paragraph(valor == null ? "Pendiente" : "Nota: " + formato(valor))
                );
                tarjetaPeriodo.setMinWidth("140px");
                tarjetaPeriodo.setSpacing(false);
                tarjetaPeriodo.getStyle()
                        .set("border", "1px solid #dddddd")
                        .set("border-radius", "8px");
                filaPeriodos.add(tarjetaPeriodo);
            }
            contenido.add(filaPeriodos);
            contenido.add(new Paragraph(calcularProyeccion(estudiante, asignatura)));
        } catch (IOException e) {
            Notification.show("No se pudieron leer las notas: " + e.getMessage());
            contenido.add(new Paragraph("No fue posible cargar las notas de esta asignatura."));
        }

        return new AccordionPanel(new H3(asignatura), contenido);
    }

    private String calcularProyeccion(cuentaUsuario estudiante, String asignatura)
            throws IOException {
        double acumulado = notasService.puntosAcumulados(estudiante.getUsuario(), asignatura);
        double pendiente = notasService.porcentajePendiente(estudiante.getUsuario(), asignatura);
        double maximoPosible = acumulado + (5.0 * pendiente);

        if (acumulado >= 3.0) {
            return "Ya ganaste la asignatura: llevas " + formato(acumulado)
                    + " puntos ponderados.";
        }
        if (maximoPosible < 3.0 - 1e-9) {
            return "Ya no te alcanza para llegar a 3.0. Tu máximo posible es "
                    + formato(maximoPosible) + ".";
        }
        if (pendiente <= 1e-9) {
            return "Asignatura finalizada. Tu nota es " + formato(acumulado) + ".";
        }

        double notaNecesaria = (3.0 - acumulado) / pendiente;
        double notaNecesariaConUnDecimal = Math.ceil((notaNecesaria - 1e-9) * 10) / 10;
        if (notaNecesariaConUnDecimal > 5.0) {
            return "Ya no te alcanza para llegar a 3.0.";
        }

        return "Necesitas un promedio mínimo de " + formato(notaNecesariaConUnDecimal)
                + " en el " + Math.round(pendiente * 100) + "% pendiente para llegar a 3.0.";
    }

    private static boolean esEstudiante(cuentaUsuario usuario) {
        String rol = usuario.getRole();
        return rol != null && ("ESTUDIANTE".equalsIgnoreCase(rol)
                || "ROLE_ESTUDIANTE".equalsIgnoreCase(rol));
    }

    private static int pesoPeriodo(int periodo) {
        return periodo == 3 ? 40 : 30;
    }

    private static String formato(double valor) {
        return String.format(java.util.Locale.forLanguageTag("es-CO"), "%.1f", valor);
    }
}
