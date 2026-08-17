package cl.duoc.backendiii.semana1.intereses;

import cl.duoc.backendiii.semana1.JobCompletionNotificationListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Job 2 — Cálculo de intereses mensuales.
 *
 * calculoInteresesMensualesJob
 * ├── Step 1 crearTablaCuentasStep (tasklet): crea la tabla cuentas_saldo si no existe.
 * └── Step 2 aplicarInteresesStep (chunk): lee intereses.csv, aplica la tasa
 *     según tipo de cuenta y hace upsert del saldo final en cuentas_saldo (H2).
 */
@Configuration
public class InteresesMensualesJobConfig {

    @Bean
    public FlatFileItemReader<CuentaOrigen> interesesReader() {
        return new FlatFileItemReaderBuilder<CuentaOrigen>()
                .name("interesesReader")
                .resource(new ClassPathResource("input/intereses.csv"))
                .linesToSkip(1)
                .delimited()
                .delimiter(",")
                .names("cuentaId", "nombre", "saldo", "edad", "tipo")
                .fieldSetMapper(fs -> new CuentaOrigen(
                        fs.readString("cuentaId"),
                        fs.readString("nombre"),
                        fs.readBigDecimal("saldo"),
                        fs.readInt("edad"),
                        fs.readString("tipo")
                ))
                .build();
    }

    @Bean
    public InteresProcessor interesProcessor() {
        return new InteresProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<CuentaConInteres> interesesWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<CuentaConInteres>()
                .dataSource(dataSource)
                .sql("""
                        MERGE INTO cuentas_saldo (cuenta_id, nombre, tipo, saldo_anterior, tasa_aplicada, interes, saldo_final, fecha_actualizacion)
                        KEY (cuenta_id)
                        VALUES (:cuentaId, :nombre, :tipo, :saldoAnterior, :tasaAplicada, :interes, :saldoFinal, CURRENT_TIMESTAMP)
                        """)
                // No se usa BeanPropertySqlParameterSource porque los records no exponen
                // getters "getX" (siguen la convención de accessors de records), así que
                // se arma el MapSqlParameterSource a mano.
                .itemSqlParameterSourceProvider(item -> new MapSqlParameterSource()
                        .addValue("cuentaId", item.cuentaId())
                        .addValue("nombre", item.nombre())
                        .addValue("tipo", item.tipo())
                        .addValue("saldoAnterior", item.saldoAnterior())
                        .addValue("tasaAplicada", item.tasaAplicada())
                        .addValue("interes", item.interes())
                        .addValue("saldoFinal", item.saldoFinal()))
                .build();
    }

    @Bean
    public Step crearTablaCuentasStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JdbcTemplate jdbcTemplate
    ) {
        return new StepBuilder("crearTablaCuentasStep", jobRepository)
                .tasklet(new CrearTablaCuentasTasklet(jdbcTemplate), transactionManager)
                .build();
    }

    @Bean
    public Step aplicarInteresesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<CuentaOrigen> interesesReader,
            InteresProcessor interesProcessor,
            JdbcBatchItemWriter<CuentaConInteres> interesesWriter
    ) {
        return new StepBuilder("aplicarInteresesStep", jobRepository)
                .<CuentaOrigen, CuentaConInteres>chunk(5, transactionManager)
                .reader(interesesReader)
                .processor(interesProcessor)
                .writer(interesesWriter)
                .build();
    }

    @Bean
    public Job calculoInteresesMensualesJob(
            JobRepository jobRepository,
            Step crearTablaCuentasStep,
            Step aplicarInteresesStep
    ) {
        return new JobBuilder("calculoInteresesMensualesJob", jobRepository)
                .listener(new JobCompletionNotificationListener())
                .start(crearTablaCuentasStep)
                .next(aplicarInteresesStep)
                .build();
    }
}
