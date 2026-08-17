package cl.duoc.backendiii.semana1.cuentasanuales;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo de entrada: una fila de cuentas_anuales.csv
 * (cuenta_id,fecha,transaccion,monto,descripcion).
 */
public record MovimientoCuenta(
        String cuentaId,
        LocalDate fecha,
        String transaccion,
        BigDecimal monto,
        String descripcion
) {
}
