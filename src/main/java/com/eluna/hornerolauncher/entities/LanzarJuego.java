package com.eluna.hornerolauncher.entities;

import com.eluna.hornerolauncher.GlobalConfig;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

@AllArgsConstructor
public class LanzarJuego extends Thread {
    private String script;

    private Path rutaScript(String recurso) throws Exception {
        URL url = LanzarJuego.class.getResource(recurso);
        if (url == null) {
            throw new IOException("No se encontró el recurso: " + recurso);
        }
        return Path.of(url.toURI());
    }

    public void lanzarJuego()
            throws Exception {
        Path scriptReal = rutaScript(this.script);
        ProcessBuilder pb;
        if (GlobalConfig.so.contains("linux")) {
            pb = new ProcessBuilder("bash", scriptReal.toString());
        } else if (GlobalConfig.so.contains("win32")) {
            pb = new ProcessBuilder("cmd", "/c", scriptReal.toString());
        } else {
            throw new UnsupportedOperationException("Sistema operativo no compatible");
        }

        pb.inheritIO();
        int codigo = pb.start().waitFor();
    }

    @SneakyThrows
    public void run() {
        lanzarJuego();
    }
}
