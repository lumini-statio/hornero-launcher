package com.eluna.hornerolauncher.controllers;

import com.eluna.hornerolauncher.services.ServicioJuego;
import com.eluna.hornerolauncher.services.ServicioVersiones;
import javafx.fxml.FXML;
import javafx.scene.control.Label;


public class HorneroController {
    private final ServicioJuego juego = new ServicioJuego();
    private final ServicioVersiones versiones = new ServicioVersiones();
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
    public void descargar() {}
}
