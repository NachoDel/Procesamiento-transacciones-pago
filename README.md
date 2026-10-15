# TP Final — Programación Concurrente 2026

Sistema de procesamiento concurrente de transacciones de pago modelado con una Red de Petri en Java 17. La ejecución se coordina mediante un `Monitor`, Workers `H0`–`H4` y políticas de selección aleatoria o de prioridad de alto riesgo.

## Requisitos

- JDK 17 o superior.
- Maven compatible con el proyecto (se recomienda Maven 3.9+).
- Windows, Linux o macOS. No se requiere un IDE específico.

## Compilar y ejecutar pruebas

Desde la raíz del repositorio:

```bash
mvn clean test
```

Para compilar el programa sin volver a ejecutar los tests:

```bash
mvn -DskipTests package
```

**PowerShell (Windows):** si Maven interpreta de forma incorrecta un argumento `-D`, encerralo entre comillas:

```powershell
mvn "-DskipTests" package
```

## Ejecutar el programa principal

La clase de entrada es `app.Main` (no `ar.edu.unc.concurrente.Main`). Ejecutala desde la raíz del proyecto, después de compilar:

```bash
java -cp target/classes app.Main
```

Sin argumentos realiza **una corrida completa de 200 transacciones** usando `RANDOM`, seed `2026` y tiempos `BASELINE`. Normalmente tarda varias decenas de segundos.

Para consultar las opciones **sin ejecutar transacciones**:

```bash
java -cp target/classes app.Main --help
```

### Opciones

| Opción | Valores / comportamiento | Predeterminado |
|---|---|---|
| `--policy` | `RANDOM` o `PRIORITY` | `RANDOM` |
| `--seed` | entero largo (seed del selector aleatorio) | `2026` |
| `--target` | cantidad positiva de transacciones completas | `200` |
| `--scale` | factor positivo de tiempos; `1.0` = baseline, `1.05` = 5 % mayor | `1.0` |
| `--timeout-seconds` | tiempo máximo para completar la corrida | `60` |
| `--log` | ruta del log de esa corrida | `results/logs/` |
| `--help` | muestra ayuda y termina | — |

Las opciones se escriben como `--clave=valor`. Usá **punto decimal** para `--scale` tanto en Windows como en Linux.

### Ejemplo: comparar políticas con 200 transacciones

```bash
java -cp target/classes app.Main --policy=RANDOM --target=200 --seed=2026
java -cp target/classes app.Main --policy=PRIORITY --target=200 --seed=2026
```

### Ejemplo: prueba rápida en Windows PowerShell

```powershell
mvn "-DskipTests" package
java -cp target/classes app.Main --policy=RANDOM --target=12 --scale=0.01 --seed=2026
```

Esta prueba rápida es de diagnóstico: sus tiempos no sirven para validar el requisito temporal de 20–40 segundos de la campaña formal.

### Resultado de una corrida

`Main` muestra en consola:

- admisiones `T0` y finalizaciones `T9`;
- cantidades de `IT1` (tarjeta), `IT2` (alto riesgo) e `IT3` (transferencia);
- total de T-invariantes y duración;
- resultado de P-invariantes y T-invariantes;
- estado final de los Workers y ubicación del log.

**Una corrida se considera exitosa** cuando las admisiones, finalizaciones y T-invariantes coinciden exactamente con `--target`, las invariantes son válidas y todos los Workers terminaron. Si no se cumplen estas condiciones, el runner informa el fallo.

> El log por defecto está en `results/logs/`, con política, configuración temporal, seed y target en el nombre. Si se repite exactamente la misma configuración, el logger reemplaza el archivo anterior; guardá una copia si necesitás conservarlo como evidencia.

## Ejecuciones experimentales formales

`app.Main` ejecuta **una única corrida**. No sustituye las campañas de DEV4, que están implementadas en `experiment.FormalCampaignMain`.

Para ejecutar una campaña formal (20 corridas, configuración temporal ya calibrada):

```bash
java -cp target/classes experiment.FormalCampaignMain
```

Consultá `docs/protocolo-experimentos.md`, `docs/runner-experimental.md` y `docs/evidencia-T4.2.2.md` antes de repetir la campaña, ya que puede tardar varios minutos.

## Estructura del código

```text
src/main/java/
├── app/           # Punto de entrada Main
├── config/        # Modelo de pagos, tiempos y conflictos
├── experiment/    # Runner, progreso y campañas
├── logging/       # Log de disparos
├── monitor/       # Exclusión mutua y esperas
├── petri/         # Red de Petri genérica
├── policy/        # Políticas aleatoria y priorizada
├── timing/        # Semántica temporal
├── verification/  # Verificación de invariantes
└── worker/        # Workers y lifecycle
```

La política de ramas utiliza `develop` para integración y `main` para versiones estables. Las tareas se integran mediante Pull Requests.
