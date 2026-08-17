package cl.duoc.backendiii.semana1.transacciones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * ItemProcessor: detecta anomalías en cada transacción y, de paso, acumula
 * las estadísticas del día (total procesadas, anomalías, monto total) que
 * luego lee ResumenTransaccionesTasklet en el segundo step del Job.
 *
 * Reglas de anomalía (simples, con fines didácticos):
 * - monto == 0            -> MONTO_CERO
 * - monto negativo        -> MONTO_NEGATIVO
 * - monto > 2500           -> MONTO_ALTO_INUSUAL
 * - misma fecha+monto+tipo ya vista -> POSIBLE_DUPLICADO
 */
public class TransaccionAnomaliaProcessor implements ItemProcessor<Transaccion, TransaccionProcesada> {

    private static final Logger log = LoggerFactory.getLogger(TransaccionAnomaliaProcessor.class);
    private static final BigDecimal MONTO_ALTO = new BigDecimal("2500");

    private final Set<String> combinacionesVistas = new HashSet<>();

    // Acumulado durante el step; el Job ejecuta este step antes del resumen,
    // por lo que al terminar el chunk step estos valores ya están completos.
    private int totalProcesadas = 0;
    private int totalAnomalias = 0;
    private BigDecimal montoTotal = BigDecimal.ZERO;

    @Override
    public TransaccionProcesada process(Transaccion item) {
        totalProcesadas++;
        montoTotal = montoTotal.add(item.monto());

        String detalle = detectarAnomalia(item);
        boolean anomalia = detalle != null;
        if (anomalia) {
            totalAnomalias++;
            log.warn("Anomalía en transacción {}: {}", item.id(), detalle);
        }

        return new TransaccionProcesada(item.id(), item.fecha(), item.monto(), item.tipo(), anomalia,
                anomalia ? detalle : "OK");
    }

    private String detectarAnomalia(Transaccion item) {
        if (item.monto().compareTo(BigDecimal.ZERO) == 0) {
            return "MONTO_CERO";
        }
        if (item.monto().compareTo(BigDecimal.ZERO) < 0) {
            return "MONTO_NEGATIVO";
        }
        if (item.monto().compareTo(MONTO_ALTO) > 0) {
            return "MONTO_ALTO_INUSUAL";
        }
        String clave = item.fecha() + "|" + item.monto() + "|" + item.tipo();
        if (!combinacionesVistas.add(clave)) {
            return "POSIBLE_DUPLICADO";
        }
        return null;
    }

    public int getTotalProcesadas() {
        return totalProcesadas;
    }

    public int getTotalAnomalias() {
        return totalAnomalias;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }
}
