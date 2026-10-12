package com.eluna.hornerolauncher.services;

import com.eluna.hornerolauncher.entities.LanzarJuego;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ServicioJuego {
    private ServicioVersiones servicioVersiones;
    public void ejecutarJuego(String version) {
        new LanzarJuego(version).start();
    }
}
