# ADR — Prioridad, fairness y arbitraje persistente de conflictos

**Estado:** Aceptada — revisada durante T4.2.2  
**Proyecto:** TP Final — Programación Concurrente 2026  
**Alcance original:** T3.1.1, T3.1.2 y T3.1.3  
**Revisión experimental:** T4.2.2

---

## 1. Contexto

El sistema de procesamiento concurrente de pagos modela sus caminos de ejecución mediante una Red de Petri.

En el punto de encaminamiento existen tres transiciones alternativas:

| Transición | Camino |
|---|---|
| `T1` | Pago con tarjeta |
| `T4` | Pago de alto riesgo |
| `T6` | Transferencia bancaria |

Estas transiciones conforman el grupo de conflicto estructural:

```text
{T1, T4, T6}
```

El proyecto requiere comparar dos estrategias de resolución del conflicto:

```text
RANDOM
    -> selección pseudoaleatoria entre las alternativas del conflicto

PRIORITY
    -> prioridad para el flujo de alto riesgo iniciado por T4
```

La transición `T4` tiene una característica estructural importante:

```text
T1 necesita P7

T6 necesita P8

T4 necesita P7 + P8 simultáneamente
```

Por lo tanto, el flujo de alto riesgo necesita que ambos recursos compartidos se encuentren disponibles al mismo tiempo.

Esta propiedad terminó siendo relevante para el comportamiento práctico de la política priorizada.

---

## 2. Separación de responsabilidades

Se mantiene la siguiente separación general:

```text
PetriNet
    -> marcado
    -> sensibilización
    -> disparo formal de transiciones

ConflictGroup
    -> identifica qué transiciones pertenecen
       al mismo conflicto estructural

Policy
    -> decide qué solicitud debe recibir preferencia
       dentro de un conflicto

Monitor
    -> exclusión mutua
    -> espera
    -> temporalidad
    -> arbitraje
    -> reactivación de threads
```

El `Monitor` debe permanecer agnóstico respecto del significado concreto de la Red de Petri.

Por lo tanto:

```text
Monitor NO conoce T4 como "alto riesgo"
Monitor NO conoce P7
Monitor NO conoce P8
Monitor NO conoce conceptos de negocio
```

La transición prioritaria se configura externamente mediante `PriorityPolicy`.

---

## 3. Decisión original

La decisión original tomada durante T3.1 establecía que `Policy` solamente debía actuar entre transiciones que:

```text
- pertenecieran al mismo ConflictGroup;
- estuvieran actualmente habilitadas;
- tuvieran threads esperando.
```

El esquema era:

```text
PetriNet
    -> determina qué transiciones están sensibilizadas

Monitor
    -> detecta cuáles están habilitadas y esperando

Policy
    -> selecciona entre esas candidatas
```

También se decidió originalmente:

```text
- no mantener grants persistentes;
- no reservar recursos;
- no implementar aging;
- no implementar round-robin;
- no imponer cuotas mínimas;
- no convertir Policy en un scheduler global.
```

La intención era mantener una implementación simple y una separación estricta entre la lógica formal de la Red de Petri y la política de selección.

---

## 4. RandomPolicy

`RandomPolicy` selecciona pseudoaleatoriamente una posición dentro de la lista de candidatas recibidas.

Si recibe:

```text
[T1, T4, T6]
```

cada posición posee probabilidad teórica:

```text
1 / 3
```

en esa invocación.

La política puede recibir una `seed` para hacer reproducible su secuencia pseudoaleatoria cuando recibe exactamente la misma secuencia de listas de candidatas.

Sin embargo:

```text
misma seed
    !=
misma ejecución concurrente completa
```

El scheduling de threads, la temporalidad y el marcado pueden modificar qué candidatos aparecen y en qué orden se producen las decisiones.

Por lo tanto, una misma seed no garantiza exactamente la misma distribución final de invariantes.

---

## 5. PriorityPolicy

`PriorityPolicy` recibe externamente:

```text
priorityTransition
fallbackPolicy
```

En la configuración oficial:

```text
priorityTransition = T4
fallbackPolicy = RandomPolicy
```

La regla es:

```text
si la transición prioritaria pertenece a las candidatas:
    seleccionarla

en caso contrario:
    delegar en fallbackPolicy
```

No existe conocimiento hardcodeado de `T4` dentro de la implementación genérica de la política.

---

## 6. Fairness y starvation

Se mantiene la decisión de NO implementar mecanismos adicionales de fairness como:

```text
- aging;
- cuotas mínimas;
- round-robin obligatorio;
- porcentajes mínimos por camino;
- alternancia forzada.
```

Por lo tanto, `PriorityPolicy` representa una prioridad estricta.

Si la transición prioritaria se encuentra solicitada de manera sostenida, las alternativas de menor prioridad pueden experimentar starvation.

Esta posibilidad se considera aceptada como consecuencia de la política elegida.

Sin embargo, aceptar starvation no significa que la transición prioritaria pueda quedar sistemáticamente excluida por el scheduler de threads.

La política priorizada debe tener un efecto observable sobre el conflicto que pretende resolver.

---

# 7. Evidencia experimental que motivó la revisión

Durante T4.2.2 se realizaron campañas experimentales de 200 invariantes.

Las corridas eran formalmente correctas:

```text
Admissions = 200
Completions = 200

P-invariants valid = true
T-invariants valid = true
Workers terminated = true
Successful = true
```

Sin embargo, las distribuciones observadas mostraron que las políticas no estaban teniendo el efecto esperado.

---

## 8. Comportamiento observado antes de la corrección

### 8.1 RANDOM

En distintas corridas se obtuvieron distribuciones como:

```text
130 / 0 / 70
132 / 0 / 68
0 / 200 / 0
130 / 0 / 70
```

correspondientes a:

```text
IT1 / IT2 / IT3
```

En varias ejecuciones completas `IT2` resultó igual a cero.

La distribución parecía estar fuertemente condicionada por el scheduling de los Workers.

---

### 8.2 PRIORITY

El problema resultó aún más evidente con la política priorizada.

En las 10 corridas de la primera campaña formal:

```text
IT2 = 0
```

en todas las ejecuciones.

Acumulado:

```text
IT1 = 1260 / 2000 = 63.0 %
IT2 =    0 / 2000 =  0.0 %
IT3 =  740 / 2000 = 37.0 %
```

Por lo tanto:

```text
PriorityPolicy configurada para priorizar T4
        ↓
0 ejecuciones del flujo iniciado por T4
```

La política formalmente existía, pero no estaba produciendo el efecto experimental esperado.

Esto motivó la revisión de la decisión original.

---

# 9. Primer problema identificado: bypass de Policy

Se detectó que una transición podía ingresar a:

```java
Monitor.fireTransition(...)
```

y, si `canFireNow(transition)` retornaba `true`, continuar directamente hasta:

```java
PetriNet.fire(...)
```

sin competir mediante `Policy` contra otras solicitudes habilitadas y pendientes del mismo grupo de conflicto.

En ese escenario podía ocurrir:

```text
scheduler de Java
        ↓
thread obtiene primero el lock
        ↓
transición dispara
```

en lugar de:

```text
solicitudes del conflicto
        ↓
Policy
        ↓
transición seleccionada
```

La adquisición del lock estaba sustituyendo parcialmente la responsabilidad de la política.

---

## 10. Test de regresión para el bypass

Se incorporó:

```text
MonitorConflictArbitrationTest
```

con un escenario determinista en el cual:

```text
T2 ya tiene una solicitud esperando

T1 y T2 se encuentran habilitadas

llega una nueva solicitud T1
```

La política del test selecciona `T2`.

El comportamiento esperado es:

```text
T1 debe esperar
T2 debe disparar
```

Antes de la corrección se obtuvo:

```text
FAIL

Non-selected conflict transition should wait
expected: true
actual: false
```

Esto confirmó que una solicitud podía saltear la `Policy`.

Después de corregir el arbitraje directo:

```text
PASS
```

---

# 11. La primera corrección no fue suficiente

Después de impedir que una transición habilitada salteara directamente la `Policy`, se repitió:

```text
PRIORITY + BASELINE + 200
```

El resultado continuó siendo:

```text
IT1 = 129
IT2 =   0
IT3 =  71
```

Por lo tanto, existía un segundo problema diferente.

---

# 12. Segundo problema: T4 necesita dos recursos simultáneamente

La transición `T4` necesita:

```text
P7 + P8
```

simultáneamente.

Mientras `T4` esperaba esos recursos, `T1` o `T6` podían volver a consumir alguno de ellos.

Podía producirse repetidamente un comportamiento equivalente a:

```text
T4 espera P7 + P8

P7 queda libre
    ↓
T1 vuelve a consumir P7

P8 queda libre
    ↓
T6 vuelve a consumir P8

T4 continúa esperando
```

La transición prioritaria podía permanecer esperando indefinidamente sin llegar a encontrarse habilitada.

La decisión original de considerar solamente transiciones actualmente sensibilizadas impedía que `PriorityPolicy` protegiera efectivamente la solicitud de `T4`.

---

# 13. Segundo test de regresión

Se agregó otro escenario a:

```text
MonitorConflictArbitrationTest
```

El caso representa:

```text
T1
    -> alternativa normal
    -> requiere un subconjunto de recursos

T2
    -> alternativa prioritaria
    -> requiere más de un recurso simultáneamente
```

La solicitud prioritaria entra primero, pero inicialmente todavía no posee todos los recursos necesarios.

Luego ingresa una alternativa de menor prioridad que sí podría dispararse.

El comportamiento esperado es:

```text
la alternativa de menor prioridad debe esperar

la solicitud prioritaria conserva el turno

cuando se libera el recurso faltante:
    la prioritaria dispara
```

Antes de implementar el grant se obtuvo:

```text
FAIL

Lower-priority transition should wait
expected: true
actual: false
```

Esto reprodujo determinísticamente el mismo fenómeno observado en las campañas experimentales.

---

# 14. Decisión revisada: grant lógico persistente

Se decide introducir un:

```text
grant lógico por grupo de conflicto
```

Cuando existen varias solicitudes pendientes dentro de un `ConflictGroup`, `Policy` puede seleccionar cuál obtiene temporalmente el derecho de continuar dentro de ese conflicto.

Ejemplo:

```text
ConflictGroup = {T1, T4, T6}
```

Si `PriorityPolicy` selecciona `T4`:

```text
grant = T4
```

entonces:

```text
T1 no puede adelantarse
T6 no puede adelantarse
```

mientras la solicitud seleccionada sigue pendiente.

---

# 15. El grant NO modifica la Red de Petri

El grant es exclusivamente un mecanismo lógico del `Monitor`.

No significa:

```text
reservar físicamente tokens
```

ni:

```text
alterar el marcado
```

ni:

```text
forzar una transición no sensibilizada
```

La transición seleccionada continúa obligada a cumplir:

```java
PetriNet.isEnabled(transition)
```

antes de realizar el disparo real.

Por lo tanto:

```text
Policy
    -> selecciona quién posee el turno del conflicto

Grant
    -> evita que otra alternativa se adelante

PetriNet
    -> decide si la transición está formalmente habilitada
```

La semántica formal de la Red de Petri permanece intacta.

---

# 16. Modificación del concepto de candidata

La decisión original establecía:

```text
Policy recibe únicamente solicitudes:
    pendientes
    +
    actualmente habilitadas
```

La decisión revisada establece:

```text
Policy puede arbitrar solicitudes:
    pendientes
    +
    disponibles para la ejecución
```

aunque alguna de ellas todavía se encuentre esperando que se liberen los recursos necesarios para quedar sensibilizada.

Esto permite que una transición prioritaria conserve temporalmente el turno del conflicto.

Sin embargo:

```text
ser candidata de Policy
    !=
poder dispararse
```

El disparo continúa dependiendo de:

```text
canFireNow(...)
        ↓
PetriNet.isEnabled(...)
```

---

# 17. Duración del grant

El grant pertenece a una solicitud concreta.

Se mantiene hasta que:

```text
1. la transición seleccionada dispara exitosamente;

2. la solicitud seleccionada es abortada;

3. la ejecución determina que la solicitud no puede continuar.
```

Después:

```text
grant = none
```

y el siguiente conflicto vuelve a ser resuelto mediante `Policy`.

No existe una reserva permanente entre distintas operaciones.

---

# 18. Primer intento de grant y aparición de deadlock

La primera implementación del grant reveló una interacción importante con la prioridad entre transiciones inmediatas y temporales.

Se produjo el siguiente ciclo:

```text
T4 posee el grant
        ↓
T1/T6 quedan bloqueadas por el grant
```

pero al mismo tiempo:

```text
T1/T6 seguían siendo consideradas
transiciones inmediatas habilitadas
```

Por lo tanto, una transición temporal necesaria para completar una operación y liberar P7/P8 podía quedar bloqueada.

El resultado observado en `ExperimentRunnerTest` fue:

```text
Experimental run timed out before reaching target

target = 20
admissions = 3
completions = 0
```

Esto confirmó la existencia de un interbloqueo introducido por la primera versión del mecanismo.

---

# 19. Integración correcta entre grant y temporalidad

Para resolver el deadlock se modificó la definición utilizada por:

```text
hasEnabledImmediateTransition()
```

Una transición inmediata solamente puede bloquear a una transición temporal cuando:

```text
- está sensibilizada;
- está disponible;
- está autorizada por el grant de su conflicto.
```

Por lo tanto, una transición inmediata que perdió el conflicto no puede impedir que una transición temporal termine el trabajo necesario para liberar recursos.

El escenario deja de ser:

```text
inmediata pierde grant
        ↓
bloquea temporal
        ↓
temporal no libera recurso
        ↓
transición con grant nunca se habilita
```

y pasa a:

```text
inmediata pierde grant
        ↓
temporal puede finalizar
        ↓
libera recursos
        ↓
transición con grant puede habilitarse
```

---

# 20. Estado interno de grants

El `Monitor` mantiene un grant por grupo de conflicto.

Conceptualmente:

```text
conflictGrants[i]
```

puede contener:

```text
-1
    -> no existe grant

transition
    -> transición actualmente seleccionada
       dentro de ese grupo
```

Los grupos de conflicto son validados como disjuntos.

Por lo tanto, una transición no pertenece simultáneamente a múltiples grants.

---

# 21. Policy continúa sin ser un scheduler global

La introducción del grant NO convierte a `Policy` en un scheduler general de toda la Red de Petri.

Policy solamente participa dentro de los grupos configurados.

En el modelo oficial:

```text
Policy
    -> {T1, T4, T6}
```

Las transiciones independientes siguen ejecutándose sin pasar por la política.

Esto es particularmente importante para las transiciones que completan operaciones y liberan recursos.

---

# 22. Regresión del Monitor y del Runner

Después de introducir el grant explícito y corregir su interacción con la temporalidad se ejecutaron específicamente:

```text
MonitorTest
MonitorConflictArbitrationTest
ExperimentRunnerTest
```

Resultado:

```text
Tests run: 21
Failures: 0
Errors: 0
Skipped: 0
```

Distribución:

```text
MonitorTest                     16
MonitorConflictArbitrationTest   2
ExperimentRunnerTest             3
----------------------------------
TOTAL                            21
```

Posteriormente se ejecutó toda la suite del proyecto.

Resultado:

```text
Tests run: 171
Failures: 0
Errors: 0
Skipped: 0
```

Esto verificó que el nuevo mecanismo no rompiera los contratos existentes de:

```text
PetriNet
Monitor
Workers
Policies
Temporalidad
Logging
Verificación
ExperimentRunner
Campañas
```

---

# 23. Evidencia experimental posterior a la corrección

Una vez estabilizado el mecanismo de grant se repitieron las calibraciones experimentales.

---

## 23.1 RANDOM + BASELINE

Resultado:

```text
Admissions: 200
Completions: 200

IT1: 83
IT2: 57
IT3: 60

Total T-invariants: 200

Duration: 31.526 s

P-invariants valid: true
T-invariants valid: true
Workers terminated: true
Successful: true
```

Distribución:

```text
IT1 = 41.5 %
IT2 = 28.5 %
IT3 = 30.0 %
```

La política aleatoria produce ahora operaciones en los tres caminos.

---

## 23.2 PRIORITY + BASELINE

Resultado:

```text
Admissions: 200
Completions: 200

IT1: 54
IT2: 100
IT3: 46

Total T-invariants: 200

Duration: 36.490 s

P-invariants valid: true
T-invariants valid: true
Workers terminated: true
Successful: true
```

Distribución:

```text
IT1 = 27.0 %
IT2 = 50.0 %
IT3 = 23.0 %
```

La política priorizada produce ahora un efecto observable sobre el camino de alto riesgo.

Comparación:

```text
RANDOM
IT2 = 28.5 %

PRIORITY
IT2 = 50.0 %
```

Por lo tanto:

```text
PriorityPolicy
        ↓
incrementa efectivamente
la participación de IT2
```

---

# 24. Consecuencia sobre los tiempos experimentales

El cambio en la distribución de flujos modifica también la carga efectiva sobre los recursos compartidos.

Antes de la corrección, la política PRIORITY casi nunca ejecutaba el flujo de alto riesgo.

Después de la corrección, dicho flujo representa aproximadamente el 50 % de las operaciones en la calibración realizada.

Como consecuencia, los tiempos reales de ejecución aumentaron.

Ejemplo:

```text
PRIORITY + BASELINE

antes:
aproximadamente 28 s
IT2 = 0

después:
36.490 s
IT2 = 100
```

Por lo tanto fue necesario recalibrar la configuración temporal experimental.

---

# 25. Recalibración de VARIANT

Antes de la corrección se había considerado:

```text
VARIANT_SCALE = 1.20
```

Sin embargo, después de corregir el arbitraje:

```text
PRIORITY + BASELINE
    = 36.490 s
```

Aplicar un factor aproximado de `1.20` podía llevar la ejecución fuera del rango requerido de:

```text
20 s <= duration <= 40 s
```

Por lo tanto se realizó una nueva calibración.

La configuración definitiva adoptada es:

```text
VARIANT_SCALE = 1.05
```

---

# 26. Calibración definitiva

Los resultados obtenidos después de la corrección son:

| Política | Timing | IT1 | IT2 | IT3 | Duración |
|---|---:|---:|---:|---:|---:|
| RANDOM | BASELINE | 83 | 57 | 60 | 31.526 s |
| RANDOM | VARIANT 1.05 | 82 | 57 | 61 | 33.398 s |
| PRIORITY | BASELINE | 54 | 100 | 46 | 36.490 s |
| PRIORITY | VARIANT 1.05 | 54 | 100 | 46 | 38.422 s |

En las cuatro calibraciones:

```text
Admissions = 200
Completions = 200
Total T-invariants = 200

P-invariants valid = true
T-invariants valid = true
Workers terminated = true
Successful = true
```

Además:

```text
20 s <= duration <= 40 s
```

en todos los casos.

Por lo tanto se congela:

```text
VARIANT_SCALE = 1.05
```

para la campaña formal definitiva.

---

# 27. Campaña experimental previa a la corrección

La primera campaña formal ejecutada antes de introducir el grant produjo:

```text
20 corridas
20 SUCCESS
0 INVALID_RESULT
0 ERROR
```

Desde el punto de vista de invariantes y finalización, las corridas eran válidas.

Sin embargo, la distribución mostró que las políticas no estaban siendo representadas correctamente.

Especialmente:

```text
PRIORITY
IT2 = 0
```

en las 10 corridas correspondientes.

Por lo tanto, esa campaña se considera:

```text
evidencia diagnóstica pre-corrección
```

y NO debe utilizarse como campaña formal definitiva para analizar el efecto de las políticas.

La campaña formal debe repetirse utilizando:

```text
Monitor corregido
+
grant lógico explícito
+
VARIANT_SCALE = 1.05
```

---

# 28. Consecuencias positivas de la decisión

La decisión revisada permite:

```text
- que Policy gobierne efectivamente los conflictos configurados;

- que el scheduler de Java no sustituya silenciosamente
  la decisión de Policy;

- que PriorityPolicy produzca una prioridad observable para T4;

- que RandomPolicy distribuya operaciones por los tres caminos;

- conservar PetriNet como autoridad sobre la sensibilización;

- evitar modificar físicamente tokens para implementar prioridad;

- mantener Monitor independiente del modelo concreto;

- integrar correctamente prioridad, temporalidad y reactivación.
```

---

# 29. Costos y limitaciones

El nuevo diseño introduce mayor complejidad interna.

El `Monitor` ahora mantiene:

```text
estado de grant por ConflictGroup
```

y debe integrar dicho estado con:

```text
canFireNow(...)
hasEnabledImmediateTransition(...)
signalEnabledWaitingTransitions(...)
interrupciones
esperas temporales
```

Además:

```text
PriorityPolicy continúa sin garantizar fairness.
```

Una alternativa de menor prioridad puede experimentar starvation si la transición prioritaria es solicitada continuamente.

Esta consecuencia se mantiene aceptada como parte de la prioridad estricta.

---

# 30. Decisión final

Se adopta definitivamente la siguiente arquitectura:

```text
ConflictGroup
    -> identifica alternativas estructuralmente relacionadas


Policy
    -> selecciona entre solicitudes pendientes
       pertenecientes al conflicto


Grant lógico
    -> conserva temporalmente el turno
       de la solicitud seleccionada

    -> no modifica tokens

    -> no altera el marcado

    -> no fuerza disparos inválidos


PetriNet
    -> mantiene marcado

    -> determina sensibilización

    -> ejecuta el disparo formal


Monitor
    -> exclusión mutua

    -> espera

    -> arbitraje

    -> grants

    -> temporalidad

    -> reactivación


RandomPolicy
    -> selección pseudoaleatoria


PriorityPolicy
    -> prioridad estricta configurada externamente

    -> fallback pseudoaleatorio

    -> posible starvation
```

La decisión original:

```text
"Policy solamente participa entre transiciones
actualmente habilitadas y no se mantiene ningún grant"
```

queda reemplazada por:

```text
"Policy puede arbitrar solicitudes pendientes
de un ConflictGroup y la selección puede mantenerse
mediante un grant lógico hasta que la solicitud elegida
pueda dispararse formalmente."
```

El grant:

```text
NO reserva físicamente recursos.

NO modifica el marcado.

NO permite disparar una transición no sensibilizada.
```

Su única responsabilidad es:

```text
evitar que otra alternativa del mismo conflicto
se adelante a la solicitud seleccionada por Policy.
```

Esta revisión queda respaldada por:

```text
- evidencia experimental pre-corrección;

- dos tests de regresión específicos;

- detección y corrección del deadlock temporal;

- 171 tests del proyecto exitosos;

- nuevas calibraciones de RANDOM;

- nuevas calibraciones de PRIORITY;

- verificación de P-invariantes;

- verificación de T-invariantes;

- finalización correcta de Workers;

- tiempos dentro del rango experimental requerido.
```

Por lo tanto, esta ADR reemplaza la decisión anterior respecto de la ausencia de grants persistentes y constituye la definición vigente para la resolución de conflictos del sistema.
