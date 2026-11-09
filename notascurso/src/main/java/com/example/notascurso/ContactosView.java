package com.example.notascurso;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Route(value = "contactos", layout = DashboardLayout.class)
@PageTitle("Notas del curso | Contactos")
public class ContactosView extends VerticalLayout {

    servicio servicioUsuarios = new servicio();
    cuentaUsuario usuarioActual;
    List<cuentaUsuario> usuarios = new ArrayList<>();
    ComboBox<cuentaUsuario> selector = new ComboBox<>("Selecciona un contacto");
    TextArea campoMensaje = new TextArea("Escribe tu mensaje");
    VerticalLayout conversacion = new VerticalLayout();
    Path archivo = Path.of("mensajes.txt");

    public ContactosView() {
        usuarioActual = servicioUsuarios.getUsuarioActual();
        if (usuarioActual == null) {
            UI.getCurrent().navigate("");
            return;
        }

        setWidthFull();
        setPadding(true);
        campoMensaje.setWidthFull();
        conversacion.setWidthFull();

        try {
            usuarios = servicioUsuarios.cargarUsuarios();
            configurarContactos();
        } catch (IOException e) {
            Notification.show("No se pudieron cargar los contactos: " + e.getMessage());
        }

        Button actualizar = new Button("Actualizar mensajes");
        actualizar.addClickListener(event -> cargarConversacion());

        Button enviar = new Button("Enviar mensaje");
        enviar.addClickListener(event -> enviarMensaje());

        selector.addValueChangeListener(event -> cargarConversacion());

        add(new H2("Contactos"), selector, actualizar, conversacion, campoMensaje, enviar);
        cargarConversacion();
    }

    void configurarContactos() {
        List<cuentaUsuario> contactos = new ArrayList<>();
        for (cuentaUsuario usuario : usuarios) {
            if (!usuario.getUsuario().equals(usuarioActual.getUsuario())
                    && esEstudiante(usuarioActual) && esProfesor(usuario)) {
                contactos.add(usuario);
            } else if (!usuario.getUsuario().equals(usuarioActual.getUsuario())
                    && esProfesor(usuarioActual) && esEstudiante(usuario)) {
                contactos.add(usuario);
            }
        }
        selector.setItems(contactos);
        selector.setItemLabelGenerator(usuario ->
                usuario == null ? "" : usuario.getNombre());
    }

    void cargarConversacion() {
        conversacion.removeAll();
        cuentaUsuario contacto = selector.getValue();

        if (contacto == null) {
            conversacion.add(new Paragraph("Selecciona un contacto para ver los mensajes."));
            return;
        }

        if (!Files.exists(archivo)) {
            conversacion.add(new Paragraph("Todavía no hay mensajes. Puedes escribir el primero."));
            return;
        }

        try {
            List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
            boolean hayMensajes = false;

            for (int indice = 0; indice < lineas.size(); indice++) {
                String linea = lineas.get(indice);
                if (linea.isBlank()) {
                    continue;
                }

                String[] datos = linea.split("\\|", 3);
                if (datos.length != 3) {
                    Notification.show("Formato inválido en mensajes.txt, línea "
                            + (indice + 1));
                    return;
                }

                if (esDeEstaConversacion(datos, contacto)) {
                    conversacion.add(new Paragraph(datos[0] + ": " + datos[2]));
                    hayMensajes = true;
                }
            }

            if (!hayMensajes) {
                conversacion.add(new Paragraph("Todavía no hay mensajes con este contacto."));
            }
        } catch (IOException e) {
            Notification.show("No se pudieron leer los mensajes: " + e.getMessage());
        }
    }

    boolean esDeEstaConversacion(String[] datos, cuentaUsuario contacto) {
        String usuario = usuarioActual.getUsuario();
        String otroUsuario = contacto.getUsuario();

        return (datos[0].equals(usuario) && datos[1].equals(otroUsuario))
                || (datos[0].equals(otroUsuario) && datos[1].equals(usuario));
    }

    void enviarMensaje() {
        cuentaUsuario contacto = selector.getValue();
        String texto = campoMensaje.getValue();

        if (contacto == null) {
            Notification.show("Selecciona un contacto primero.");
            return;
        }

        if (texto == null || texto.isBlank()) {
            Notification.show("Escribe un mensaje antes de enviarlo.");
            return;
        }

        if (texto.contains("|") || texto.contains("\n") || texto.contains("\r")) {
            Notification.show("El mensaje no puede contener | ni saltos de línea.");
            return;
        }

        String linea = usuarioActual.getUsuario() + "|" + contacto.getUsuario()
                + "|" + texto.trim() + System.lineSeparator();

        try {
            Files.writeString(archivo, linea, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            campoMensaje.clear();
            cargarConversacion();
            Notification.show("Mensaje enviado y guardado.");
        } catch (IOException e) {
            Notification.show("No se pudo guardar el mensaje: " + e.getMessage());
        }
    }

    boolean esProfesor(cuentaUsuario usuario) {
        String rol = usuario.getRole();
        return "PROFESOR".equalsIgnoreCase(rol)
                || "ROLE_PROFESOR".equalsIgnoreCase(rol);
    }

    boolean esEstudiante(cuentaUsuario usuario) {
        String rol = usuario.getRole();
        return "ESTUDIANTE".equalsIgnoreCase(rol)
                || "ROLE_ESTUDIANTE".equalsIgnoreCase(rol);
    }
}
