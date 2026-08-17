package cl.duoc.backendiii.semana1.cuentasanuales;

import java.math.BigDecimal;

/**
 * Data maestra de la cuenta (nombre, tipo, saldo), leída desde intereses.csv
 * y usada como referencia para enriquecer el estado de cuenta anual.
 */
public record CuentaMaestra(
        String cuentaId,
        String nombre,
        BigDecimal saldo,
        String tipo
) {
}
