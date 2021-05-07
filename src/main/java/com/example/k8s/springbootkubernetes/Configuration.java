package com.example.k8s.springbootkubernetes;

import ch.qos.logback.access.tomcat.LogbackValve;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;

import javax.servlet.Filter;

@org.springframework.context.annotation.Configuration
public class Configuration {
    @Bean
    public TomcatServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        // put logback-access.xml in src/main/resources/conf
        tomcat.addContextValves(new LogbackValve());
        return tomcat;
    }

    @Bean(name = "TeeFilter")
    public Filter teeFilter() {
        return new ch.qos.logback.access.servlet.TeeFilter();
    }
}
