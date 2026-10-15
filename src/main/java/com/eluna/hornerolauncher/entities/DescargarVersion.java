package com.eluna.hornerolauncher.entities;

import com.eluna.hornerolauncher.GlobalConfig;
import com.eluna.hornerolauncher.utils.dto.VersionInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class DescargarVersion extends Thread {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path jsonVersionPath;

    private final Map<String, String> version;

    public DescargarVersion(Path jsonVersionPath, Map<String, String> version) {
        this.jsonVersionPath = jsonVersionPath;
        this.version = version;
    }

    public String descargarVersion()
            throws Exception {

        HttpRequest reqJson = HttpRequest.newBuilder(URI.create(version.get("url"))).GET().build();
        HttpResponse<String> res = CLIENT.send(reqJson, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        JsonObject data = JsonParser.parseString(res.body()).getAsJsonObject();

        if (jsonVersionPath.getParent() != null) {
            Files.createDirectories(jsonVersionPath.getParent());
        }

        // se guardan los datos del json en el archivo de la misma versión <version>.json.
        Files.writeString(jsonVersionPath, GSON.toJson(data), StandardCharsets.UTF_8);

        // leer el json guardado y sacar la url del cliente
        JsonObject leido = JsonParser.parseString(Files.readString(jsonVersionPath, StandardCharsets.UTF_8))
                .getAsJsonObject();
        JsonObject downloads = leido.getAsJsonObject("downloads");
        String cliente = downloads.getAsJsonObject("client").get("url").getAsString();

        // descargar el sha, comparando el sha1
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");

        HttpRequest reqJar = HttpRequest.newBuilder(URI.create(cliente))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<InputStream> resJar = CLIENT.send(reqJar, HttpResponse.BodyHandlers.ofInputStream());

        if (resJar.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + resJar.statusCode() + " al descargar " + cliente);
        }

        String versionNumber = version.get("version");
        Path jar = Paths.get(String.valueOf(GlobalConfig.baseMCPath), "versions", versionNumber, versionNumber + ".jar");
        Files.createDirectories(jar.getParent());

        // descargar cliente jar
        try (InputStream in = resJar.body(); OutputStream out = Files.newOutputStream(jar)) {
            byte[] buffer = new byte[1024 * 64];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                sha1.update(buffer, 0, n);
            }
        }

        // devuelve sha1
        StringBuilder hex = new StringBuilder();
        for (byte b : sha1.digest()) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    @Override
    public void run() {
        try {
            descargarVersion();
        } catch (Exception e) {
            throw new RuntimeException("Error en la ejecución del programa: \n" + e);
        }
    }
}