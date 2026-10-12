package com.eluna.hornerolauncher.services;

import com.eluna.hornerolauncher.utils.dto.VersionInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.xml.stream.events.EntityReference;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServicioVersiones {
    public static Map<String, VersionInfo> encontrarVersionCoincidente(String versionBuscada)
            throws IOException {

        JsonObject data;
        try (Reader reader = Files.newBufferedReader(Path.of("/con/eluna/hornerolauncher/data/versiones.json"), StandardCharsets.UTF_8)) {
            data = JsonParser.parseReader(reader).getAsJsonObject();
        }

        Map<String, VersionInfo> parecidas = new LinkedHashMap<>();
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
}
