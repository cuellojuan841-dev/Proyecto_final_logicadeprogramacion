package com.example.notascurso;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
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
@StyleSheet("css/siggo.css")
public class ContactosView extends VerticalLayout {

    servicio servicioUsuarios = new servicio();
    cuentaUsuario usuarioActual;
    List<cuentaUsuario> usuarios = new ArrayList<>();
    ComboBox<cuentaUsuario> selector = new ComboBox<>("Selecciona un contacto");
    TextArea campoMensaje = new TextArea("Escribe tu mensaje");
    VerticalLayout conversacion = new VerticalLayout();
    Path archivo = Path.of("mensajes.txt");

    public ContactosView() {
        // La vista usa la sesión para saber quién envía y recibe los mensajes.
        addClassName("contactos-view");
        usuarioActual = servicioUsuarios.getUsuarioActual();
        // Sin una cuenta en sesión no se puede abrir la sección de mensajes.
        if (usuarioActual == null) {
            UI.getCurrent().navigate("");
            return;
        }

        setWidthFull();
        setPadding(true);
        campoMensaje.setWidthFull();
        conversacion.setWidthFull();

        try {
            // Cargo usuarios para armar la lista de contactos permitidos.
            usuarios = servicioUsuarios.cargarUsuarios();
            configurarContactos();
        } catch (IOException e) {
            // Informo el problema sin tumbar la vista completa.
            Notification.show("No se pudieron cargar los contactos: " + e.getMessage());
        }

        Button actualizar = new Button("Actualizar mensajes");
        actualizar.addClassName("secondary-button");
        actualizar.addClickListener(event -> cargarConversacion());

        Button enviar = new Button("Enviar mensaje");
        enviar.addClassName("primary-button");
        enviar.addClickListener(event -> enviarMensaje());

        selector.addValueChangeListener(event -> cargarConversacion());
        selector.addClassName("contactos-selector");
        campoMensaje.addClassName("mensaje-field");
        conversacion.addClassName("conversacion-card");

        add(new H2("Contactos"), selector, actualizar, conversacion, campoMensaje, enviar);
        cargarConversacion();
    }

    void configurarContactos() {
        // Solo permito conversaciones entre profesor y estudiante, excluyendo al usuario actual.
        List<cuentaUsuario> contactos = new ArrayList<>();
        // Recorro usuarios y solo agrego el otro rol, nunca a la cuenta actual.
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
        // Limpio el panel antes de dibujar de nuevo los mensajes del contacto seleccionado.
        conversacion.removeAll();
        cuentaUsuario contacto = selector.getValue();

        // Si todavía no eligieron a quién escribir, muestro una indicación y salgo.
        if (contacto == null) {
            conversacion.add(new Paragraph("Selecciona un contacto para ver los mensajes."));
            return;
        }

        // El archivo puede no existir hasta que se envía el primer mensaje.
        if (!Files.exists(archivo)) {
            conversacion.add(new Paragraph("Todavía no hay mensajes. Puedes escribir el primero."));
            return;
        }

        try {
            List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
            boolean hayMensajes = false;

            // Reviso cada mensaje y muestro únicamente los de esta conversación.
            for (int indice = 0; indice < lineas.size(); indice++) {
                String linea = lineas.get(indice);
                if (linea.isBlank()) {
                    continue;
                }

                String[] datos = linea.split("\\|", 3);
                // Si la línea no tiene emisor, receptor y texto, aviso dónde está el error.
                if (datos.length != 3) {
                    Notification.show("Formato inválido en mensajes.txt, línea "
                            + (indice + 1));
                    return;
                }

                // La misma conversación incluye mensajes enviados y recibidos.
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
        // datos contiene emisor, receptor y texto; comparo ambos sentidos de la conversación.
        String usuario = usuarioActual.getUsuario();
        String otroUsuario = contacto.getUsuario();

        return (datos[0].equals(usuario) && datos[1].equals(otroUsuario))
                || (datos[0].equals(otroUsuario) && datos[1].equals(usuario));
    }

    void enviarMensaje() {
        // Valido lo ingresado antes de convertirlo en una línea del archivo de mensajes.
        cuentaUsuario contacto = selector.getValue();
        String texto = campoMensaje.getValue();

        // Primero verifico que haya destinatario y texto para guardar.
        if (contacto == null) {
            Notification.show("Selecciona un contacto primero.");
            return;
        }

        if (texto == null || texto.isBlank()) {
            Notification.show("Escribe un mensaje antes de enviarlo.");
            return;
        }

        // No permito separadores ni saltos porque cambiarían la estructura de la fila.
        if (texto.contains("|") || texto.contains("\n") || texto.contains("\r")) {
            Notification.show("El mensaje no puede contener | ni saltos de línea.");
            return;
        }

        String linea = usuarioActual.getUsuario() + "|" + contacto.getUsuario()
                + "|" + texto.trim() + System.lineSeparator();

        try {
            // Creo el archivo si falta y agrego el mensaje al final sin borrar los anteriores.
            Files.writeString(archivo, linea, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            campoMensaje.clear();
            cargarConversacion();
            Notification.show("Mensaje enviado y guardado.");
        } catch (IOException e) {
            // Si no se pudo escribir el archivo, se lo comunico a quien envió el mensaje.
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
