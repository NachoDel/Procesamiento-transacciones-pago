# T5.1.2 — Validación de portabilidad

## Objetivo

Probar desde consola que un **clon limpio** del proyecto se compila y ejecuta en Windows y Linux sin depender de un IDE, rutas locales fijas ni permisos específicos sobre archivos fuente.

**Prerrequisitos:** JDK 17 o superior y Maven disponibles en `PATH`; Git para obtener el repositorio. La primera ejecución de Maven puede requerir conexión a Internet para descargar dependencias. No se promete compatibilidad con todos los sistemas operativos existentes.

## Procedimiento

1. Clonar el repositorio en una carpeta nueva, idealmente con espacios en el nombre de la ruta. No copiar `target/`, `.idea/` ni logs locales.
2. Seleccionar el mismo commit o rama de la prueba en los dos entornos.
3. Ejecutar el script correspondiente desde la raíz del repositorio.
4. Conservar el directorio generado de evidencia y anotar los resultados en la matriz.
5. Si falla alguna etapa, documentar y corregir el problema antes de repetir desde un clon limpio.

### Windows — PowerShell

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\portability\verify.ps1
```

`-ExecutionPolicy Bypass` afecta sólo ese proceso, no cambia permanentemente la configuración del usuario. Si la sesión ya permite ejecutar scripts, también sirve:

```powershell
.\scripts\portability\verify.ps1
```

### Linux — Bash

```bash
bash scripts/portability/verify.sh
```

Al invocarlo mediante `bash` no es necesario utilizar `chmod +x`.

## Qué verifica cada script

- Captura versión de `java`, `javac`, `mvn` y hash del commit.
- Ejecuta `mvn clean test` y `mvn -DskipTests package`.
- Ejecuta `app.Main --help` sin crear Workers.
- Ejecuta `RANDOM` y `PRIORITY` por separado, cada una con `--target=12 --scale=0.01` y timeout de 20 s.
- Exige `T0=12`, `T9=12`, 12 T-invariantes, P/T-invariantes válidos, Workers terminados y corrida exitosa.
- Comprueba que se creó un log no vacío para cada política.

Las corridas rápidas **no** validan la ventana de 20–40 segundos exigida para 200 invariantes. Para ese requisito se conserva la campaña de DEV4 en `docs/evidencia-T4.2.2.md`.

## Evidencias

Cada script crea un directorio específico para el SO y la fecha de ejecución:

```text
results/portability/windows/AAAAMMDD_HHMMSS/
results/portability/linux/AAAAMMDD_HHMMSS/
```

Incluye versiones de las herramientas, commit, salida de los tests y del empaquetado, ayuda, resúmenes y logs de ambas políticas. Sólo aparece `status.txt` con `SUCCESS` al finalizar todas las comprobaciones. `results/portability/.gitignore` evita versionar por accidente esos archivos generados; guardar un ZIP de evidencia si se necesita adjuntar a la entrega.

## Matriz de portabilidad — completar con pruebas reales

| SO / terminal | Java / Maven | Commit | `clean test` | `package` | `--help` | RANDOM 12 | PRIORITY 12 | Carpeta de evidencia | Estado |
|---|---|---|---|---|---|---|---|---|---|
| Windows / PowerShell | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente | `results/portability/windows/...` | PENDIENTE |
| Linux / Bash | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente | `results/portability/linux/...` | PENDIENTE |

## Validación preliminar — Windows (repositorio de trabajo)

**Fecha:** 15/10/2026. Esta ejecución **no se realizó desde un clon limpio** y, por lo tanto, todavía no satisface ese criterio de cierre.

| Campo | Resultado observado |
|---|---|
| SO y terminal | Windows 11 / PowerShell |
| Java y compilador | Eclipse Temurin OpenJDK 17.0.20.1 / `javac 17.0.20.1` |
| Maven | Apache Maven 3.10.0 |
| Commit registrado | `184ca21b6fa6837f9ac989cad0694390588f71bf` |
| `mvn clean test` | **SUCCESS**: 174 tests, 0 failures, 0 errors, 0 skipped |
| `mvn -DskipTests package` | **SUCCESS**: JAR construido |
| `app.Main --help` | **SUCCESS**: mostró la ayuda |
| RANDOM (`--target=12 --scale=0.01`) | **SUCCESS**: IT1=5, IT2=1, IT3=6; 12 admisiones y 12 finalizaciones; 0,052 s |
| PRIORITY (`--target=12 --scale=0.01`) | **SUCCESS**: IT1=3, IT2=6, IT3=3; 12 admisiones y 12 finalizaciones; 0,068 s |
| Invariantes y finalización | Ambas corridas: P/T-invariantes válidos, Workers terminados |
| Evidencia local | `results/portability/windows/20261015_113210/` |

El warning de conexión a `apache.snapshots` no impidió compilar, probar ni empaquetar. Los registros generados quedan en la carpeta local de evidencia; no es necesario versionarlos.

**Evidencia funcional adicional:** previamente se ejecutó `PRIORITY` con 200 transacciones y tiempos `BASELINE`: 200 admisiones, 200 finalizaciones, IT1=54, IT2=99, IT3=47, duración 36,526 s, P/T-invariantes válidos y Workers terminados.

**Pendiente:** realizar las pruebas desde clones limpios en Windows y Linux. Actualizar las filas de la matriz anterior sólo después de completar cada una.

## Criterios de cierre

- [ ] La prueba de Windows pasa desde un clon limpio.
- [ ] La prueba de Linux pasa desde un clon limpio.
- [ ] La matriz registra Java, Maven, commit y ubicación de las evidencias reales.
- [ ] Se registran y solucionan defectos de separadores, codificación, permisos o rutas detectados.
- [ ] La documentación y scripts están revisados y mergeados a `develop`.

**No cerrar T5.1.2 hasta completar ambas validaciones.**
