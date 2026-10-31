package com.example.notascurso;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "contactos", layout = DashboardLayout.class)
@PageTitle("Notas del curso | Contactos")
public class ContactosView extends VerticalLayout {

    public ContactosView() {
        add(
                new H2("Contactos"),
                new Paragraph("Esta sección estará disponible próximamente.")
        );
    }
}
