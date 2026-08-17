package cl.duoc.backendiii.semana1.intereses;

import java.math.BigDecimal;

/**
 * Modelo de salida: la cuenta con la tasa aplicada y el saldo final,
 * lista para persistir en la tabla cuentas_saldo.
 */
public record CuentaConInteres(
        String cuentaId,
        String nombre,
        String tipo,
        BigDecimal saldoAnterior,
        BigDecimal tasaAplicada,
        BigDecimal interes,
        BigDecimal saldoFinal
) {
}
