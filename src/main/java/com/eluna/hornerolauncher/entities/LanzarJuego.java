package com.eluna.hornerolauncher.entities;

import com.eluna.hornerolauncher.GlobalConfig;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LanzarJuego extends Thread {
    private String script;

    public LanzarJuego(String rutaScript) {
        this.script = rutaScript;
    }

    public void lanzarJuego()
            throws Exception {
        Path scriptReal = Path.of(this.script);

        if (!Files.exists(scriptReal)) {
            throw new FileNotFoundException("No se encontró el script: " + scriptReal);
        }

        ProcessBuilder pb;
        if (GlobalConfig.so.contains("linux")) {
            pb = new ProcessBuilder("bash", scriptReal.toString());
        } else if (GlobalConfig.so.contains("win32")) {
            pb = new ProcessBuilder("cmd", "/c", scriptReal.toString());
        } else {
            throw new UnsupportedOperationException("Sistema operativo no compatible");
        }

        pb.directory(GlobalConfig.baseMCPath.toFile());
        pb.inheritIO();
        int codigo = pb.start().waitFor();
        IO.println("El juego terminó con código: "+codigo);
    }

    public void run() {
        try {
            lanzarJuego();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
