package com.example.notascurso;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinSession;

public class DashboardLayout extends AppLayout {

    public DashboardLayout() {
        DrawerToggle menuToggle = new DrawerToggle();
        H3 titulo = new H3("Dashboard");
        titulo.getStyle().set("margin", "0");
        addToNavbar(menuToggle, titulo);

        Button notas = new Button("Notas");
        notas.addClickListener(event -> abrirVistaDeNotas());

        Button contactos = new Button("Contactos");
        contactos.addClickListener(event -> UI.getCurrent().navigate("contactos"));

        Button salir = new Button("Salir");
        salir.addClickListener(event -> cerrarSesion());

        VerticalLayout menu = new VerticalLayout(
                new H3("Menú"),
                notas,
                contactos,
                salir
        );

        menu.setPadding(true);
        menu.setSpacing(true);
        addToDrawer(menu);
    }

    private void cerrarSesion() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(cuentaUsuario.class, null);
        }
        UI.getCurrent().navigate("");
    }

    private void abrirVistaDeNotas() {
        cuentaUsuario usuario = new servicio().getUsuarioActual();
        if (usuario == null) {
            UI.getCurrent().navigate("");
            return;
        }

        String rol = usuario.getRole();
        if ("PROFESOR".equalsIgnoreCase(rol) || "ROLE_PROFESOR".equalsIgnoreCase(rol)) {
            UI.getCurrent().navigate("profesor");
        } else if ("ESTUDIANTE".equalsIgnoreCase(rol)
                || "ROLE_ESTUDIANTE".equalsIgnoreCase(rol)) {
            UI.getCurrent().navigate("estudiante");
        } else {
            Notification.show("El usuario no tiene una vista de notas asignada.");
        }
    }
}
