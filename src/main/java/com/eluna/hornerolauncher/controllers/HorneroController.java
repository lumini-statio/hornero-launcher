package com.eluna.hornerolauncher.controllers;

import com.eluna.hornerolauncher.services.ServicioJuego;
import com.eluna.hornerolauncher.services.ServicioVersiones;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.HashMap;
import java.util.Map;


public class HorneroController {
    private final ServicioJuego juego = new ServicioJuego();
    @FXML
    public Label welcomeText;

    @FXML
    public void initialize() {
        welcomeText.setText("Bienvenido, Player!");
    }

    @FXML
    public void ejecutarJuego() {
        String version = "/com/eluna/hornerolauncher/data/lanzador_1.21.11";
        juego.ejecutarJuego(version);
    }

    @FXML
    public void descargar() {
        Map<String,String> version = Map.of(
                "version", "26.3",
                "type", "release",
                "url", "https://piston-meta.mojang.com/v1/packages/702fe59163c6ee6578607daa85811d9bc9c7cc40/26.3.json",
                "sha1", "702fe59163c6ee6578607daa85811d9bc9c7cc40"
        );
        boolean descargaCompletada = ServicioVersiones.descargarVersion(version);
    }
}
