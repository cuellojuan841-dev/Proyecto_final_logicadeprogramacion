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
        // Armo el historial con las materias asociadas a la cuenta que inició sesión.
        addClassName("estudiante-view");
        setWidthFull();
        setPadding(true);
        add(new H2("Mi historial académico"));

        cuentaUsuario cuenta = usuarioService.getUsuarioActual();
        // Esta pantalla es solo para cuentas con rol de estudiante.
        if (cuenta == null || !esEstudiante(cuenta)) {
            add(new Paragraph("Inicia sesión con una cuenta de estudiante para consultar tus notas."));
            return;
        }

        Estudiante estudiante = new Estudiante(
                cuenta.getUsuario(), cuenta.getNombre(), cuenta.getObjetos());
        add(new H3("Estudiante: " + estudiante.getNombre()));
        List<String> asignaturas = estudiante.getAsignaturas();
        // Sin materias asignadas no hay paneles que mostrar.
        if (asignaturas == null || asignaturas.isEmpty()) {
            add(new Paragraph("No tienes asignaturas registradas en usuarios.txt."));
            return;
        }

        Accordion historial = new Accordion();
        historial.addClassName("historial-academico");
        historial.setWidthFull();

        // Creo un panel desplegable para cada asignatura del estudiante.
        for (String asignatura : asignaturas) {
            historial.add(crearPanel(estudiante, asignatura));
        }
        add(historial);
    }

    private AccordionPanel crearPanel(Estudiante estudiante, String nombreAsignatura) {
        // Cada panel del Accordion concentra las notas y la proyección de una asignatura.
        VerticalLayout contenido = new VerticalLayout();
        contenido.setPadding(true);
        contenido.setSpacing(true);

        try {
            // Cargo el plan de esta materia para mostrar sus actividades reales.
            Asignatura asignatura = notasService.obtenerAsignatura(nombreAsignatura);
            HorizontalLayout filaPeriodos = new HorizontalLayout();
            filaPeriodos.setWidthFull();
            filaPeriodos.setSpacing(true);
            filaPeriodos.getStyle().set("overflow-x", "auto");

            // Recorro las actividades y consulto la nota asociada a cada posición.
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
            // Después de las tarjetas muestro la proyección para alcanzar 3.0.
            contenido.add(new Paragraph(calcularProyeccion(estudiante, nombreAsignatura)));
        } catch (IOException e) {
            // Si falla la lectura, muestro el error y mantengo visible el panel.
            Notification.show("No se pudieron leer las notas: " + e.getMessage());
            contenido.add(new Paragraph("No fue posible cargar las notas de esta asignatura."));
        }

        return new AccordionPanel(new H3(nombreAsignatura), contenido);
    }

    private String calcularProyeccion(Estudiante estudiante, String asignatura)
            throws IOException {
        // Los puntos acumulados ya incluyen el peso de las notas; pendiente es la fracción sin calificar.
        double acumulado = notasService.puntosAcumulados(estudiante.getUsuario(), asignatura);
        double pendiente = notasService.porcentajePendiente(estudiante.getUsuario(), asignatura);
        double maximoPosible = acumulado + (5.0 * pendiente);

        // Primero reviso si ya llegó a los puntos necesarios para aprobar.
        if (acumulado >= 3.0) {
            return "Ya ganaste la asignatura: llevas " + formato(acumulado)
                    + " puntos ponderados.";
        }
        // Si ni sacando 5 en todo lo pendiente llega a 3.0, ya no es posible aprobar.
        if (maximoPosible < 3.0 - 1e-9) {
            return "Ya no te alcanza para llegar a 3.0. Tu máximo posible es "
                    + formato(maximoPosible) + ".";
        }
        // Si no queda porcentaje, la nota final ya está definida.
        if (pendiente <= 1e-9) {
            return "Asignatura finalizada. Tu nota es " + formato(acumulado) + ".";
        }

        // Divido los puntos que faltan por el peso restante para hallar la nota mínima.
        double notaNecesaria = (3.0 - acumulado) / pendiente;
        // Redondeo hacia arriba a un decimal para no mostrar una nota insuficiente.
        double notaNecesariaConUnDecimal = Math.ceil((notaNecesaria - 1e-9) * 10) / 10;
        if (notaNecesariaConUnDecimal > 5.0) {
            return "Ya no te alcanza para llegar a 3.0.";
        }

        return "Necesitas un promedio mínimo de " + formato(notaNecesariaConUnDecimal)
                + " en el " + Math.round(pendiente * 100) + "% pendiente para llegar a 3.0.";
    }

    private static boolean esEstudiante(cuentaUsuario usuario) {
        // Verifico el rol antes de mostrar información académica de esta vista.
        String rol = usuario.getRole();
        return rol != null && ("ESTUDIANTE".equalsIgnoreCase(rol)
                || "ROLE_ESTUDIANTE".equalsIgnoreCase(rol));
    }

    private static String formato(double valor) {
        return String.format(java.util.Locale.forLanguageTag("es-CO"), "%.1f", valor);
    }
}
