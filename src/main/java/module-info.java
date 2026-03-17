module dtu.services.library
{
    // Standard Libraries
    requires transitive java.sql;
    requires transitive jakarta.servlet;

    // Spring Core & Web
    requires transitive spring.web;
    requires transitive spring.boot;
    requires transitive spring.core;
    requires transitive spring.beans;
    requires transitive spring.webmvc;
    requires transitive spring.context;
    requires transitive spring.boot.autoconfigure;

    // Security
    requires spring.vault.core;
    requires transitive spring.security.web;
    requires transitive spring.security.core;
    requires transitive spring.security.config;
    requires transitive spring.security.crypto;
    requires transitive spring.security.oauth2.core;
    requires transitive spring.security.oauth2.jose;
    requires transitive spring.security.oauth2.resource.server;

    // Data & Messaging
    requires spring.kafka;
    requires transitive spring.tx;
    requires transitive spring.jdbc;
    requires transitive spring.boot.jdbc;
    requires transitive spring.integration.core;

    // Transformations
    requires transitive org.mapstruct;

    // Json
    requires transitive tools.jackson.core;
    requires transitive tools.jackson.databind;
    requires transitive tools.jackson.dataformat.yaml;
    requires transitive com.fasterxml.jackson.annotation;

    // Logging & Utils
    requires static lombok;
    requires static org.jspecify;
    requires transitive org.slf4j;
    requires transitive logstash.logback.encoder;

    // Package Visibility
    exports dtu.services.library.utils;
    exports dtu.services.library.metrics;
    exports dtu.services.library.config.events;


    // Reflection access for Spring and Jackson
    opens dtu.services.library.context to tools.jackson.databind;
    opens dtu.services.library.config.events to tools.jackson.databind;
}
