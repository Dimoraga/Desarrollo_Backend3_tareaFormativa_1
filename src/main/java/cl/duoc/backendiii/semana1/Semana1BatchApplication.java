package cl.duoc.backendiii.semana1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación Spring Boot.
 *
 * Cuando se ejecuta esta clase, Spring Boot:
 * 1. Levanta el contexto de aplicación.
 * 2. Detecta la configuración de Spring Batch.
 * 3. Crea los beans declarados en BatchConfiguration.
 * 4. Inicializa la base H2 usada por JobRepository.
 * 5. Ejecuta automáticamente el Job porque spring.batch.job.enabled=true.
 *
 * Para la clase, esta es la forma simple de demostrar que el batch se puede
 * ejecutar como una aplicación Java normal.
 */
@SpringBootApplication
public class Semana1BatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(Semana1BatchApplication.class, args);
    }
}
