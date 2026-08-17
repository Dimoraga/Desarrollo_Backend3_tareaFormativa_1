# Tarea Formativa 1 correspondiente al curso Desarrollo Backend III

Esta actividad consistió en familiarizarnos con Spring Batch y la gestión de datos Legacy
Siguiendo las instrucciones entregadas se configuró un Job para cada uno de los tres procesos siguientes:
1. Reporte de transacciones diarias: Procesar transacciones diarias para detectar anomalías y generar un resumen.
2. Cálculo de intereses mensuales: Aplicar intereses sobre cuentas de ahorro y préstamos y actualizar el saldo final en una base de datos.
3. Generación de Estado de cuentas anuales: Compilar datos anuales para cada cuenta y generar un informe detallado para auditorías.

Implementa tres Jobs
independientes que procesan archivos CSV de cuentas y transacciones bancarias,
mostrando los componentes centrales de Spring Batch: `Job`, `Step`, `ItemReader`,
`ItemProcessor`, `ItemWriter` y `Tasklet`.

## Los tres Jobs

### 1. Reporte de transacciones diarias (`reporteTransaccionesDiariasJob`)
Lee `input/transacciones.csv`, detecta anomalías en cada transacción y genera un
resumen del día.

- **Step 1** `procesarTransaccionesStep` (chunk): lee, marca anomalías y escribe el
  detalle en `output/reporte_transacciones_diarias.csv`.
- **Step 2** `resumenTransaccionesStep` (tasklet): con las estadísticas acumuladas en
  el step anterior, escribe `output/resumen_transacciones_diarias.txt`.

Reglas de anomalía: monto en cero, monto negativo, monto superior a 2500, o una
combinación fecha+monto+tipo repetida (posible duplicado).

### 2. Cálculo de intereses mensuales (`calculoInteresesMensualesJob`)
Lee `input/intereses.csv` (cuentas de ahorro, préstamo e hipoteca), aplica la tasa
mensual correspondiente y actualiza el saldo final en la base de datos.

- **Step 1** `crearTablaCuentasStep` (tasklet): crea la tabla H2 `cuentas_saldo` si
  no existe.
- **Step 2** `aplicarInteresesStep` (chunk): calcula interés según tipo de cuenta
  (ahorro 0.5%, préstamo 1.8%, hipoteca 1.0%) y hace *upsert* del resultado en
  `cuentas_saldo`.

### 3. Estado de cuentas anuales (`generarEstadoCuentasAnualesJob`)
Cruza la data maestra de cada cuenta (`input/intereses.csv`) con sus movimientos del
año (`input/cuentas_anuales.csv`) y genera un informe detallado para auditoría en
`output/estado_cuentas_anuales.csv` (depósitos, retiros, movimiento neto y una
observación por cuenta).

- **Step único** `compilarEstadoCuentasAnualesStep` (tasklet): se implementa como
  tasklet, no como chunk, porque necesita ver todos los movimientos de una cuenta
  antes de poder escribir su línea del informe.

Los tres Jobs se ejecutan en secuencia al iniciar la aplicación, orquestados por
`BatchJobRunner`.

## Estructura del código

```
src/main/java/cl/duoc/backendiii/semana1/
├── Semana1BatchApplication.java        # Punto de entrada Spring Boot
├── BatchJobRunner.java                 # Lanza los 3 Jobs en orden al arrancar
├── JobCompletionNotificationListener.java  # Listener genérico de fin de Job
├── transacciones/                      # Job 1: reporte de transacciones diarias
├── intereses/                          # Job 2: cálculo de intereses mensuales
└── cuentasanuales/                     # Job 3: estado de cuentas anuales

src/main/resources/
├── application.properties
└── input/
    ├── transacciones.csv      # id, fecha, monto, tipo
    ├── intereses.csv          # cuenta_id, nombre, saldo, edad, tipo
    └── cuentas_anuales.csv    # cuenta_id, fecha, transaccion, monto, descripcion
```

Cada paquete de Job sigue el mismo patrón interno:

- **Modelos** (`record`): representan las filas de entrada y el resultado ya
  procesado.
- **Processor**: contiene la lógica de negocio (detección de anomalías, cálculo de
  interés, agregación anual).
- **`*JobConfig`**: declara los beans de Spring Batch (`Job`, `Step`, `Reader`,
  `Writer`) para ese proceso.

## Requisitos

- Java 17+
- Maven 3.9+ (o usar el `mvnw` si se agrega al proyecto)
- Docker (opcional, para ejecutar en contenedor)

## Cómo ejecutar

### Con Maven

```bash
mvn spring-boot:run
```

Al iniciar, la aplicación:

1. Crea el esquema de metadata de Spring Batch en una base H2 en memoria.
2. Ejecuta los tres Jobs en orden (transacciones → intereses → cuentas anuales).
3. Deja los archivos generados en la carpeta `output/`.

### Con Docker

```bash
docker compose up --build
```

El `docker-compose.yml` monta `./output` como volumen, así que los reportes quedan
disponibles en el host aunque el contenedor se detenga.

## Salidas generadas

| Archivo | Job | Contenido |
|---|---|---|
| `output/reporte_transacciones_diarias.csv` | 1 | Detalle de transacciones con anomalía marcada |
| `output/resumen_transacciones_diarias.txt` | 1 | Totales del día (procesadas, anomalías, montos) |
| `output/estado_cuentas_anuales.csv` | 3 | Estado anual por cuenta para auditoría |
| Tabla H2 `cuentas_saldo` | 2 | Saldo final por cuenta tras aplicar interés (en memoria, se pierde al cerrar la app) |

## Base de datos

Se usa H2 en memoria (`jdbc:h2:mem:batchdb`), autocontenida y sin instalación
adicional. Se recrea en cada ejecución, por lo que la tabla `cuentas_saldo` del Job 2
no persiste entre corridas.
