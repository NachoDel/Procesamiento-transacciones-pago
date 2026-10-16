# T5.1.3 — Diagramas UML del procesamiento concurrente de pagos

## Alcance y fuente

Diagramas preparados a partir de `src/main/java` del repositorio compartido, rama `docs/T5.1.3-diagramas-uml`, commit `184ca21b6fa6837f9ac989cad0694390588f71bf`.

Los archivos **`.puml` son las fuentes editables** de la documentación UML. Las imágenes `.svg`/`.png` entregadas junto a ellos son vistas previas esquemáticas para consulta; si modificás el `.puml`, regenerá las imágenes definitivas con PlantUML para que queden idénticas a la fuente.

## Diagramas

| Archivo fuente | Qué representa | Imagen de referencia |
|---|---|---|
| [`diagrama-clases-nucleo.puml`](diagrama-clases-nucleo.puml) | Diseño de `Main`, `ExperimentRunner`, Monitor, red, políticas, Workers, observer, log y verificación. | [`diagrama-clases-nucleo.svg`](../img/uml/diagrama-clases-nucleo.svg) |
| [`diagrama-clases-campanas.puml`](diagrama-clases-campanas.puml) | Ampliación del diagrama de clases: campaña formal, calibración, ejecución repetida, records y CSV. | [`diagrama-clases-campanas.svg`](../img/uml/diagrama-clases-campanas.svg) |
| [`diagrama-secuencia-corrida.puml`](diagrama-secuencia-corrida.puml) | Una corrida completa: armado, Workers concurrentes, espera, disparo, post-fire, cierre de T0, drenaje, shutdown y análisis del log. | [`diagrama-secuencia-corrida.svg`](../img/uml/diagrama-secuencia-corrida.svg) |

Separar las **dos vistas de clases** evita un diagrama único demasiado grande. Juntas constituyen la documentación del diagrama de clases del TP.

## Decisiones que los diagramas documentan

1. `MonitorInterface` expone `boolean fireTransition(int)`. `Worker` conoce la interfaz, no la clase concreta `Monitor`.
2. `Monitor` conserva el lock, condiciones, arbitraje de conflictos y temporización. Depende de `PetriNet`, `Policy`, `TransitionSemantics`, `TransitionTimingConfig` y `PostFireObserver`.
3. El único grupo de conflicto oficial para la política es `{T1, T4, T6}`; no se representa una prioridad global artificial.
4. `ExperimentRunner` es responsable de que **T0 deje de estar disponible** al cerrarse admisiones; esta regla se transmite como `IntPredicate` sin codificarla dentro de `Monitor`.
5. `PostFireObserver` se invoca tras confirmar `PetriNet.fire(t)` y bajo el lock del Monitor. `ExperimentPostFireObserver` actualiza `RunProgressTracker` y luego `ExecutionAudit`; el auditor verifica el P-invariante y registra el disparo.
6. `H0` deja de iniciar ciclos cuando `isAdmissionClosed()`. `H1`–`H4` continúan hasta alcanzar el `T9` número `TARGET`. Luego se solicita `requestStop()`, `interruptAll()` y `awaitTermination()`.
7. `TInvariantAnalyzer` consume el archivo **después** del cierre de `ExecutionLogger` y de la terminación de los Workers.
8. En campañas, `CampaignExecutor` utiliza `CampaignExecutor.RunExecutor` (interfaz funcional package-private) con `ExperimentRunner::run` en producción. Registra corridas exitosas e inválidas o fallidas y persiste el CSV con `CampaignSummaryWriter`.

## Cómo exportar las imágenes definitivas en Windows o Linux

Desde la raíz del repositorio, con **PlantUML** instalado:

```bash
plantuml -tsvg docs/uml/*.puml
plantuml -tpng docs/uml/*.puml
```

O con un archivo `plantuml.jar` instalado localmente (sin subirlo al repositorio):

```bash
java -jar /ruta/local/plantuml.jar -tsvg docs/uml/*.puml
java -jar /ruta/local/plantuml.jar -tpng docs/uml/*.puml
```

PlantUML genera las imágenes junto a cada `.puml`. Si querés consolidarlas en `docs/img/uml/`, movelas allí manteniendo sus nombres. En Windows/PowerShell, el comodín `*.puml` puede variar según la forma de invocación; también podés indicar los tres archivos explícitamente.

No se requiere tocar código ni ejecutar Maven para modificar diagramas.

## Qué deliberadamente no se muestra

Los diagramas son **vistas de arquitectura**, no un volcado automático de todos los atributos, constructores y getters. Se omiten métodos privados de validación y clases auxiliares internas que no alteran las relaciones principales. El detalle completo sigue estando en el código fuente.

El diagrama de secuencia describe **una ejecución representativa**, no un orden determinista de los disparos de los cinco threads. Los eventos de Workers pueden intercalarse de distintas formas, preservando las restricciones del Monitor.

## Verificación antes de mergear

- Comparar clases, nombres de métodos y dependencias con `src/main/java`.
- Abrir los `.puml` en la extensión PlantUML del editor o renderizarlos con la CLI.
- Comprobar que las etiquetas y flechas se lean sin recortes al exportar SVG.
- Ejecutar `git diff --check`.
- El cambio debe limitarse a documentación y recursos gráficos.
