package cl.duoc.backendiii.semana1.transacciones;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo de salida: la transacción con el resultado de la detección de
 * anomalías, listo para el reporte y para el resumen del día.
 */
public record TransaccionProcesada(
        Long id,
        LocalDate fecha,
        BigDecimal monto,
        String tipo,
        boolean anomalia,
        String detalle
) {
}
