package cl.duoc.backendiii.semana1.cuentasanuales;

import java.math.BigDecimal;

/**
 * Modelo de salida: el resumen anual de una cuenta, listo para el informe
 * de auditoría (output/estado_cuentas_anuales.csv).
 */
public record EstadoCuentaAnual(
        String cuentaId,
        String nombre,
        String tipo,
        BigDecimal saldo,
        int cantidadMovimientos,
        BigDecimal totalDepositado,
        BigDecimal totalRetirado,
        BigDecimal movimientoNeto,
        String observacion
) {
}
