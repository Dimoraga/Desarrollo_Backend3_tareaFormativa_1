package cl.duoc.backendiii.semana1;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Con tres Jobs en el contexto (transacciones, intereses, cuentas anuales),
 * el runner automático de Spring Boot (JobLauncherApplicationRunner) no
 * sabe cuál ejecutar y falla. Este runner los lanza a mano, uno por uno,
 * en el orden lógico del negocio: primero el reporte diario, luego el
 * cálculo de intereses y por último el estado de cuentas anuales.
 *
 * Cada ejecución recibe un parámetro "runId" único (timestamp) para que
 * Spring Batch la trate como una JobInstance nueva.
 */
@Component
public class BatchJobRunner implements ApplicationRunner {

    private final JobLauncher jobLauncher;
    private final Job reporteTransaccionesDiariasJob;
    private final Job calculoInteresesMensualesJob;
    private final Job generarEstadoCuentasAnualesJob;

    public BatchJobRunner(
            JobLauncher jobLauncher,
            Job reporteTransaccionesDiariasJob,
            Job calculoInteresesMensualesJob,
            Job generarEstadoCuentasAnualesJob
    ) {
        this.jobLauncher = jobLauncher;
        this.reporteTransaccionesDiariasJob = reporteTransaccionesDiariasJob;
        this.calculoInteresesMensualesJob = calculoInteresesMensualesJob;
        this.generarEstadoCuentasAnualesJob = generarEstadoCuentasAnualesJob;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        jobLauncher.run(reporteTransaccionesDiariasJob, nuevosParametros());
        jobLauncher.run(calculoInteresesMensualesJob, nuevosParametros());
        jobLauncher.run(generarEstadoCuentasAnualesJob, nuevosParametros());
    }

    private JobParameters nuevosParametros() {
        return new JobParametersBuilder()
                .addLong("runId", System.currentTimeMillis())
                .toJobParameters();
    }
}
