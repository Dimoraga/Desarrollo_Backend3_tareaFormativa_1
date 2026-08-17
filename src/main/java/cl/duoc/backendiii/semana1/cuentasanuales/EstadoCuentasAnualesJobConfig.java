package cl.duoc.backendiii.semana1.cuentasanuales;

import cl.duoc.backendiii.semana1.JobCompletionNotificationListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Job 3 — Generación de estado de cuentas anuales.
 *
 * generarEstadoCuentasAnualesJob
 * └── Step compilarEstadoCuentasAnualesStep (tasklet): cruza data maestra
 *     (intereses.csv) con movimientos del año (cuentas_anuales.csv) y
 *     escribe output/estado_cuentas_anuales.csv para auditoría.
 */
@Configuration
public class EstadoCuentasAnualesJobConfig {

    @Bean
    public Step compilarEstadoCuentasAnualesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("compilarEstadoCuentasAnualesStep", jobRepository)
                .tasklet(new EstadoCuentasAnualesTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Job generarEstadoCuentasAnualesJob(
            JobRepository jobRepository,
            Step compilarEstadoCuentasAnualesStep
    ) {
        return new JobBuilder("generarEstadoCuentasAnualesJob", jobRepository)
                .listener(new JobCompletionNotificationListener())
                .start(compilarEstadoCuentasAnualesStep)
                .build();
    }
}
