package cl.duoc.backendiii.semana1.transacciones;

import cl.duoc.backendiii.semana1.JobCompletionNotificationListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

/**
 * Job 1 — Reporte de transacciones diarias.
 *
 * reporteTransaccionesDiariasJob
 * ├── Step 1 procesarTransaccionesStep (chunk): lee transacciones.csv,
 * │   detecta anomalías por registro y escribe el detalle en
 * │   output/reporte_transacciones_diarias.csv
 * └── Step 2 resumenTransaccionesStep (tasklet): con las estadísticas
 *     acumuladas en el Step 1, escribe output/resumen_transacciones_diarias.txt
 */
@Configuration
public class TransaccionesDiariasJobConfig {

    @Bean
    public FlatFileItemReader<Transaccion> transaccionesReader() {
        return new FlatFileItemReaderBuilder<Transaccion>()
                .name("transaccionesReader")
                .resource(new ClassPathResource("input/transacciones.csv"))
                .linesToSkip(1)
                .delimited()
                .delimiter(",")
                .names("id", "fecha", "monto", "tipo")
                .fieldSetMapper(fs -> new Transaccion(
                        fs.readLong("id"),
                        LocalDate.parse(fs.readString("fecha")),
                        fs.readBigDecimal("monto"),
                        fs.readString("tipo")
                ))
                .build();
    }

    @Bean
    public TransaccionAnomaliaProcessor transaccionAnomaliaProcessor() {
        return new TransaccionAnomaliaProcessor();
    }

    @Bean
    public FlatFileItemWriter<TransaccionProcesada> transaccionesWriter() {
        return new FlatFileItemWriterBuilder<TransaccionProcesada>()
                .name("transaccionesWriter")
                .resource(new FileSystemResource("output/reporte_transacciones_diarias.csv"))
                .headerCallback(writer -> writer.write("id;fecha;monto;tipo;anomalia;detalle"))
                .lineAggregator(t -> String.join(";",
                        String.valueOf(t.id()),
                        t.fecha().toString(),
                        t.monto().toPlainString(),
                        t.tipo(),
                        t.anomalia() ? "SI" : "NO",
                        t.detalle()
                ))
                .build();
    }

    @Bean
    public Step procesarTransaccionesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<Transaccion> transaccionesReader,
            TransaccionAnomaliaProcessor transaccionAnomaliaProcessor,
            FlatFileItemWriter<TransaccionProcesada> transaccionesWriter
    ) {
        return new StepBuilder("procesarTransaccionesStep", jobRepository)
                .<Transaccion, TransaccionProcesada>chunk(5, transactionManager)
                .reader(transaccionesReader)
                .processor(transaccionAnomaliaProcessor)
                .writer(transaccionesWriter)
                .build();
    }

    @Bean
    public Step resumenTransaccionesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            TransaccionAnomaliaProcessor transaccionAnomaliaProcessor
    ) {
        return new StepBuilder("resumenTransaccionesStep", jobRepository)
                .tasklet(new ResumenTransaccionesTasklet(transaccionAnomaliaProcessor), transactionManager)
                .build();
    }

    @Bean
    public Job reporteTransaccionesDiariasJob(
            JobRepository jobRepository,
            Step procesarTransaccionesStep,
            Step resumenTransaccionesStep
    ) {
        return new JobBuilder("reporteTransaccionesDiariasJob", jobRepository)
                .listener(new JobCompletionNotificationListener())
                .start(procesarTransaccionesStep)
                .next(resumenTransaccionesStep)
                .build();
    }
}
