package org.example.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AppApplication {

    public static void main(String[] args) {
        // Esto inicia el contenedor de Spring y el servidor Tomcat embebido
        SpringApplication.run(AppApplication.class, args);
        System.out.println("🚀 Servidor de Finanzas en Pareja levantado con éxito!");
        System.out.println("📖 Swagger UI: http://localhost:8080/swagger-ui.html");
    }
}