package com.example.notascurso;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.dependency.StyleSheet;
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
@StyleSheet("css/siggo.css")
public class EstudianteView extends VerticalLayout {
     final servicio usuarioService = new servicio();
     final NotasService notasService = new NotasService();

    public EstudianteView() {
        addClassName("estudiante-view");
        setWidthFull();
        setPadding(true);
        add(new H2("Mi historial académico"));

        cuentaUsuario cuenta = usuarioService.getUsuarioActual();
        if (cuenta == null || !esEstudiante(cuenta)) {
            add(new Paragraph("Inicia sesión con una cuenta de estudiante para consultar tus notas."));
            return;
        }

        Estudiante estudiante = new Estudiante(
                cuenta.getUsuario(), cuenta.getNombre(), cuenta.getObjetos());
        add(new H3("Estudiante: " + estudiante.getNombre()));
        List<String> asignaturas = estudiante.getAsignaturas();
        if (asignaturas == null || asignaturas.isEmpty()) {
            add(new Paragraph("No tienes asignaturas registradas en usuarios.txt."));
            return;
        }

        Accordion historial = new Accordion();
        historial.addClassName("historial-academico");
        historial.setWidthFull();

        for (String asignatura : asignaturas) {
            historial.add(crearPanel(estudiante, asignatura));
        }
        add(historial);
    }

    private AccordionPanel crearPanel(Estudiante estudiante, String nombreAsignatura) {
        VerticalLayout contenido = new VerticalLayout();
        contenido.setPadding(true);
        contenido.setSpacing(true);

        try {
            Asignatura asignatura = notasService.obtenerAsignatura(nombreAsignatura);
            HorizontalLayout filaPeriodos = new HorizontalLayout();
            filaPeriodos.setWidthFull();
            filaPeriodos.setSpacing(true);
            filaPeriodos.getStyle().set("overflow-x", "auto");

            for (int indice = 0; indice < asignatura.getActividades().size(); indice++) {
                int numeroActividad = indice + 1;
                ActividadEvaluativa actividad = asignatura.getActividades().get(indice);
                Double valor = notasService.obtenerNota(
                        estudiante.getUsuario(), nombreAsignatura, numeroActividad);
                VerticalLayout tarjetaPeriodo = new VerticalLayout(
                        new Paragraph(actividad.getNombre()),
                        new H3(formato(actividad.getPorcentaje()) + "%"),
                        new Paragraph(valor == null ? "Pendiente" : "Nota: " + formato(valor))
                );
                tarjetaPeriodo.addClassName("periodo-card");
                tarjetaPeriodo.setMinWidth("140px");
                tarjetaPeriodo.setSpacing(false);
                tarjetaPeriodo.getStyle()
                        .set("border", "1px solid #dddddd")
                        .set("border-radius", "8px");
                filaPeriodos.add(tarjetaPeriodo);
            }
            contenido.add(filaPeriodos);
            contenido.add(new Paragraph(calcularProyeccion(estudiante, nombreAsignatura)));
        } catch (IOException e) {
            Notification.show("No se pudieron leer las notas: " + e.getMessage());
            contenido.add(new Paragraph("No fue posible cargar las notas de esta asignatura."));
        }

        return new AccordionPanel(new H3(nombreAsignatura), contenido);
    }

    private String calcularProyeccion(Estudiante estudiante, String asignatura)
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

    private static String formato(double valor) {
        return String.format(java.util.Locale.forLanguageTag("es-CO"), "%.1f", valor);
    }
}
