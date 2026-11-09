package com.example.notascurso;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.router.Route;

import java.io.IOException;

@Route("")
@StyleSheet("css/siggo.css")
public class MainView extends VerticalLayout {

    private final servicio authService = new servicio();

    public MainView() {
        addClassName("login-view");
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 titulo = new H1("SIGGO UBP");
        titulo.addClassName("login-title");
        TextField usuarioField = new TextField("Usuario");
        PasswordField passwordField = new PasswordField("Contraseña");
        Button loginButton = new Button("Ingresar");
        loginButton.addClassName("primary-button");

        loginButton.addClickListener(event -> {
            String usuario = usuarioField.getValue();
            String password = passwordField.getValue();

            cuentaUsuario usuarioEncontrado;
            try {
                usuarioEncontrado = authService.autenticar(usuario, password);
            } catch (IOException e) {
                Notification.show("No se pudieron cargar los usuarios: " + e.getMessage());
                return;
            }

            if (usuarioEncontrado == null) {
                Notification.show("Usuario o contraseña incorrectos");
                return;
            }

            Notification.show("Bienvenido " + usuarioEncontrado.getNombre());

            String role = usuarioEncontrado.getRole();
            if ("PROFESOR".equalsIgnoreCase(role) || "ROLE_PROFESOR".equalsIgnoreCase(role)) {
                UI.getCurrent().navigate("profesor");
            } else if ("ESTUDIANTE".equalsIgnoreCase(role) || "ROLE_ESTUDIANTE".equalsIgnoreCase(role)) {
                UI.getCurrent().navigate("estudiante");
            } else {
                Notification.show("El rol de este usuario no tiene una vista asignada.");
            }
        });

        add(titulo, usuarioField, passwordField, loginButton);
    }
}
