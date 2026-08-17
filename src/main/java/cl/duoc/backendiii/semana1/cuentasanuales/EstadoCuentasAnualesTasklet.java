package cl.duoc.backendiii.semana1.cuentasanuales;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.ClassPathResource;

import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tasklet: cruza la data maestra de cada cuenta (intereses.csv: nombre, tipo,
 * saldo) con sus movimientos del año (cuentas_anuales.csv) y genera un
 * informe consolidado para auditoría.
 *
 * Se implementa como Tasklet, no como Step chunk, porque el resultado no es
 * "un registro de entrada -> un registro de salida": es una agregación por
 * cuenta que necesita ver todos los movimientos antes de poder escribir
 * cualquier línea del informe.
 */
public class EstadoCuentasAnualesTasklet implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        Map<String, CuentaMaestra> maestras = leerCuentasMaestras();
        Map<String, List<MovimientoCuenta>> movimientosPorCuenta = leerMovimientos();

        List<EstadoCuentaAnual> estados = new ArrayList<>();
        for (CuentaMaestra maestra : maestras.values()) {
            List<MovimientoCuenta> movimientos = movimientosPorCuenta.getOrDefault(maestra.cuentaId(), List.of());
            estados.add(compilarEstado(maestra, movimientos));
        }

        escribirInforme(estados);
        return RepeatStatus.FINISHED;
    }

    private EstadoCuentaAnual compilarEstado(CuentaMaestra maestra, List<MovimientoCuenta> movimientos) {
        BigDecimal depositado = BigDecimal.ZERO;
        BigDecimal retirado = BigDecimal.ZERO;

        for (MovimientoCuenta m : movimientos) {
            if (m.monto().compareTo(BigDecimal.ZERO) >= 0) {
                depositado = depositado.add(m.monto());
            } else {
                retirado = retirado.add(m.monto().abs());
            }
        }

        BigDecimal neto = depositado.subtract(retirado);
        String observacion;
        if (movimientos.isEmpty()) {
            observacion = "SIN_MOVIMIENTOS";
        } else if (maestra.saldo().add(neto).compareTo(BigDecimal.ZERO) < 0) {
            observacion = "SALDO_PROYECTADO_NEGATIVO";
        } else {
            observacion = "OK";
        }

        return new EstadoCuentaAnual(maestra.cuentaId(), maestra.nombre(), maestra.tipo(), maestra.saldo(),
                movimientos.size(), depositado, retirado, neto, observacion);
    }

    private Map<String, CuentaMaestra> leerCuentasMaestras() throws Exception {
        FlatFileItemReader<CuentaMaestra> reader = new FlatFileItemReaderBuilder<CuentaMaestra>()
                .name("cuentasMaestrasReader")
                .resource(new ClassPathResource("input/intereses.csv"))
                .linesToSkip(1)
                .delimited()
                .delimiter(",")
                .names("cuentaId", "nombre", "saldo", "edad", "tipo")
                .fieldSetMapper(fs -> new CuentaMaestra(
                        fs.readString("cuentaId"),
                        fs.readString("nombre"),
                        fs.readBigDecimal("saldo"),
                        fs.readString("tipo")
                ))
                .build();

        Map<String, CuentaMaestra> resultado = new LinkedHashMap<>();
        reader.open(new ExecutionContext());
        try {
            CuentaMaestra item;
            while ((item = reader.read()) != null) {
                resultado.put(item.cuentaId(), item);
            }
        } finally {
            reader.close();
        }
        return resultado;
    }

    private Map<String, List<MovimientoCuenta>> leerMovimientos() throws Exception {
        FlatFileItemReader<MovimientoCuenta> reader = new FlatFileItemReaderBuilder<MovimientoCuenta>()
                .name("movimientosReader")
                .resource(new ClassPathResource("input/cuentas_anuales.csv"))
                .linesToSkip(1)
                .delimited()
                .delimiter(",")
                .names("cuentaId", "fecha", "transaccion", "monto", "descripcion")
                .fieldSetMapper(fs -> new MovimientoCuenta(
                        fs.readString("cuentaId"),
                        LocalDate.parse(fs.readString("fecha")),
                        fs.readString("transaccion"),
                        fs.readBigDecimal("monto"),
                        fs.readString("descripcion")
                ))
                .build();

        Map<String, List<MovimientoCuenta>> resultado = new LinkedHashMap<>();
        reader.open(new ExecutionContext());
        try {
            MovimientoCuenta item;
            while ((item = reader.read()) != null) {
                resultado.computeIfAbsent(item.cuentaId(), k -> new ArrayList<>()).add(item);
            }
        } finally {
            reader.close();
        }
        return resultado;
    }

    private void escribirInforme(List<EstadoCuentaAnual> estados) throws Exception {
        Files.createDirectories(Path.of("output"));
        try (Writer writer = Files.newBufferedWriter(Path.of("output/estado_cuentas_anuales.csv"),
                StandardCharsets.UTF_8)) {
            writer.write("cuenta_id;nombre;tipo;saldo;cantidad_movimientos;total_depositado;total_retirado;movimiento_neto;observacion\n");
            for (EstadoCuentaAnual e : estados) {
                writer.write(String.join(";",
                        e.cuentaId(), e.nombre(), e.tipo(), e.saldo().toPlainString(),
                        String.valueOf(e.cantidadMovimientos()), e.totalDepositado().toPlainString(),
                        e.totalRetirado().toPlainString(), e.movimientoNeto().toPlainString(), e.observacion()));
                writer.write("\n");
            }
        }
    }
}
