package cl.duoc.backendiii.semana1.intereses;

import java.math.BigDecimal;

/**
 * Modelo de entrada: una fila de intereses.csv (cuenta_id,nombre,saldo,edad,tipo).
 */
public record CuentaOrigen(
        String cuentaId,
        String nombre,
        BigDecimal saldo,
        int edad,
        String tipo
) {
}
