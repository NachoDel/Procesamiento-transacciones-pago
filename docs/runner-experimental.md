# Runner experimental

## Objetivo

El `ExperimentRunner` permite ejecutar corridas completas y reproducibles
del sistema concurrente de procesamiento de pagos.

Su responsabilidad es coordinar los componentes ya existentes del proyecto,
sin incorporar lógica propia de la Red de Petri, del Monitor o de los Workers.

El runner permite posteriormente ejecutar las campañas experimentales
definidas en `docs/protocolo-experimentos.md`.

---

## Componentes involucrados

Una corrida utiliza los componentes reales del sistema:

- `PetriNet`
- `Monitor`
- `WorkerCoordinator`
- `RandomPolicy`
- `PriorityPolicy`
- `TransitionTimingConfig`
- `RunProgressTracker`
- `ExecutionAudit`
- `ExecutionLogger`
- `PlaceInvariantChecker`
- `TInvariantAnalyzer`

Cada corrida crea una nueva instancia de la Red de Petri, evitando compartir
estado mutable entre experimentos.

---

## Configuración de una corrida

La configuración se representa mediante `ExperimentRunConfig`.

Cada corrida define:

- política;
- seed;
- nombre de configuración temporal;
- configuración temporal;
- target de operaciones;
- archivo de log;
- timeout máximo de ejecución;
- timeout máximo de shutdown.

Las políticas actualmente soportadas son:

```text
RANDOM
PRIORITY
