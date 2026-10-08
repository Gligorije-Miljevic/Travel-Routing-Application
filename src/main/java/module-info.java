module com.example.projekat {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;

    requires java.sql;
    requires gs.core;
    requires gs.ui.javafx;
    requires gs.ui.swing;
    requires java.desktop;

    opens com.example.projekat to javafx.fxml;
    opens com.example.projekat.country to javafx.fxml;
    exports com.example.projekat;
    exports com.example.projekat.country;
    exports com.example.projekat.algorithm;
}