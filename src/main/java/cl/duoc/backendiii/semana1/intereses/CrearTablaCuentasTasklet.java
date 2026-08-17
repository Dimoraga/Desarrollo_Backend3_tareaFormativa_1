package cl.duoc.backendiii.semana1.intereses;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tasklet: crea (si no existe) la tabla donde queda el saldo final tras
 * aplicar intereses. Se ejecuta como Step 1, antes del cálculo por chunk.
 */
public class CrearTablaCuentasTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    public CrearTablaCuentasTasklet(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS cuentas_saldo (
                    cuenta_id VARCHAR(20) PRIMARY KEY,
                    nombre VARCHAR(100),
                    tipo VARCHAR(20),
                    saldo_anterior DECIMAL(15,2),
                    tasa_aplicada DECIMAL(6,4),
                    interes DECIMAL(15,2),
                    saldo_final DECIMAL(15,2),
                    fecha_actualizacion TIMESTAMP
                )
                """);
        return RepeatStatus.FINISHED;
    }
}
