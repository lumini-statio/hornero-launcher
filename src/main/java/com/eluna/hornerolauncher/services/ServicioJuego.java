package com.eluna.hornerolauncher.services;

import com.eluna.hornerolauncher.entities.LanzarJuego;

public class ServicioJuego {
    public void ejecutarJuego(String version) {
        new LanzarJuego(version).start();
    }
}
