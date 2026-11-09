package com.example.notascurso;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.Locale;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Route(value = "profesor", layout = DashboardLayout.class)
@PageTitle("Notas del curso | Profesor")
@StyleSheet("css/siggo.css")
public class ProfesorView extends VerticalLayout {
    private final servicio usuarioService = new servicio();
    private final NotasService notasService = new NotasService();
    private final VerticalLayout detalleAsignatura = new VerticalLayout();
    private final List<cuentaUsuario> estudiantes;
    private final List<String> asignaturas;

    public ProfesorView() {
        addClassName("profesor-view");
        setWidthFull();
        setPadding(true);
        setSpacing(true);

        cuentaUsuario usuarioActual = usuarioService.getUsuarioActual();
        if (usuarioActual == null || !esProfesor(usuarioActual)) {
            add(new H2("Gestión de notas"),
                    new Paragraph("Inicia sesión con una cuenta de profesor para continuar."));
            estudiantes = List.of();
            asignaturas = List.of();
            return;
        }

        add(new H1("Bienvenido profesor " + usuarioActual.getNombre()));
        asignaturas = usuarioActual.getObjetos() == null
                ? List.of()
                : new ArrayList<>(usuarioActual.getObjetos());

        List<cuentaUsuario> usuarios;
        try {
            usuarios = usuarioService.cargarUsuarios();
        } catch (IOException e) {
            add(new Paragraph("No se pudieron cargar los usuarios: " + e.getMessage()));
            estudiantes = List.of();
            return;
        }
        List<cuentaUsuario> listaEstudiantes = new ArrayList<>();
        for (cuentaUsuario usuario : usuarios) {
            if (esEstudiante(usuario)) {
                listaEstudiantes.add(usuario);
            }
        }
        estudiantes = listaEstudiantes;

        if (asignaturas.isEmpty()) {
            add(new H2("Gestión de notas"),
                    new Paragraph("Este profesor no tiene asignaturas asignadas en usuarios.txt."));
            return;
        }

        add(new H2("Gestión de notas"), new H3("Seleccione una materia"));

        HorizontalLayout materias = new HorizontalLayout();
        materias.addClassName("materias-profesor");
        materias.setWidthFull();
        materias.setPadding(true);
        materias.setSpacing(true);
        materias.getStyle().set("overflow-x", "auto");

        for (String asignatura : asignaturas) {
            materias.add(crearTarjetaMateria(asignatura));
        }

        detalleAsignatura.setWidthFull();
        add(materias, detalleAsignatura);
    }

    private VerticalLayout crearTarjetaMateria(String nombreMateria) {
        H3 nombre = new H3(nombreMateria);
        nombre.addClassName("materia-title");
        Button ingresarNotas = new Button("Ingresar notas");
        ingresarNotas.addClassName("primary-button");
        ingresarNotas.addClickListener(event -> mostrarAsignatura(nombreMateria));
        Button configurarPlan = new Button("Configurar evaluación");
        configurarPlan.addClassName("secondary-button");
        configurarPlan.addClickListener(event -> abrirConfiguracionPlan(nombreMateria));

        VerticalLayout tarjeta = new VerticalLayout(nombre, ingresarNotas, configurarPlan);
        tarjeta.addClassName("materia-card");
        tarjeta.setWidth("220px");
        tarjeta.setMinWidth("220px");
        tarjeta.setPadding(true);
        tarjeta.setSpacing(true);
        tarjeta.getStyle()
                .set("border", "1px solid #cccccc")
                .set("border-radius", "8px");
        return tarjeta;
    }

    private void mostrarAsignatura(String asignatura) {
        TextField busqueda = new TextField("buscar estudiante");
        busqueda.setPlaceholder("escribe un nombre");
        busqueda.setClearButtonVisible(true);
        busqueda.setValueChangeMode(ValueChangeMode.EAGER);
        ComboBox<String> filtroEstado = new ComboBox<>("filtrar promedio");
        filtroEstado.setItems("Todos", "Sin notas", "Menor a 3.0", "3.0 o más");
        filtroEstado.setValue("Todos");

        HorizontalLayout filtros = new HorizontalLayout(busqueda, filtroEstado);
        filtros.addClassName("filtros-profesor");
        filtros.setWidthFull();
        busqueda.setWidthFull();
        filtroEstado.setWidthFull();
        detalleAsignatura.removeAll();
        detalleAsignatura.add(new H2(asignatura));

        Asignatura materia;
        try {
            materia = notasService.obtenerAsignatura(asignatura);
        } catch (IOException e) {
            Notification.show("No se pudo cargar el plan de evaluación: " + e.getMessage());
            return;
        }

        List<cuentaUsuario> estudiantesAsignatura = estudiantesDeAsignatura(asignatura);
        if (estudiantesAsignatura.isEmpty()) {
            detalleAsignatura.add(new Paragraph(
                    "No hay estudiantes inscritos en esta asignatura."));
            return;
        }

        Grid<cuentaUsuario> grid = new Grid<>();
        grid.addClassName("siggo-grid");
        grid.setWidthFull();
        grid.addColumn(estudiante -> estudiante == null ? "" : estudiante.getNombre())
                .setHeader("Estudiante").setAutoWidth(true);

        for (int indice = 0; indice < materia.getActividades().size(); indice++) {
            final int numeroActividad = indice + 1;
            ActividadEvaluativa actividad = materia.getActividades().get(indice);
            grid.addColumn(estudiante -> textoNota(
                    estudiante, asignatura, numeroActividad))
                    .setHeader(actividad.getNombre() + " ("
                            + formato(actividad.getPorcentaje()) + "%)")
                    .setAutoWidth(true);
        }

        grid.addColumn(estudiante -> textoPromedio(estudiante, asignatura))
                .setHeader("Promedio actual").setAutoWidth(true);
        grid.addColumn(this::textoPromedioGeneral)
                .setHeader("Promedio general").setAutoWidth(true);
        grid.addComponentColumn(estudiante -> indicadorRiesgo(estudiante, asignatura))
                .setHeader("Estado").setAutoWidth(true);
        grid.addComponentColumn(estudiante -> botonesNotas(estudiante, asignatura))
                .setHeader("Acciones").setAutoWidth(true);
        grid.setItems(estudiantesAsignatura);

        Div contenedorGrid = new Div(grid);
        contenedorGrid.addClassName("grid-scroll");
        contenedorGrid.setWidthFull();

        busqueda.addValueChangeListener(event ->
                actualizarGrid(grid, estudiantesAsignatura, busqueda.getValue(),
                        filtroEstado.getValue(), asignatura));
        filtroEstado.addValueChangeListener(event ->
                actualizarGrid(grid, estudiantesAsignatura, busqueda.getValue(),
                        filtroEstado.getValue(), asignatura));

        try {
            detalleAsignatura.add(crearReporte(asignatura, estudiantesAsignatura),
                    filtros, contenedorGrid);
        } catch (IOException e) {
            Notification.show("No se pudieron leer las notas: " + e.getMessage());
            detalleAsignatura.add(filtros, contenedorGrid);
        }
    }

    private void abrirConfiguracionPlan(String asignatura) {
        List<ActividadEvaluativa> actividades;
        try {
            actividades = notasService.actividadesDeAsignatura(asignatura);
        } catch (IOException e) {
            Notification.show("No se pudo cargar el plan: " + e.getMessage());
            return;
        }

        Dialog dialogo = new Dialog();
        dialogo.setHeaderTitle("Plan de evaluación: " + asignatura);
        VerticalLayout filas = new VerticalLayout();
        List<TextField> nombres = new ArrayList<>();
        List<NumberField> porcentajes = new ArrayList<>();

        for (ActividadEvaluativa actividad : actividades) {
            agregarFilaActividad(filas, nombres, porcentajes,
                    actividad.getNombre(), actividad.getPorcentaje());
        }

        Button agregar = new Button("Agregar actividad",
                event -> agregarFilaActividad(filas, nombres, porcentajes, "", null));
        Button cancelar = new Button("Cancelar", event -> dialogo.close());
        Button guardar = new Button("Guardar plan", event -> {
            List<ActividadEvaluativa> nuevoPlan = new ArrayList<>();
            for (int indice = 0; indice < nombres.size(); indice++) {
                Double porcentaje = porcentajes.get(indice).getValue();
                if (porcentaje == null) {
                    Notification.show("Ingresa el porcentaje de cada actividad.");
                    return;
                }
                nuevoPlan.add(new ActividadEvaluativa(
                        nombres.get(indice).getValue(), porcentaje));
            }

            try {
                notasService.guardarPlanEvaluacion(asignatura, nuevoPlan);
                Notification.show("Plan de evaluación guardado.");
                dialogo.close();
                mostrarAsignatura(asignatura);
            } catch (IllegalArgumentException | IOException e) {
                Notification.show("No se pudo guardar el plan: " + e.getMessage());
            }
        });

        dialogo.add(filas, agregar);
        dialogo.getFooter().add(cancelar, guardar);
        dialogo.open();
    }

    private void agregarFilaActividad(VerticalLayout filas, List<TextField> nombres,
            List<NumberField> porcentajes, String nombreInicial, Double porcentajeInicial) {
        TextField nombre = new TextField("Actividad");
        nombre.setValue(nombreInicial);
        NumberField porcentaje = new NumberField("Porcentaje");
        porcentaje.setMin(0.1);
        porcentaje.setMax(100);
        porcentaje.setStep(0.1);
        if (porcentajeInicial == null) {
            porcentaje.clear();
        } else {
            porcentaje.setValue(porcentajeInicial);
        }

        HorizontalLayout fila = new HorizontalLayout(nombre, porcentaje);
        Button quitar = new Button("Quitar");
        quitar.addClickListener(event -> {
            filas.remove(fila);
            nombres.remove(nombre);
            porcentajes.remove(porcentaje);
        });
        fila.add(quitar);
        fila.setWidthFull();
        nombre.setWidthFull();
        porcentaje.setWidth("150px");

        nombres.add(nombre);
        porcentajes.add(porcentaje);
        filas.add(fila);
    }

    private void actualizarGrid(Grid<cuentaUsuario> grid, List<cuentaUsuario> estudiantesAsignatura,
            String textoBusqueda, String estadoSeleccionado, String asignatura) {
        String texto = textoBusqueda == null
                ? ""
                : textoBusqueda.trim().toLowerCase(Locale.ROOT);
        List<cuentaUsuario> resultados = new ArrayList<>();

        try {
            for (cuentaUsuario estudiante : estudiantesAsignatura) {
                String nombre = estudiante.getNombre() == null
                        ? ""
                        : estudiante.getNombre().toLowerCase(Locale.ROOT);

                if (!nombre.contains(texto)) {
                    continue;
                }

                Double promedio = notasService.promedioActual(
                        estudiante.getUsuario(), asignatura);
                if (coincideConEstado(promedio, estadoSeleccionado)) {
                    resultados.add(estudiante);
                }
            }
            grid.setItems(resultados);
        } catch (IOException e) {
            Notification.show("No se pudo filtrar la lista: " + e.getMessage());
        }
    }

    private List<cuentaUsuario> estudiantesDeAsignatura(String asignatura) {
        List<cuentaUsuario> resultado = new ArrayList<>();
        for (cuentaUsuario estudiante : estudiantes) {
            List<String> materiasEstudiante = estudiante.getObjetos();
            if (materiasEstudiante != null && materiasEstudiante.contains(asignatura)) {
                resultado.add(estudiante);
            }
        }
        return resultado;
    }

    private boolean coincideConEstado(Double promedio, String estado) {
        if ("Todos".equals(estado)) {
            return true;
        }
        if ("Sin notas".equals(estado)) {
            return promedio == null;
        }
        if ("Menor a 3.0".equals(estado)) {
            return promedio != null && promedio < 3.0;
        }
        if ("3.0 o más".equals(estado)) {
            return promedio != null && promedio >= 3.0;
        }
        return false;
    }

    private VerticalLayout crearReporte(
            String asignatura, List<cuentaUsuario> estudiantesAsignatura) throws IOException {
        VerticalLayout reporte = new VerticalLayout();
        reporte.addClassName("reporte-card");
        reporte.setPadding(true);
        reporte.setSpacing(false);
        reporte.getStyle()
                .set("border", "1px solid #dddddd")
                .set("border-radius", "8px");

        double suma = 0;
        double mayor = 0;
        double menor = 5;
        int cantidadConNotas = 0;
        int aprobados = 0;
        for (cuentaUsuario estudiante : estudiantesAsignatura) {
            Double promedio = notasService.promedioActual(estudiante.getUsuario(), asignatura);
            if (promedio != null) {
                suma += promedio;
                cantidadConNotas++;
                if (promedio > mayor) {
                    mayor = promedio;
                }
                if (promedio < menor) {
                    menor = promedio;
                }
                if (promedio >= 3.0) {
                    aprobados++;
                }
            }
        }

        if (cantidadConNotas == 0) {
            reporte.add(new H3("Reporte del grupo"),
                    new Paragraph("Aún no hay notas registradas."));
            return reporte;
        }

        double promedioGrupo = suma / cantidadConNotas;
        double porcentajeAprobados = aprobados / (double) cantidadConNotas;

        ProgressBar progreso = new ProgressBar(0, 1, porcentajeAprobados);
        progreso.setWidthFull();
        reporte.add(
                new H3("Reporte del grupo (promedios actuales)"),
                new Paragraph("Promedio del grupo: " + formato(promedioGrupo)),
                new Paragraph("Nota más alta: " + formato(mayor)),
                new Paragraph("Nota más baja: " + formato(menor)),
                new Paragraph("Estudiantes aprobados: "
                        + Math.round(porcentajeAprobados * 100) + "%"),
                progreso
        );
        return reporte;
    }

    private HorizontalLayout botonesNotas(cuentaUsuario estudiante, String asignatura) {
        Button editar = new Button("Ingresar / editar");
        editar.addClickListener(event -> abrirEditor(estudiante, asignatura));

        Button borrar = new Button("Borrar nota");
        borrar.addClickListener(event -> abrirConfirmacionBorrado(estudiante, asignatura));

        HorizontalLayout botones = new HorizontalLayout(editar, borrar);
        botones.addClassName("botones-nota");
        return botones;
    }

    private void abrirEditor(cuentaUsuario estudiante, String asignatura) {
        Dialog dialogo = new Dialog();
        dialogo.setHeaderTitle("Nota de " + estudiante.getNombre() + " - " + asignatura);

        ComboBox<Integer> periodo;
        try {
            periodo = crearSelectorActividad(asignatura);
        } catch (IOException e) {
            Notification.show("No se pudo cargar el plan: " + e.getMessage());
            return;
        }
        NumberField nota = crearCampoNota();
        cargarCampoNota(estudiante, asignatura, periodo.getValue(), nota);
        periodo.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                cargarCampoNota(estudiante, asignatura, event.getValue(), nota);
            }
        });

        Button guardar = new Button("Guardar nota", event -> {
            if (periodo.getValue() == null || nota.getValue() == null) {
                Notification.show("Selecciona una actividad e ingresa una nota.");
                return;
            }
            try {
                notasService.guardarNota(estudiante.getUsuario(), asignatura,
                        periodo.getValue(), nota.getValue());
                Notification.show("Nota guardada correctamente.");
                dialogo.close();
                mostrarAsignatura(asignatura);
            } catch (IllegalArgumentException | IOException e) {
                Notification.show("No se pudo guardar la nota: " + e.getMessage());
            }
        });
        Button cancelar = new Button("Cancelar", event -> dialogo.close());
        dialogo.add(periodo, nota);
        dialogo.getFooter().add(cancelar, guardar);
        dialogo.open();
    }

    private void abrirConfirmacionBorrado(cuentaUsuario estudiante, String asignatura) {
        Dialog dialogo = new Dialog();
        dialogo.setHeaderTitle("Confirmar borrado");

        ComboBox<Integer> periodo;
        List<ActividadEvaluativa> actividades;
        try {
            actividades = notasService.actividadesDeAsignatura(asignatura);
            periodo = crearSelectorActividad(asignatura);
        } catch (IOException e) {
            Notification.show("No se pudo cargar el plan: " + e.getMessage());
            return;
        }
        Paragraph confirmacion = new Paragraph("¿Seguro de borrar la nota de "
                + actividades.get(periodo.getValue() - 1).getNombre()
                + " de "
                + estudiante.getNombre() + "?");
        periodo.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                confirmacion.setText("¿Seguro de borrar la nota de "
                        + actividades.get(event.getValue() - 1).getNombre()
                        + " de " + estudiante.getNombre() + "?");
            }
        });

        Button cancelar = new Button("Cancelar", event -> dialogo.close());
        Button confirmar = new Button("Sí, borrar", event -> {
            try {
                boolean eliminada = notasService.eliminarNota(estudiante.getUsuario(),
                        asignatura, periodo.getValue());
                Notification.show(eliminada
                        ? "Nota borrada correctamente."
                        : "No había una nota guardada para esa actividad.");
                dialogo.close();
                mostrarAsignatura(asignatura);
            } catch (IOException | IllegalArgumentException e) {
                Notification.show("No se pudo borrar la nota: " + e.getMessage());
            }
        });
        dialogo.add(confirmacion, periodo);
        dialogo.getFooter().add(cancelar, confirmar);
        dialogo.open();
    }

    private ComboBox<Integer> crearSelectorActividad(String asignatura) throws IOException {
        List<ActividadEvaluativa> actividades = notasService.actividadesDeAsignatura(asignatura);
        List<Integer> numeros = new ArrayList<>();
        for (int indice = 0; indice < actividades.size(); indice++) {
            numeros.add(indice + 1);
        }

        ComboBox<Integer> selector = new ComboBox<>("Actividad");
        selector.setItems(numeros);
        selector.setItemLabelGenerator(numero -> {
            ActividadEvaluativa actividad = actividades.get(numero - 1);
            return actividad.getNombre() + " ("
                    + formato(actividad.getPorcentaje()) + "%)";
        });
        selector.setValue(1);
        selector.setRequiredIndicatorVisible(true);
        return selector;
    }

    private NumberField crearCampoNota() {
        NumberField nota = new NumberField("Nota (0.0 a 5.0)");
        nota.setMin(0.0);
        nota.setMax(5.0);
        nota.setStep(0.1);
        nota.setStepButtonsVisible(true);
        nota.setRequiredIndicatorVisible(true);
        return nota;
    }

    private void cargarCampoNota(cuentaUsuario estudiante, String asignatura,
            int periodo, NumberField campo) {
        try {
            Double valor = notasService.obtenerNota(
                    estudiante.getUsuario(), asignatura, periodo);
            if (valor == null) {
                campo.clear();
            } else {
                campo.setValue(valor);
            }
        } catch (IOException e) {
            campo.clear();
            Notification.show("No se pudo cargar la nota: " + e.getMessage());
        }
    }

    private String textoNota(cuentaUsuario estudiante, String asignatura, int periodo) {
        try {
            Double nota = notasService.obtenerNota(estudiante.getUsuario(), asignatura, periodo);
            return nota == null ? "Pendiente" : formato(nota);
        } catch (IOException e) {
            return "Error de lectura";
        }
    }

    private String textoPromedio(cuentaUsuario estudiante, String asignatura) {
        try {
            Double promedio = notasService.promedioActual(
                    estudiante.getUsuario(), asignatura);
            return promedio == null ? "Sin notas" : formato(promedio);
        } catch (IOException e) {
            return "Error de lectura";
        }
    }

    private String textoPromedioGeneral(cuentaUsuario estudiante) {
        double suma = 0;
        int materiasCalificadas = 0;
        try {
            List<String> materiasEstudiante = estudiante.getObjetos();
            if (materiasEstudiante == null) {
                return "Sin notas";
            }
            for (String asignatura : materiasEstudiante) {
                Double promedio = notasService.promedioActual(
                        estudiante.getUsuario(), asignatura);
                if (promedio != null) {
                    suma += promedio;
                    materiasCalificadas++;
                }
            }
            return materiasCalificadas == 0
                    ? "Sin notas"
                    : formato(suma / materiasCalificadas);
        } catch (IOException e) {
            return "Error de lectura";
        }
    }

    private Paragraph indicadorRiesgo(cuentaUsuario estudiante, String asignatura) {
        Paragraph estado = new Paragraph();
        try {
            Double promedio = notasService.promedioActual(
                    estudiante.getUsuario(), asignatura);
            if (promedio == null) {
                estado.setText("Sin notas");
            } else if (promedio < 3.0) {
                estado.setText("En riesgo");
                estado.getStyle()
                        .set("color", "#842029")
                        .set("background-color", "#f8d7da")
                        .set("padding", "4px 8px")
                        .set("border-radius", "8px")
                        .set("font-weight", "bold");
            } else {
                estado.setText("Al día");
            }
        } catch (IOException e) {
            estado.setText("Error de lectura");
        }
        return estado;
    }

    private static boolean esEstudiante(cuentaUsuario usuario) {
        String rol = usuario.getRole();
        return rol != null && ("ESTUDIANTE".equalsIgnoreCase(rol)
                || "ROLE_ESTUDIANTE".equalsIgnoreCase(rol));
    }

    private static boolean esProfesor(cuentaUsuario usuario) {
        String rol = usuario.getRole();
        return rol != null && ("PROFESOR".equalsIgnoreCase(rol)
                || "ROLE_PROFESOR".equalsIgnoreCase(rol));
    }

    private static String formato(double valor) {
        return String.format(java.util.Locale.forLanguageTag("es-CO"), "%.2f", valor);
    }
}
