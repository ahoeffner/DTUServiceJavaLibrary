module dtu.services.library
{
    // Standard Libraries
    requires java.sql;
    requires jakarta.servlet;

    // Spring Core & Web
    requires transitive spring.web;
    requires transitive spring.boot;
    requires transitive spring.core;
    requires transitive spring.beans;
    requires transitive spring.webmvc;
    requires transitive spring.context;
    requires transitive spring.boot.autoconfigure;

    // Security
    requires transitive spring.security.web;
    requires transitive spring.security.core;
    requires transitive spring.security.config;
    requires transitive spring.security.crypto;
    requires transitive spring.security.oauth2.core;
    requires transitive spring.security.oauth2.jose;
    requires transitive spring.security.oauth2.resource.server;

    // Data & Messaging
    requires spring.tx;
    requires spring.jdbc;
    requires spring.kafka;
    requires spring.vault.core;
    requires spring.integration.core;

    // Json
    requires transitive tools.jackson.core;
    requires transitive tools.jackson.databind;
    requires transitive tools.jackson.dataformat.yaml;
    requires transitive com.fasterxml.jackson.annotation;

    // Logging & Utils
    requires org.slf4j;
    requires static org.jspecify;
    requires logstash.logback.encoder;

    // Package Visibility
    exports dtu.services.library.utils;

    // Reflection access for Spring and Jackson
    opens dtu.services.library.context to tools.jackson.databind;
    opens dtu.services.library.config.events to tools.jackson.databind;
}
