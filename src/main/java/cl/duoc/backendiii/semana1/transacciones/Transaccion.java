package cl.duoc.backendiii.semana1.transacciones;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo de entrada: una fila de transacciones.csv (id,fecha,monto,tipo).
 */
public record Transaccion(
        Long id,
        LocalDate fecha,
        BigDecimal monto,
        String tipo
) {
}
