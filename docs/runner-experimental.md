# Runner experimental

## Objetivo

`experiment.ExperimentRunner` orquesta una corrida completa del sistema concurrente de procesamiento de pagos. Utiliza los componentes existentes sin trasladar al runner la lógica interna de la Red de Petri, del Monitor ni de los Workers.

Es una implementación **operativa**. La campaña formal ya está construida sobre este runner mediante `CampaignPlan` y `CampaignExecutor`; las instrucciones de ejecución están en el [`README.md`](../README.md).

## Componentes involucrados

| Componente | Responsabilidad |
|---|---|
| `ExperimentRunConfig` | Política, seed, nombre y valores de timing, target, archivo de log y timeouts. |
| `PetriNet` | Marcado y disparos de la red. Cada corrida usa una instancia nueva. |
| `RandomPolicy` / `PriorityPolicy` | Arbitraje del conflicto según la política configurada. |
| `Monitor` | Exclusión mutua, espera, revalidación y disparo atómico. |
| `WorkerCoordinator` | Creación y control del ciclo de vida de H0–H4. |
| `RunProgressTracker` | Conteo de admisiones T0 y finalizaciones T9; estado de cierre de admisión. |
| `ExperimentPostFireObserver` | Compone la actualización del progreso con la auditoría de cada disparo. |
| `ExecutionAudit` | Comprueba los P-invariantes y registra el disparo. |
| `ExecutionLogger` | Escribe la traza parseable de transiciones. |
| `TInvariantAnalyzer` | Analiza el log final mediante expresiones regulares. |
| `ExperimentRunResult` | Resultado consolidado de una corrida. |

## Configuración de una corrida

Una corrida utiliza `ExperimentRunConfig` con:

- `PolicyMode`: `RANDOM` o `PRIORITY`;
- seed del selector aleatorio (o fallback de prioridad);
- nombre y objeto de configuración temporal;
- cantidad objetivo de operaciones completas (`target`);
- ruta del log;
- timeout de ejecución y timeout de shutdown.

El `Main` permite ejecutar una corrida de 200 T-invariantes por defecto o ajustar parámetros mediante línea de comandos. Las campañas definen sus propias configuraciones con `CampaignPlan`.

## Flujo de ejecución

1. **Aislamiento.** `ExperimentRunner.run(config)` crea una nueva `PetriNet`, una política independiente y un `RunProgressTracker` para el objetivo de la corrida.
2. **Observación y log.** Abre `ExecutionLogger` y prepara `ExecutionAudit` y `ExperimentPostFireObserver`.
3. **Monitor.** Construye el Monitor con la semántica temporal, los grupos de conflicto, la política y una condición de disponibilidad dinámica para T0.
4. **Workers.** Construye `WorkerCoordinator` con H0–H4, configurando para H0 el stop selectivo `tracker::isAdmissionClosed`.
5. **Ejecución.** Inicia los Workers y espera el objetivo de finalizaciones, comprobando también timeout o fallos asíncronos de auditoría.
6. **Cierre global.** Solicita stop, interrumpe para despertar Threads bloqueados y espera su terminación.
7. **Comprobaciones.** Exige exactamente `TARGET` disparos T0 y T9, ningún fallo post-fire y todos los Workers terminados.
8. **Post-run.** Cierra el log, lo analiza con `TInvariantAnalyzer` y genera `ExperimentRunResult`.

## Cierre de admisiones y drenaje

Al confirmarse el disparo `T0` número `TARGET`, `RunProgressTracker` cierra nuevas admisiones. Desde ese momento se aplican dos medidas coordinadas:

```text
H0: no inicia nuevos ciclos
T0: deja de ser una transición disponible para el Monitor
```

La segunda medida es importante porque T0 es inmediata y podría seguir sensibilizada estructuralmente en `PetriNet`. No debe bloquear el avance de transiciones temporales si H0 ya no la va a ejecutar.

**H1–H4 siguen activos** para finalizar las transacciones admitidas. Cuando T9 alcanza `TARGET`, el runner solicita la terminación global. La política específica de drenaje está explicada en [`ADR/Monitor-Disponibilidad-Dinamica-Drenaje.md`](ADR/Monitor-Disponibilidad-Dinamica-Drenaje.md).

El Monitor sigue siendo agnóstico al modelo: el conocimiento de T0 y TARGET pertenece al runner y al tracker, no a `Monitor`.

## Auditoría y análisis

La composición de `ExperimentPostFireObserver` se invoca sincrónicamente después de cada disparo confirmado y ejecuta la actualización de progreso y la auditoría.

La auditoría verifica los **P-invariantes online** y escribe el log. La lectura final del log ocurre únicamente cuando los Workers terminaron y `ExecutionLogger` ya está cerrado.

`TInvariantAnalyzer` cuenta IT1, IT2 e IT3 mediante expresiones regulares sobre las secuencias propias de los Workers. Ver detalles del formato en [`log-format.md`](log-format.md).

## Medición temporal

La duración se mide con `System.nanoTime()` desde inmediatamente antes de `coordinator.startAll()` hasta que el thread coordinador observa la finalización del `TARGET` de T9.

**La duración experimental no incluye** el overhead administrativo posterior del shutdown ni el análisis del log. El runner usa un intervalo de sondeo corto para comprobar progreso, fallos y timeout.

## Resultado de la corrida

`ExperimentRunResult` concentra, entre otros datos:

- admisiones y finalizaciones;
- IT1, IT2 e IT3 y su suma;
- duración;
- validez de P-invariantes y T-invariantes;
- terminación de Workers;
- ruta del log y resultado exitoso/no exitoso.

Una ejecución con `target=200` solamente es válida si se cumplen las condiciones del [`protocolo-experimentos.md`](protocolo-experimentos.md).

## Campañas formales

`CampaignPlan` construye la matriz de dos políticas × dos configuraciones temporales × cinco seeds, con `TARGET=200`: 20 corridas. `CampaignExecutor` usa el runner y `CampaignSummaryWriter` registra resultados consolidados.

El código y el protocolo de campaña ya están implementados. Los resultados obtenidos están documentados en [`evidencia-T4.2.2.md`](evidencia-T4.2.2.md) y su interpretación en [`analisis-T4.2.3.md`](analisis-T4.2.3.md).
