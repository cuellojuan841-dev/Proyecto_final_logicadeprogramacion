package com.example.notascurso;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinSession;

@StyleSheet("css/siggo.css")
public class DashboardLayout extends AppLayout {

    public DashboardLayout() {
        // AppLayout deja la barra superior y el menú lateral compartidos entre las vistas.
        addClassName("siggo-layout");
        DrawerToggle menuToggle = new DrawerToggle();
        H3 titulo = new H3("SIGGO UBP");
        titulo.addClassName("siggo-brand");
        titulo.getStyle().set("margin", "0");
        // DrawerToggle abre o cierra las opciones que agrego al cajón lateral.
        addToNavbar(menuToggle, titulo);

        // Este botón manda a la pantalla de notas que le corresponde al usuario por su rol.
        Button notas = new Button("Notas");
        notas.addClassName("nav-button");
        notas.addClickListener(event -> abrirVistaDeNotas());

        // Contactos es una ruta común para profesores y estudiantes.
        Button contactos = new Button("Contactos");
        contactos.addClassName("nav-button");
        contactos.addClickListener(event -> UI.getCurrent().navigate("contactos"));

        // Al salir, se borra la sesión actual para que la cuenta deje de estar autenticada.
        Button salir = new Button("Salir");
        salir.addClassName("nav-button");
        salir.addClickListener(event -> cerrarSesion());

        // Agrupo las opciones en una columna que aparece dentro del menú desplegable.
        VerticalLayout menu = new VerticalLayout(
                new H3("Menú"),
                notas,
                contactos,
                salir
        );

        menu.addClassName("siggo-menu");
        menu.setPadding(true);
        menu.setSpacing(true);
        addToDrawer(menu);
    }

    private void cerrarSesion() {
        // VaadinSession conserva la cuenta actual mientras se mueve entre rutas.
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            // Al quitar la cuenta, las vistas protegidas ya no la encontrarán en la sesión.
            session.setAttribute(cuentaUsuario.class, null);
        }
        // Vuelvo a la ruta inicial, donde se puede iniciar sesión otra vez.
        UI.getCurrent().navigate("");
    }

    private void abrirVistaDeNotas() {
        // El servicio obtiene de la sesión la cuenta que se autenticó en MainView.
        cuentaUsuario usuario = new servicio().getUsuarioActual();
        if (usuario == null) {
            // Si no hay cuenta guardada, no puedo decidir qué pantalla de notas abrir.
            UI.getCurrent().navigate("");
            return;
        }

        // El rol determina cuál de las dos rutas de notas corresponde a esta cuenta.
        String rolUsuario = usuario.getRole();
        if ("PROFESOR".equalsIgnoreCase(rolUsuario)
                || "ROLE_PROFESOR".equalsIgnoreCase(rolUsuario)) {
            // A los profesores se les abre la vista para gestionar notas.
            UI.getCurrent().navigate("profesor");
        } else if ("ESTUDIANTE".equalsIgnoreCase(rolUsuario)
                || "ROLE_ESTUDIANTE".equalsIgnoreCase(rolUsuario)) {
            // A los estudiantes se les muestra su historial académico.
            UI.getCurrent().navigate("estudiante");
        } else {
            // Si el rol no es uno de los conocidos, no hay una ruta segura que abrir.
            Notification.show("El usuario no tiene una vista de notas asignada.");
        }
    }
}
