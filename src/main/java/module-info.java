module org.konex.konex {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    requires org.mongodb.driver.sync.client;
    requires org.mongodb.bson;
    requires org.mongodb.driver.core;
    requires com.fasterxml.jackson.databind;

    opens org.konex to javafx.fxml;
    exports org.konex;
    exports org.konex.client;
    opens org.konex.client to javafx.fxml;
    exports org.konex.client.controller;
    opens org.konex.client.controller to javafx.fxml;
}