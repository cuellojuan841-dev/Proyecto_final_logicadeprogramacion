package com.example.notascurso;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class DashboardLayout extends AppLayout {

    public DashboardLayout() {
        DrawerToggle menuToggle = new DrawerToggle();
        Span titulo = new Span("Dashboard");
        addToNavbar(menuToggle, titulo);

        VerticalLayout menu = new VerticalLayout(
                new H3("Menú"),
                new Button("Menú principal"),
                new Button("Notas"),
                new Button("Contactos"),
                new Button("Salir")
        );
        menu.setPadding(true);
        menu.setSpacing(true);
        addToDrawer(menu);
    }
}
