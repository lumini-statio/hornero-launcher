package com.eluna.hornerolauncher;

import java.nio.file.Path;

public class GlobalConfig {
    public static String so = System.getProperty("os.name").toLowerCase();

    public static Path baseMCPath = so.contains("linux") ? Path.of(System.getProperty("user.home"),
            ".var", "app", "com.mojang.Minecraft", "data", "minecraft") :
            Path.of(System.getenv("APPDATA")).resolve(".minecraft");

    public static String dataPath = "/com/eluna/hornerolauncher/data/";
}
