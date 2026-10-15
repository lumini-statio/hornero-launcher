package com.eluna.hornerolauncher.controllers;

import com.eluna.hornerolauncher.GlobalConfig;
import com.eluna.hornerolauncher.entities.DescargarVersion;
import com.eluna.hornerolauncher.services.ConstructorComando;
import com.eluna.hornerolauncher.services.ServicioJuego;
import com.eluna.hornerolauncher.services.ServicioVersiones;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


public class HorneroController {
    private final ServicioJuego juego = new ServicioJuego();
    @FXML
    public Label welcomeText;
    @FXML
    public Button botonDescargar;
    public Button botonEjecutar;

    @FXML
    public void initialize() {
        welcomeText.setText("Bienvenido, Player!");
    }

    @FXML
    public void ejecutarJuego() {
        Path version = GlobalConfig.baseMCPath.resolve("versions").resolve("lanzador_26.3");
        juego.ejecutarJuego(version.toString());
    }

    @FXML
    public void descargar() {
        Map<String, String> version = Map.of(
                "version", "26.3",
                "type", "release",
                "url", "https://piston-meta.mojang.com/v1/packages/702fe59163c6ee6578607daa85811d9bc9c7cc40/26.3.json",
                "sha1", "702fe59163c6ee6578607daa85811d9bc9c7cc40"
        );
        String nombre = version.get("version");

        DescargarVersion descarga = ServicioVersiones.descargarVersion(version);

        botonDescargar.setDisable(true);
        welcomeText.setText("Descargando " + nombre + "...");

        // Paso 1 terminado -> arrancar paso 2
        descarga.setOnSucceeded(e -> {
            try {
                // el JSON ya existe (lo escribió la descarga); es un archivo chico
                JsonObject data = JsonParser
                        .parseString(Files.readString(ServicioVersiones.rutaJson(nombre), StandardCharsets.UTF_8))
                        .getAsJsonObject();

                String rutaLanzador = GlobalConfig.baseMCPath
                        .resolve("versions")
                        .resolve("lanzador_" + nombre).toString();

                ConstructorComando construir = new ConstructorComando(data, nombre, rutaLanzador);

                construir.setOnSucceeded(ev -> {
                    welcomeText.setText("Versión " + nombre + " lista");
                    botonDescargar.setDisable(false);
                });
                construir.setOnFailed(ev -> {
                    construir.getException().printStackTrace();
                    welcomeText.setText("Error al generar el ejecutable");
                    botonDescargar.setDisable(false);
                });

                welcomeText.setText("Generando ejecutable...");
                lanzarEnSegundoPlano(construir, "construir-" + nombre);
            } catch (Exception ex) {
                ex.printStackTrace();
                welcomeText.setText("Error: " + ex.getMessage());
                botonDescargar.setDisable(false);
            }
        });

        descarga.setOnFailed(e -> {
            descarga.getException().printStackTrace();
            welcomeText.setText("Error en la descarga: " + descarga.getException().getMessage());
            botonDescargar.setDisable(false);
        });

        lanzarEnSegundoPlano(descarga, "descarga-" + nombre);
    }

    private static void lanzarEnSegundoPlano(Task<?> tarea, String nombre) {
        Thread t = new Thread(tarea, nombre);
        t.setDaemon(true);
        t.start();
    }
}
