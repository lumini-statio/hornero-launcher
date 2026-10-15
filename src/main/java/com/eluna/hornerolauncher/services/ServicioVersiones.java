package com.eluna.hornerolauncher.services;

import com.eluna.hornerolauncher.GlobalConfig;
import com.eluna.hornerolauncher.entities.DescargarVersion;
import com.eluna.hornerolauncher.utils.dto.VersionInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static java.lang.IO.println;

public class ServicioVersiones {
    public static Map<String, VersionInfo> encontrarVersionCoincidente(String versionBuscada)
            throws IOException {

        JsonObject data;
        try (Reader reader = Files.newBufferedReader(Path.of("/con/eluna/hornerolauncher/data/versiones.json"), StandardCharsets.UTF_8)) {
            data = JsonParser.parseReader(reader).getAsJsonObject();
        }

        Map<String, VersionInfo> parecidas = new HashMap<>();
        JsonArray versiones = data.getAsJsonArray("versions");

        for (JsonElement el : versiones) {
            JsonObject v = el.getAsJsonObject();
            String vId = v.get("id").getAsString();
            if (!vId.contains(versionBuscada)) continue;

            String tipo = v.get("type").getAsString();

            parecidas.put(vId, new VersionInfo(
                    vId, tipo, v.get("url").getAsString(), v.get("sha1").getAsString()));
        }
        return parecidas;
    }

    public static boolean descargarVersion(Map<String, String> version) {
        try {
            String versionName = version.get("version");
            Path jsonVersionPath = Path.of(GlobalConfig.so, "versions", versionName, versionName, versionName + ".json");
            DescargarVersion threadDescarga = new DescargarVersion(jsonVersionPath, version);
            threadDescarga.start();
            return true;
        } catch (Exception e) {
            println(new RuntimeException(e));
            return false;
        }
    }
}
