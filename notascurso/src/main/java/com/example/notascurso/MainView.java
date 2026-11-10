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
        // Esta es la pantalla inicial: preparo los campos y el botón del inicio de sesión.
        addClassName("login-view");
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 titulo = new H1("SIGGO UBP");
        titulo.addClassName("login-title");
        TextField usuarioField = new TextField("Usuario");
        PasswordField passwordField = new PasswordField("Contraseña");
        Button loginButton = new Button("Ingresar");
        loginButton.addClassName("primary-button");

        // Al hacer clic, tomo lo que se escribió y lo mando a validar.
        loginButton.addClickListener(event -> {
            String usuario = usuarioField.getValue();
            String password = passwordField.getValue();

            cuentaUsuario usuarioEncontrado;
            try {
                // El servicio busca la cuenta en usuarios.txt y guarda la sesión si coincide.
                usuarioEncontrado = authService.autenticar(usuario, password);
            } catch (IOException e) {
                // Si no se puede leer el archivo de usuarios, no intento continuar el ingreso.
                Notification.show("No se pudieron cargar los usuarios: " + e.getMessage());
                return;
            }

            // null significa que no hubo una fila con ese usuario y esa contraseña.
            if (usuarioEncontrado == null) {
                Notification.show("Usuario o contraseña incorrectos");
                return;
            }

            Notification.show("Bienvenido " + usuarioEncontrado.getNombre());

            String role = usuarioEncontrado.getRole();
            // Uso el rol de la cuenta autenticada para abrir la pantalla correcta.
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
