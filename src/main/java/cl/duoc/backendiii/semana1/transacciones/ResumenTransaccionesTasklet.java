package cl.duoc.backendiii.semana1.transacciones;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tasklet: genera el resumen del día a partir de las estadísticas acumuladas
 * por TransaccionAnomaliaProcessor durante el step anterior.
 *
 * Se apoya en que el Job ejecuta los steps de forma secuencial: cuando este
 * tasklet corre, procesarTransaccionesStep ya terminó y el processor
 * (bean singleton) tiene los contadores finales.
 */
public class ResumenTransaccionesTasklet implements Tasklet {

    private final TransaccionAnomaliaProcessor processor;

    public ResumenTransaccionesTasklet(TransaccionAnomaliaProcessor processor) {
        this.processor = processor;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws IOException {
        int total = processor.getTotalProcesadas();
        int anomalias = processor.getTotalAnomalias();
        BigDecimal montoTotal = processor.getMontoTotal();
        BigDecimal promedio = total == 0 ? BigDecimal.ZERO
                : montoTotal.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        String resumen = """
                RESUMEN DE TRANSACCIONES DIARIAS
                Transacciones procesadas: %d
                Anomalías detectadas: %d
                Monto total: %s
                Monto promedio: %s
                """.formatted(total, anomalias, montoTotal, promedio);

        Files.createDirectories(Path.of("output"));
        try (Writer writer = Files.newBufferedWriter(Path.of("output/resumen_transacciones_diarias.txt"),
                StandardCharsets.UTF_8)) {
            writer.write(resumen);
        }

        return RepeatStatus.FINISHED;
    }
}
