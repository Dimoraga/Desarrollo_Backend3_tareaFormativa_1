package cl.duoc.backendiii.semana1.intereses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

/**
 * ItemProcessor: aplica la tasa de interés mensual según el tipo de cuenta
 * y calcula el saldo final que el writer persistirá en la base de datos.
 *
 * Tasas mensuales (didácticas):
 * - ahorro:   0.5%  (el saldo crece por el interés ganado)
 * - prestamo: 1.8%  (la deuda crece por el interés cobrado)
 * - hipoteca: 1.0%
 * - otro tipo: 0% (no se reconoce, se deja explícito en vez de fallar)
 */
public class InteresProcessor implements ItemProcessor<CuentaOrigen, CuentaConInteres> {

    private static final Logger log = LoggerFactory.getLogger(InteresProcessor.class);

    private static final Map<String, BigDecimal> TASAS_MENSUALES = Map.of(
            "ahorro", new BigDecimal("0.005"),
            "prestamo", new BigDecimal("0.018"),
            "hipoteca", new BigDecimal("0.010")
    );

    @Override
    public CuentaConInteres process(CuentaOrigen item) {
        String tipo = item.tipo().trim().toLowerCase(Locale.ROOT);
        BigDecimal tasa = TASAS_MENSUALES.getOrDefault(tipo, BigDecimal.ZERO);

        BigDecimal interes = item.saldo().multiply(tasa).setScale(2, RoundingMode.HALF_UP);
        BigDecimal saldoFinal = item.saldo().add(interes);

        log.info("Cuenta {} ({}): saldo {} + interés {} = saldo final {}",
                item.cuentaId(), tipo, item.saldo(), interes, saldoFinal);

        return new CuentaConInteres(item.cuentaId(), item.nombre(), tipo, item.saldo(), tasa, interes, saldoFinal);
    }
}
