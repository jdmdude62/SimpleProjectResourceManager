module com.subliminalsearch.simpleprojectresourcemanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.kordamp.bootstrapfx.core;
    
    requires java.sql;
    requires java.desktop;
    requires java.logging;
    requires java.prefs;
    
    requires org.slf4j;
    requires ch.qos.logback.classic;
    
    requires com.zaxxer.hikari;
    requires org.xerial.sqlitejdbc;
    
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    
    requires org.yaml.snakeyaml;
    
    requires org.apache.pdfbox;
    requires org.jfree.jfreechart;
    requires jakarta.mail;

    opens com.subliminalsearch.simpleprojectresourcemanager to javafx.fxml;
    opens com.subliminalsearch.simpleprojectresourcemanager.controller to javafx.fxml;
    opens com.subliminalsearch.simpleprojectresourcemanager.model to com.fasterxml.jackson.databind;
    
    exports com.subliminalsearch.simpleprojectresourcemanager;
    exports com.subliminalsearch.simpleprojectresourcemanager.model;
}