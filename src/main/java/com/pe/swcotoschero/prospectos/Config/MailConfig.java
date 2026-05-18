package com.pe.swcotoschero.prospectos.Config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuracion de correo electronico via Gmail con app password.
 * Solo se activa si mail.enabled=true (para no romper el arranque cuando no hay cuenta configurada).
 * Fase 1 implementara EmailService que usa este bean.
 *
 * TODO (Fase 1): el dueno debe tener 2FA activo en Gmail y generar una App Password
 * en https://myaccount.google.com/apppasswords. Luego configurar las variables de entorno.
 */
@Configuration
public class MailConfig {

    /**
     * JavaMailSender configurado por application.properties.
     * Spring Boot autoconfigura este bean si spring.mail.* esta presente;
     * esta clase queda como referencia documentada.
     * En Fase 1 se puede eliminar este bean manual y dejar solo las props.
     */
    @Bean
    @ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
    public JavaMailSender javaMailSender(
            org.springframework.boot.autoconfigure.mail.MailProperties props) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(props.getHost());
        mailSender.setPort(props.getPort());
        mailSender.setUsername(props.getUsername());
        mailSender.setPassword(props.getPassword());

        Properties javaMailProps = mailSender.getJavaMailProperties();
        javaMailProps.put("mail.transport.protocol", "smtp");
        javaMailProps.put("mail.smtp.auth", "true");
        javaMailProps.put("mail.smtp.starttls.enable", "true");
        // mail.debug=false en produccion; se puede activar via spring.mail.properties.mail.debug=true
        javaMailProps.put("mail.debug", "false");

        return mailSender;
    }
}
