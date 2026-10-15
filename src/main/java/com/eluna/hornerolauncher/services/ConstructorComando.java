package com.eluna.hornerolauncher.services;
/**
 * Sinceramente la funcionalidad de armar el comando y crear el archivo ejecutable me daba
 * mucha paja, lo traducí con claude, basándome en lo que escribí en Python.
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.concurrent.Task;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.eluna.hornerolauncher.GlobalConfig.so;
import static com.eluna.hornerolauncher.GlobalConfig.baseMCPath;

public class ConstructorComando extends Task<String> {
    private JsonObject data;
    private String version;
    private String rutaLanzador;

    public ConstructorComando(JsonObject data, String version, String rutaLanzador) {
        this.data = data;
        this.version = version;
        this.rutaLanzador = rutaLanzador;
    }

    @Override
    protected String call() throws Exception {
        construirEjecutable(data, version, rutaLanzador);
        return rutaLanzador;
    }

    /** Equivalente a los argumentos con valor por defecto (kwargs) de Python. */
    public static class Opciones {
        public String username = "Player";
        public String uuid = "00000000-0000-0000-0000-000000000000";
        public String accessToken = "0";
        public String javaPath = null;
        public String osArch = "x86_64";
        public String osVersion = "";
        public Map<String, Boolean> features = new HashMap<>();
        public boolean incluirDefaultUserJvm = true;
        public String launcherName = "mi-launcher";
        public String launcherVersion = "1.0";
    }

    /** Contexto contra el que se evalúan las reglas (os / features). */
    private static final class Ctx {
        final String osName;
        final String osArch;
        final String osVersion;
        final Map<String, Boolean> features;

        Ctx(String osName, String osArch, String osVersion, Map<String, Boolean> features) {
            this.osName = osName;
            this.osArch = osArch;
            this.osVersion = osVersion;
            this.features = features;
        }
    }

    // ------------------------------------------------------------------
    // construir_comando
    // ------------------------------------------------------------------

    /**
     * osName: "linux" | "windows" | "osx"
     * Devuelve la lista de argumentos lista para ProcessBuilder.
     */
    public static List<String> construirComando(JsonObject data, String osName,
                                                Path gameDir, Path librariesDir,
                                                Path assetsDir, Path nativesDir,
                                                Path clientJar, Opciones op) {
        if (op == null) op = new Opciones();
        Map<String, Boolean> features = op.features != null ? op.features : new HashMap<>();
        Ctx ctx = new Ctx(osName, op.osArch, op.osVersion, features);

        // Classpath (solo librerías que aplican al SO + client.jar)
        String sep = osName.equals("windows") ? ";" : ":";
        List<String> libs = new ArrayList<>();
        for (JsonElement el : data.getAsJsonArray("libraries")) {
            JsonObject lib = el.getAsJsonObject();
            if (!permitido(reglasDe(lib), ctx)) continue;
            if (!lib.has("downloads")) continue;
            JsonObject downloads = lib.getAsJsonObject("downloads");
            if (!downloads.has("artifact")) continue;
            String path = downloads.getAsJsonObject("artifact").get("path").getAsString();
            libs.add(librariesDir.resolve(path).toString());
        }
        libs.add(clientJar.toString());
        String classpath = String.join(sep, libs);

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("auth_player_name", op.username);
        vars.put("version_name", data.get("id").getAsString());
        vars.put("game_directory", gameDir.toString());
        vars.put("assets_root", assetsDir.toString());
        vars.put("assets_index_name", data.getAsJsonObject("assetIndex").get("id").getAsString());
        vars.put("auth_uuid", op.uuid);
        vars.put("auth_access_token", op.accessToken);
        vars.put("clientid", "");
        vars.put("auth_xuid", "");
        vars.put("version_type", data.get("type").getAsString());
        vars.put("natives_directory", nativesDir.toString());
        vars.put("launcher_name", op.launcherName);
        vars.put("launcher_version", op.launcherVersion);
        vars.put("classpath", classpath);
        vars.put("path", ""); // ruta del log4j xml si lo descargaste

        JsonObject args = data.getAsJsonObject("arguments");
        List<String> jvmUsuario = new ArrayList<>();
        if (op.incluirDefaultUserJvm && args.has("default-user-jvm")) {
            jvmUsuario = expandir(args.getAsJsonArray("default-user-jvm"), ctx, vars);
        }
        List<String> jvm = expandir(args.getAsJsonArray("jvm"), ctx, vars);
        List<String> game = limpiarVacios(expandir(args.getAsJsonArray("game"), ctx, vars));

        String javaPath = op.javaPath;
        if (javaPath == null) {
            javaPath = osName.equals("windows") ? "java.exe" : "java";
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(javaPath);
        cmd.addAll(jvmUsuario);
        cmd.addAll(jvm);
        cmd.add(data.get("mainClass").getAsString());
        cmd.addAll(game);
        return cmd;
    }

    // ------------------------------------------------------------------
    // construir_ejecutable
    // ------------------------------------------------------------------

    /**
     * so: "windows" | "linux" | "osx" (también acepta "win32").
     * version: el id de la versión (en Python era version["version"]).
     * rutaLanzador: nombre del archivo a generar (en Windows se le agrega ".bat").
     */
    public static void construirEjecutable(JsonObject data,
                                           String version, String rutaLanzador) throws IOException {
        boolean esWindows = so.equals("windows") || so.contains("win32");
        String osName = esWindows ? "windows" : so;

        Opciones op = new Opciones();
        op.osVersion = esWindows ? "10.0.19045" : "";

        List<String> cmd = construirComando(
                data, osName,
                baseMCPath,
                baseMCPath.resolve("libraries"),
                baseMCPath.resolve("assets"),
                baseMCPath.resolve("natives"),
                baseMCPath.resolve("versions").resolve(version).resolve(version + ".jar"),
                op);

        System.out.println("\n=== " + so + " ===");
        Path carpeta = baseMCPath.resolve("versions");
        Path destino = carpeta.resolve(esWindows ? rutaLanzador + ".bat" : rutaLanzador);

        Files.createDirectories(carpeta);

        if (!Files.exists(destino)) {
            Files.createFile(destino);
        }

        if (esWindows) {
            Files.write(Paths.get(rutaLanzador + ".bat"),
                    listToCmdLine(cmd).getBytes(StandardCharsets.UTF_8));
        } else {
            Files.write(destino, shellJoin(cmd).getBytes(StandardCharsets.UTF_8));
            try {
                Set<PosixFilePermission> perms = Files.getPosixFilePermissions(destino);
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.GROUP_EXECUTE);
                perms.add(PosixFilePermission.OTHERS_EXECUTE);
                Files.setPosixFilePermissions(destino, perms);
            } catch (UnsupportedOperationException e) {
                // El sistema de archivos no es POSIX (ej. corriendo en Windows): se ignora.
            }
        }
    }

    // ------------------------------------------------------------------
    // Reglas (permitido)
    // ------------------------------------------------------------------

    private static JsonArray reglasDe(JsonObject obj) {
        return obj.has("rules") ? obj.getAsJsonArray("rules") : null;
    }

    /** Sin reglas = permitido. Con reglas: la última que aplica decide (por defecto, denegado). */
    private static boolean permitido(JsonArray rules, Ctx ctx) {
        if (rules == null || rules.size() == 0) return true;
        boolean allowed = false;
        for (JsonElement el : rules) {
            JsonObject rule = el.getAsJsonObject();
            if (reglaAplica(rule, ctx)) {
                allowed = "allow".equals(rule.get("action").getAsString());
            }
        }
        return allowed;
    }

    private static final Pattern DIGITOS = Pattern.compile("\\d+");

    /** Equivalente a _ver: extrae los números de la versión como tupla. */
    private static List<Long> ver(String v) {
        List<Long> out = new ArrayList<>();
        Matcher m = DIGITOS.matcher(v);
        while (m.find()) out.add(Long.parseLong(m.group()));
        return out;
    }

    /** Comparación lexicográfica, igual que la de tuplas en Python. */
    private static int comparar(List<Long> a, List<Long> b) {
        int n = Math.min(a.size(), b.size());
        for (int i = 0; i < n; i++) {
            int c = Long.compare(a.get(i), b.get(i));
            if (c != 0) return c;
        }
        return Integer.compare(a.size(), b.size());
    }

    private static boolean reglaAplica(JsonObject rule, Ctx ctx) {
        if (rule.has("os")) {
            JsonObject os = rule.getAsJsonObject("os");
            if (os.has("name") && !os.get("name").getAsString().equals(ctx.osName)) return false;
            if (os.has("arch") && !os.get("arch").getAsString().equals(ctx.osArch)) return false;
            if (os.has("version")
                    && !Pattern.compile(os.get("version").getAsString())
                    .matcher(ctx.osVersion).find()) {
                return false;
            }
            if (os.has("versionRange")) { // extensión de tu manifiesto
                JsonObject rng = os.getAsJsonObject("versionRange");
                List<Long> v = ver(ctx.osVersion.isEmpty() ? "0" : ctx.osVersion);
                if (rng.has("min") && comparar(v, ver(rng.get("min").getAsString())) < 0) {
                    return false;
                }
                if (rng.has("max") && comparar(v, ver(rng.get("max").getAsString())) >= 0) {
                    return false; // max exclusivo
                }
            }
        }
        if (rule.has("features")) {
            for (Map.Entry<String, JsonElement> e : rule.getAsJsonObject("features").entrySet()) {
                boolean esperado = e.getValue().getAsBoolean();
                boolean actual = ctx.features.getOrDefault(e.getKey(), false);
                if (esperado != actual) return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Expansión de argumentos
    // ------------------------------------------------------------------

    private static final Pattern VAR = Pattern.compile("\\$\\{(\\w+)\\}");

    private static String sustituir(String s, Map<String, String> vars) {
        Matcher m = VAR.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            // Igual que Python: si la variable no existe, se reemplaza por ""
            m.appendReplacement(sb, Matcher.quoteReplacement(vars.getOrDefault(m.group(1), "")));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Los elementos pueden ser un string o un objeto {rules, value} (value: string o lista). */
    private static List<String> expandir(JsonArray items, Ctx ctx, Map<String, String> vars) {
        List<String> out = new ArrayList<>();
        if (items == null) return out;
        for (JsonElement el : items) {
            if (el.isJsonPrimitive()) {
                out.add(sustituir(el.getAsString(), vars));
                continue;
            }
            JsonObject obj = el.getAsJsonObject();
            if (!permitido(reglasDe(obj), ctx)) continue;
            JsonElement value = obj.get("value");
            if (value == null) continue;
            if (value.isJsonArray()) {
                for (JsonElement v : value.getAsJsonArray()) {
                    out.add(sustituir(v.getAsString(), vars));
                }
            } else {
                out.add(sustituir(value.getAsString(), vars));
            }
        }
        return out;
    }

    /** Quita pares "--flag valor" cuando el valor quedó vacío (ej: --xuid, --clientId). */
    private static List<String> limpiarVacios(List<String> args) {
        List<String> res = new ArrayList<>();
        int i = 0;
        while (i < args.size()) {
            if (args.get(i).startsWith("--") && i + 1 < args.size() && args.get(i + 1).isEmpty()) {
                i += 2;
                continue;
            }
            res.add(args.get(i));
            i++;
        }
        return res;
    }

    // ------------------------------------------------------------------
    // Equivalentes de subprocess.list2cmdline y shlex.join
    // ------------------------------------------------------------------

    /** Port de subprocess.list2cmdline (reglas de quoting de MS C runtime). */
    public static String listToCmdLine(List<String> args) {
        StringBuilder result = new StringBuilder();
        boolean primero = true;
        for (String arg : args) {
            if (!primero) result.append(' ');
            primero = false;

            boolean needQuote = arg.contains(" ") || arg.contains("\t") || arg.isEmpty();
            if (needQuote) result.append('"');

            int backslashes = 0;
            for (char c : arg.toCharArray()) {
                if (c == '\\') {
                    backslashes++;
                } else if (c == '"') {
                    result.append("\\".repeat(backslashes * 2));
                    backslashes = 0;
                    result.append("\\\"");
                } else {
                    if (backslashes > 0) {
                        result.append("\\".repeat(backslashes));
                        backslashes = 0;
                    }
                    result.append(c);
                }
            }
            if (backslashes > 0) result.append("\\".repeat(backslashes));
            if (needQuote) {
                result.append("\\".repeat(backslashes));
                result.append('"');
            }
        }
        return result.toString();
    }

    private static final Pattern SEGURO = Pattern.compile("[a-zA-Z0-9_@%+=:,./-]+");

    private static String shellQuote(String s) {
        if (s.isEmpty()) return "''";
        if (SEGURO.matcher(s).matches()) return s;
        return "'" + s.replace("'", "'\"'\"'") + "'";
    }

    /** Equivalente a shlex.join. */
    public static String shellJoin(List<String> args) {
        List<String> q = new ArrayList<>();
        for (String a : args) q.add(shellQuote(a));
        return String.join(" ", q);
    }
}