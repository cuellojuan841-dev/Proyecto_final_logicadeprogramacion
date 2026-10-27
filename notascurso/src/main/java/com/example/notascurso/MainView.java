package com.example.notascurso;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("")
public class MainView extends VerticalLayout {

    private final servicio authService = new servicio();

    public MainView() {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 titulo = new H1("Iniciar sesión");
        TextField usuarioField = new TextField("Usuario");
        PasswordField passwordField = new PasswordField("Contraseña");
        Button loginButton = new Button("Ingresar");

        loginButton.addClickListener(event -> {
            String usuario = usuarioField.getValue();
            String password = passwordField.getValue();

            cuentaUsuario usuarioEncontrado = authService.autenticar(usuario, password);

            if (usuarioEncontrado == null) {
                Notification.show("Usuario o contraseña incorrectos");
                return;
            }

            Notification.show("Bienvenido " + usuarioEncontrado.getNombre() + " (" + usuarioEncontrado.getRole() + ")");
        });

        add(titulo, usuarioField, passwordField, loginButton);
    }
}
