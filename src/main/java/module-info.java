module com.eluna.hornerolauncher {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome;
    requires org.kordamp.bootstrapfx.core;
    requires static lombok;
    requires com.google.gson;
    requires java.net.http;
    requires java.xml;
    
    opens com.eluna.hornerolauncher to javafx.fxml;
    exports com.eluna.hornerolauncher;
    exports com.eluna.hornerolauncher.controllers;
    exports com.eluna.hornerolauncher.services;
    exports com.eluna.hornerolauncher.entities;
    exports com.eluna.hornerolauncher.views;
    exports com.eluna.hornerolauncher.utils.dto;
}