# T5.1.2 — Validación de portabilidad

## Objetivo

Verificar que el proyecto puede compilarse, probarse y ejecutarse desde **clones limpios** en **Windows y Linux**, sin depender del IDE, de los archivos compilados previamente ni de rutas absolutas específicas de una máquina.

**Prerrequisitos:** JDK 17 o superior, Maven y Git. El primer uso de Maven puede requerir acceso a Internet para resolver dependencias.

## Procedimiento reproducible

1. Clonar la versión integrada (`develop`) en un directorio nuevo, preferiblemente con espacios en su ruta. Las validaciones históricas registradas abajo se hicieron sobre `test/T5.1.2-portability`.
2. Comprobar `git status`, la rama y el hash del commit antes de la ejecución.
3. Desde la raíz del clon, ejecutar el script correspondiente.
4. Guardar el directorio generado por el script y registrar la versión de Java, Maven, commit y resultado.

**Windows / PowerShell:**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\portability\verify.ps1
```

**Linux / Bash:**

```bash
bash ./scripts/portability/verify.sh
```

Ambos scripts realizan las mismas comprobaciones:

- Capturan versiones de Java, `javac`, Maven, información del sistema y commit.
- Ejecutan compilación limpia y suite Maven (`mvn clean test`).
- Construyen el JAR (`mvn -DskipTests package`).
- Comprueban la ayuda de `app.Main` sin iniciar Workers.
- Ejecutan `RANDOM` y `PRIORITY`, cada una con `--target=12 --scale=0.01`.
- Comprueban 12 admisiones T0, 12 finalizaciones T9, 12 T-invariantes, P/T-invariantes válidos, Workers terminados y logs generados.
- Escriben `status.txt` con `SUCCESS` sólo al finalizar toda la validación.

Estas corridas rápidas verifican **funcionalidad y portabilidad**, no la ventana experimental de 20–40 segundos con 200 T-invariantes. Esa ventana se estudió por separado en las campañas experimentales.

## Matriz de resultados observados

| Sistema / terminal | Java de ejecución / Maven | Commit probado | `clean test` | JAR | Ayuda | RANDOM 12 | PRIORITY 12 | Resultado |
|---|---|---|---|---|---|---|---|---|
| Windows / PowerShell | Java 17.0.20.1 / Maven 3.10.0 | `aaf18fc7a7bd2292cf057febc25a5cbf974ef48f` | **174/174** | OK | OK | OK | OK | **SUCCESS** |
| Linux / Bash | Java 27 / Maven 3.10.0 | `c311dc2fb75df1a1e95428b2fe7cbc9a3ef64def` | **174/174** | OK | OK | OK | OK | **SUCCESS** |

Las dos ejecuciones se realizaron sobre la rama `test/T5.1.2-portability` y desde clones limpios. **Los hashes de commit son diferentes**, por lo que no se afirma que ambos entornos probaron exactamente la misma revisión; se registra cada revisión de forma explícita para mantener la trazabilidad.

El compilador utilizó compatibilidad de código Java 17 (`--release 17`) en ambas plataformas. En Linux se ejecutó con **Java 27**, no con Java 17. La prueba comprueba compatibilidad con un runtime posterior al mínimo exigido, mientras que Java 17 como runtime se verificó en Windows.

## Validación Windows — clon limpio

**Fecha:** 15/10/2026.

- **Entorno:** Windows 11, PowerShell, Eclipse Temurin Java 17.0.20.1 y Maven 3.10.0.
- **Rama:** `test/T5.1.2-portability`.
- **Commit:** `aaf18fc7a7bd2292cf057febc25a5cbf974ef48f`.
- **Ubicación:** clon nuevo en un directorio con espacios.
- **Compilación y tests:** `BUILD SUCCESS`; 174 tests, 0 fallas, 0 errores, 0 omitidos.
- **Empaquetado y ayuda:** JAR generado correctamente; `app.Main --help` correcto.
- **RANDOM:** IT1=4, IT2=2, IT3=6; T0=T9=12; 0,057 s.
- **PRIORITY:** IT1=3, IT2=5, IT3=4; T0=T9=12; 0,069 s.
- **Verificación:** P-invariantes y T-invariantes válidos, Workers terminados y ambas corridas exitosas.
- **Evidencia local:** `results/portability/windows/20261015_115119/`.

**Resultado: VALIDACIÓN EXITOSA.**

### Validación preliminar adicional en Windows

El 15/10/2026 también se ejecutó el script en el repositorio de desarrollo (no era un clon limpio): 174 tests verdes, empaquetado correcto y ambas políticas finalizadas correctamente. Su evidencia quedó en `results/portability/windows/20261015_113210/`.

Además, se había verificado una corrida real `PRIORITY` con 200 transacciones, tiempos `BASELINE`, 200 admisiones, 200 finalizaciones, IT1=54, IT2=99, IT3=47 y duración de 36,526 s, con invariantes válidos y Workers terminados. Esta corrida no sustituye a las campañas ni a la prueba del clon limpio.

## Validación Linux — clon limpio

**Fecha:** 17/10/2026.

- **Entorno:** Linux / Bash, OpenJDK 27, `javac 27` y Apache Maven 3.10.0.
- **Rama:** `test/T5.1.2-portability`.
- **Commit:** `c311dc2fb75df1a1e95428b2fe7cbc9a3ef64def`.
- **Ubicación:** clon nuevo en un directorio con espacios; árbol de trabajo limpio antes de la prueba.
- **Compilación y tests:** 34 archivos fuente productivos y 29 de tests compilados con `release 17`; `BUILD SUCCESS`; 174 tests, 0 fallas, 0 errores y 0 omitidos.
- **Empaquetado y ayuda:** JAR generado correctamente y `app.Main --help` ejecutado.
- **RANDOM:** IT1=5, IT2=3, IT3=4; T0=T9=12; 0,036 s.
- **PRIORITY:** IT1=3, IT2=6, IT3=3; T0=T9=12; 0,036 s.
- **Verificación:** P-invariantes y T-invariantes válidos, Workers terminados y ambas corridas exitosas.
- **Evidencia local:** `results/portability/linux/20261017_192931/`.
- **Estado registrado:** `status.txt = SUCCESS`.

**Resultado: VALIDACIÓN EXITOSA.**

Las distribuciones de IT pueden variar entre ejecuciones por la concurrencia. La misma seed no implica que todas las intercalaciones de Workers sean idénticas.

## Manejo de evidencias y observaciones

Los scripts generan directorios con fecha y hora:

```text
results/portability/windows/AAAAMMDD_HHMMSS/
results/portability/linux/AAAAMMDD_HHMMSS/
```

Allí se guardan versiones, commit, logs de Maven, resultados de ambas políticas y `status.txt`. Estos archivos no necesitan incorporarse al código fuente; la carpeta `results/portability/.gitignore` evita su inclusión accidental. Si se solicita evidencia material en la entrega, puede conservarse un ZIP externo.

En las pruebas apareció un warning de conexión al repositorio `apache.snapshots`; **no fue un fallo de compilación** ni impidió ejecutar los tests o construir el JAR.

## Criterios de cierre

- [x] Windows: compilación, tests y ejecución de ambas políticas desde un clon limpio.
- [x] Linux: compilación, tests y ejecución de ambas políticas desde un clon limpio.
- [x] Registrar Java, Maven, commit y rutas de evidencia reales en ambos entornos.
- [x] Verificar rutas con espacios, creación de logs y terminación de Workers.
- [x] No se observaron defectos de portabilidad que impidieran completar las validaciones.
- [x] Documentación y scripts integrados a `develop` mediante PR.

**Conclusión:** T5.1.2 está completada. El proyecto se compiló, probó y ejecutó desde clones limpios en Windows y Linux. Los reportes reproducibles se generan localmente y no se agregan al código fuente.
