package com.pe.swcotoschero.prospectos.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuracion de correo (Gmail con App Password). El bean solo se crea si
 * app.mail.enabled=true (para no romper el arranque cuando no hay cuenta).
 *
 * Lee las propiedades directamente de spring.mail.* (que provienen de las
 * variables de entorno MAIL_HOST/MAIL_PORT/MAIL_USERNAME/MAIL_APP_PASSWORD).
 * NO depende del bean autoconfigurado MailProperties de Spring Boot: ese bean
 * solo existe si la autoconfig de mail se activa, y al declarar este @Bean
 * propio no estaba disponible -> el arranque fallaba con MAIL_ENABLED=true.
 */
@Configuration
public class MailConfig {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Bean
    @ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties javaMailProps = mailSender.getJavaMailProperties();
        javaMailProps.put("mail.transport.protocol", "smtp");
        javaMailProps.put("mail.smtp.auth", "true");
        javaMailProps.put("mail.smtp.starttls.enable", "true");
        javaMailProps.put("mail.debug", "false");

        return mailSender;
    }
}
