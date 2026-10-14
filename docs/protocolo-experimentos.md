# Protocolo experimental

**Estado:** Vigente  
**Proyecto:** TP Final — Programación Concurrente 2026  
**Tarea de origen:** T4.2.1  
**Aplicación:** T4.2.2  
**Análisis posterior:** T4.2.3  

---

## 1. Objetivo

Este documento define el protocolo utilizado para ejecutar de forma reproducible las campañas experimentales del sistema de procesamiento concurrente de transacciones de pago.

El protocolo establece:

- cuándo se considera completado un T-invariante;
- cuándo termina una corrida;
- qué políticas se comparan;
- qué configuraciones temporales se utilizan;
- qué seeds se utilizan;
- qué métricas deben conservarse;
- cómo se identifican y almacenan los resultados;
- cómo se realiza la calibración temporal;
- qué condiciones debe cumplir una corrida para considerarse válida;
- cómo se separan las corridas de calibración de la campaña formal;
- qué evidencia experimental debe conservarse.

Este documento corresponde originalmente a `T4.2.1` y constituye el contrato para las campañas de `T4.2.2` y su posterior análisis en `T4.2.3`.

Durante `T4.2.2` el protocolo fue actualizado a partir de la evidencia experimental obtenida durante la integración del arbitraje de conflictos del `Monitor`.

---

## 2. Unidad experimental

La unidad de conteo utilizada por una corrida es un T-invariante completo.

Los T-invariantes definidos para la Red de Petri son:

### IT1 — Pago con tarjeta

```text
T0 -> T1 -> T2 -> T3 -> T9
```

### IT2 — Pago de alto riesgo

```text
T0 -> T4 -> T5 -> T9
```

### IT3 — Transferencia bancaria

```text
T0 -> T6 -> T7 -> T8 -> T9
```

Un T-invariante se considera completado únicamente cuando se completa uno de estos tres caminos.

Por lo tanto:

```text
IT1
    -> cantidad de pagos con tarjeta completados

IT2
    -> cantidad de pagos de alto riesgo completados

IT3
    -> cantidad de transferencias bancarias completadas
```

---

## 3. Regla de finalización

Cada corrida experimental debe finalizar cuando se hayan completado:

```text
IT1 + IT2 + IT3 = 200
```

T-invariantes.

La condición de parada depende de la cantidad total de invariantes completados y no de un tipo particular.

En una corrida válida deben cumplirse además:

```text
Admissions = 200
Completions = 200
```

El sistema no debe admitir nuevas operaciones una vez alcanzado el objetivo.

Los ciclos que ya hayan comenzado deben finalizar de acuerdo con la estrategia de terminación implementada por los Workers, evitando abandonar operaciones lógicas a mitad de ejecución.

Al finalizar la corrida no deben quedar threads activos.

---

## 4. Políticas experimentales

Se evaluarán de manera independiente dos políticas.

### 4.1 RANDOM

Política pseudoaleatoria.

Los conflictos se resuelven seleccionando pseudoaleatoriamente entre las solicitudes candidatas disponibles para el conflicto.

La seed de la corrida debe quedar registrada.

Una seed fija permite reproducir la secuencia pseudoaleatoria cuando se repite la misma secuencia de conjuntos de candidatos.

Sin embargo:

```text
misma seed
    !=
misma ejecución concurrente completa
```

El scheduling de threads, la temporalidad y el estado de la Red de Petri pueden modificar qué solicitudes aparecen y en qué orden se producen las decisiones.

No se exige que la distribución final sea exactamente:

```text
33.33 % / 33.33 % / 33.33 %
```

Sí se espera que la política permita participación de los tres caminos y que la distribución observada sea analizada experimentalmente.

---

### 4.2 PRIORITY

Política priorizada.

Se prioriza la transición:

```text
T4
```

correspondiente al inicio del flujo de pago de alto riesgo:

```text
IT2 = T0 -> T4 -> T5 -> T9
```

La configuración utilizada es conceptualmente:

```text
PriorityPolicy
    priorityTransition = T4
    fallbackPolicy = RandomPolicy
```

La prioridad es estricta.

Cuando la solicitud prioritaria participa del conflicto, puede conservar un grant lógico hasta que se encuentren disponibles los recursos necesarios para disparar formalmente.

El grant:

```text
NO modifica el marcado

NO reserva físicamente tokens

NO permite disparar una transición no sensibilizada
```

Su función es impedir que otra alternativa del mismo conflicto se adelante a la solicitud elegida por la política.

La decisión completa se encuentra documentada en:

```text
docs/ADR/Policy-Fairness.md
```

---

## 5. Conflicto experimental relevante

El grupo de conflicto utilizado para comparar políticas es:

```text
{T1, T4, T6}
```

donde:

```text
T1
    -> pago con tarjeta

T4
    -> pago de alto riesgo

T6
    -> transferencia bancaria
```

La estructura de recursos compartidos es relevante:

```text
T1 necesita P7

T6 necesita P8

T4 necesita P7 + P8 simultáneamente
```

Por este motivo la política PRIORITY debe analizarse especialmente mediante la cantidad de invariantes `IT2` completados.

---

## 6. Transiciones temporales

Las transiciones temporales de la Red de Petri son:

```text
T2
T3
T5
T7
T8
```

Las restantes transiciones son inmediatas:

```text
T0
T1
T4
T6
T9
```

---

## 7. Configuración temporal BASELINE

La configuración base es:

| Transición | Delay |
| --- | ---: |
| T2 | 120 ms |
| T3 | 80 ms |
| T5 | 150 ms |
| T7 | 100 ms |
| T8 | 120 ms |

Las transiciones inmediatas poseen:

```text
0 ms
```

Esta configuración se identifica como:

```text
BASELINE
```

y se construye mediante:

```java
PaymentTimingConfig.createBaseline()
```

---

## 8. Configuración temporal VARIANT

La configuración `VARIANT` se obtiene escalando la configuración `BASELINE`.

Durante la primera etapa experimental se había considerado:

```text
VARIANT_SCALE = 1.20
```

Sin embargo, luego de corregir el arbitraje de conflictos del `Monitor`, la distribución de trabajo cambió y también aumentaron los tiempos observados de las corridas PRIORITY.

Por ese motivo se realizó una nueva calibración.

La configuración definitiva es:

```text
VARIANT_SCALE = 1.05
```

y se construye mediante:

```java
PaymentTimingConfig.createScaled(1.05)
```

Los delays resultantes son:

| Transición | BASELINE | VARIANT 1.05 |
| --- | ---: | ---: |
| T2 | 120 ms | 126 ms |
| T3 | 80 ms | 84 ms |
| T5 | 150 ms | 158 ms |
| T7 | 100 ms | 105 ms |
| T8 | 120 ms | 126 ms |

Esta configuración deberá permanecer fija durante toda la campaña formal definitiva.

---

## 9. Evidencia de calibración definitiva

Después de estabilizar el arbitraje de conflictos y ejecutar la suite completa se realizaron cuatro corridas de calibración.

Todas utilizaron:

```text
TARGET = 200
SEED = 2026
```

Los resultados fueron:

| Política | Timing | IT1 | IT2 | IT3 | Duración |
| --- | --- | ---: | ---: | ---: | ---: |
| RANDOM | BASELINE | 83 | 57 | 60 | 31.526 s |
| RANDOM | VARIANT 1.05 | 82 | 57 | 61 | 33.398 s |
| PRIORITY | BASELINE | 54 | 100 | 46 | 36.490 s |
| PRIORITY | VARIANT 1.05 | 54 | 100 | 46 | 38.422 s |

En todas las calibraciones se obtuvo:

```text
Admissions = 200
Completions = 200

IT1 + IT2 + IT3 = 200

P-invariants valid = true
T-invariants valid = true
Workers terminated = true
Successful = true
```

Además:

```text
20 s <= duración <= 40 s
```

en los cuatro escenarios.

Por lo tanto, se consideran calibradas y congeladas:

```text
BASELINE
VARIANT_SCALE = 1.05
```

---

## 10. Interpretación mínima de la calibración de políticas

La calibración no reemplaza el análisis estadístico de `T4.2.3`.

Sin embargo, sirve como validación previa de que ambas políticas producen un comportamiento observable coherente con su objetivo.

### RANDOM + BASELINE

```text
IT1 = 83  -> 41.5 %
IT2 = 57  -> 28.5 %
IT3 = 60  -> 30.0 %
```

Los tres caminos participan de la ejecución.

### PRIORITY + BASELINE

```text
IT1 = 54   -> 27.0 %
IT2 = 100  -> 50.0 %
IT3 = 46   -> 23.0 %
```

El flujo correspondiente a `IT2` incrementa su participación respecto de RANDOM.

Esto confirma que la política priorizada produce un efecto observable antes de iniciar la campaña formal.

Las conclusiones definitivas sobre distribución corresponden a `T4.2.3`.

---

## 11. Seeds

Para la campaña formal se utilizarán cinco seeds:

```text
2026
2027
2028
2029
2030
```

La misma colección debe utilizarse para todas las combinaciones de política y configuración temporal.

Esto permite comparar escenarios manteniendo constante el conjunto de seeds.

---

## 12. Matriz experimental formal

Se evaluarán las siguientes combinaciones:

| Política | Temporalidad | Seeds | Corridas |
| --- | --- | --- | ---: |
| RANDOM | BASELINE | 2026-2030 | 5 |
| RANDOM | VARIANT | 2026-2030 | 5 |
| PRIORITY | BASELINE | 2026-2030 | 5 |
| PRIORITY | VARIANT | 2026-2030 | 5 |

Cantidad total:

```text
2 políticas
x
2 configuraciones temporales
x
5 seeds
=
20 corridas
```

Cada corrida debe alcanzar:

```text
200 T-invariantes completos
```

---

## 13. Orden de la campaña

La matriz formal se ejecutará en el siguiente orden:

```text
RANDOM + BASELINE + seeds 2026-2030

RANDOM + VARIANT + seeds 2026-2030

PRIORITY + BASELINE + seeds 2026-2030

PRIORITY + VARIANT + seeds 2026-2030
```

Esto produce:

```text
RANDOM_BASELINE_2026
RANDOM_BASELINE_2027
RANDOM_BASELINE_2028
RANDOM_BASELINE_2029
RANDOM_BASELINE_2030

RANDOM_VARIANT_2026
...
RANDOM_VARIANT_2030

PRIORITY_BASELINE_2026
...
PRIORITY_BASELINE_2030

PRIORITY_VARIANT_2026
...
PRIORITY_VARIANT_2030
```

---

## 14. Identificación de una corrida

Cada corrida se identifica mediante:

```text
POLICY_TIMING_SEED
```

Ejemplos:

```text
RANDOM_BASELINE_2026
PRIORITY_VARIANT_2028
```

La identidad lógica de una corrida también incluye:

```text
TARGET = 200
```

Por lo tanto, experimentalmente una ejecución queda definida por:

```text
POLICY
+
TIMING
+
SEED
+
TARGET
```

---

## 15. Runner experimental

Las corridas formales se ejecutan utilizando:

```text
ExperimentRunner
```

La campaña completa es coordinada mediante:

```text
CampaignPlan
CampaignExecutor
FormalCampaignMain
```

Responsabilidades:

```text
CampaignPlan
    -> construye la matriz de 20 configuraciones

CampaignExecutor
    -> ejecuta las corridas secuencialmente
    -> conserva SUCCESS / INVALID_RESULT / ERROR
    -> persiste resultados incrementalmente

FormalCampaignMain
    -> fija VARIANT_SCALE
    -> inicia la campaña formal
    -> presenta el resumen global
```

---

## 16. Archivos de log

Los logs de la campaña formal se almacenan utilizando:

```text
results/logs/<POLICY>_<TIMING>_<SEED>.log
```

Ejemplos:

```text
results/logs/RANDOM_BASELINE_2026.log

results/logs/PRIORITY_VARIANT_2028.log
```

El formato interno corresponde al contrato definido en:

```text
docs/log-format.md
```

---

## 17. Logs de calibración

Las corridas utilizadas únicamente para calibración se almacenan separadamente en:

```text
results/calibration/logs/
```

Ejemplos:

```text
results/calibration/logs/RANDOM_BASELINE_2026.log

results/calibration/logs/RANDOM_VARIANT_2026.log

results/calibration/logs/PRIORITY_BASELINE_2026.log

results/calibration/logs/PRIORITY_VARIANT_2026.log
```

Estos archivos no forman parte de las 20 corridas de la campaña formal.

---

## 18. Resultado consolidado

La campaña formal produce un archivo CSV consolidado:

```text
results/summary/formal-campaign.csv
```

El archivo posee una fila por intento de corrida.

Una campaña completa contiene:

```text
1 header
+
20 corridas
=
21 líneas
```

si todas las configuraciones fueron ejecutadas exactamente una vez.

---

## 19. Campos del resultado consolidado

El CSV registra:

```text
runId
status
policy
seed
timing
target
admissions
completions
it1
it2
it3
totalTInvariants
durationMillis
pInvariantsValid
tInvariantsValid
workersTerminated
logPath
successful
errorType
errorMessage
```

Esto permite analizar la campaña sin necesidad de volver a procesar todos los logs para obtener las métricas principales.

Los logs continúan siendo la evidencia detallada de la secuencia de disparos.

---

## 20. Estados de una corrida

Cada intento puede quedar registrado como:

```text
SUCCESS
INVALID_RESULT
ERROR
```

### SUCCESS

La corrida terminó y cumple todas las condiciones de éxito.

### INVALID_RESULT

La corrida produjo un `ExperimentRunResult`, pero alguna de las condiciones formales esperadas no se cumplió.

### ERROR

La ejecución no pudo producir un resultado normal debido a una excepción o falla.

Las corridas fallidas deben conservarse como evidencia.

No deben eliminarse ni reemplazarse silenciosamente.

---

## 21. Persistencia incremental

El archivo consolidado se actualiza después de cada intento de corrida.

Por lo tanto, si una campaña se interrumpe antes de completar las 20 configuraciones, los resultados ya obtenidos permanecen almacenados.

Esto evita perder toda la evidencia por una falla ocurrida en una corrida posterior.

---

## 22. Métricas por corrida

Cada corrida debe conservar como mínimo:

| Métrica | Descripción |
| --- | --- |
| runId | Identificador de la corrida |
| policy | Política utilizada |
| seed | Seed utilizada |
| timing | Configuración temporal |
| target | Cantidad objetivo de invariantes |
| admissions | Operaciones admitidas |
| completions | Operaciones completadas |
| IT1 | Invariantes de tarjeta |
| IT2 | Invariantes de alto riesgo |
| IT3 | Invariantes de transferencia |
| totalTInvariants | Total de invariantes |
| durationMillis | Duración monotónica |
| pInvariantsValid | Resultado de P-invariantes |
| tInvariantsValid | Resultado de T-invariantes |
| workersTerminated | Terminación de Workers |
| logPath | Archivo de log |
| status | Estado de la corrida |
| successful | Resultado global |

En caso de error también deben conservarse:

```text
errorType
errorMessage
```

---

## 23. Medición temporal

La duración de una corrida se mide utilizando un reloj monotónico:

```java
System.nanoTime()
```

No se utiliza:

```java
System.currentTimeMillis()
```

para calcular el intervalo total de ejecución.

`System.currentTimeMillis()` puede aparecer en el logger como timestamp de trazabilidad, pero no se utiliza para medir la duración experimental.

La duración del `ExperimentRunner` se mide desde inmediatamente antes de iniciar los Workers hasta alcanzar el objetivo de completions.

El overhead posterior de shutdown no forma parte de la duración experimental reportada.

---

## 24. Requisito temporal

Las ejecuciones utilizadas para el análisis final deben respetar:

```text
20 s <= duración <= 40 s
```

Este requisito se verifica por corrida individual.

No corresponde a la duración acumulada de las 20 corridas.

Por ejemplo:

```text
20 corridas
x
aproximadamente 30-40 segundos
```

pueden producir una campaña completa de varios minutos sin violar el requisito.

---

## 25. Verificación de P-invariantes

Después de cada disparo exitoso se verifica el marcado mediante:

```text
PlaceInvariantChecker
```

Los P-invariantes oficiales son:

```text
IP1
P2 + P3 + P4 + P7 = 1

IP2
P4 + P5 + P6 + P8 = 1

IP3
P0 + P1 + P2 + P3 + P4 + P5 + P6 + P9 = 3
```

Una violación invalida la corrida.

---

## 26. Verificación de T-invariantes

Al finalizar la corrida el archivo de log se analiza mediante:

```text
TInvariantAnalyzer
```

El análisis reconstruye los caminos de los Workers y contabiliza:

```text
IT1
IT2
IT3
```

La condición requerida es:

```text
IT1 + IT2 + IT3 = 200
```

Además:

```text
T0 = 200
T9 = 200
```

para una corrida formal completa.

---

## 27. Terminación de Workers

Una corrida válida debe terminar sin dejar Workers activos.

El resultado debe indicar:

```text
workersTerminated = true
```

El cierre se realiza después de alcanzar el target.

Si un Worker no puede finalizar dentro del timeout de shutdown, la corrida no debe considerarse exitosa.

---

## 28. Timeouts

La configuración formal utiliza:

```text
executionTimeout = 60 s
shutdownTimeout = 5 s
```

El límite experimental deseado continúa siendo:

```text
20-40 s
```

El timeout de 60 segundos no representa un tiempo aceptable de ejecución.

Su función es evitar que una corrida con falta de progreso permanezca bloqueada indefinidamente.

---

## 29. Criterio de éxito de una corrida

Una corrida se considera exitosa cuando se cumple:

```text
Admissions = 200

Completions = 200

IT1 + IT2 + IT3 = 200

P-invariantes válidos

T-invariantes válidos

Workers terminados

log generado correctamente
```

Además, para formar parte del análisis temporal final:

```text
20 s <= duración <= 40 s
```

---

## 30. Comparación entre políticas

La campaña debe permitir comparar:

```text
IT1 / 200
IT2 / 200
IT3 / 200
```

entre:

```text
RANDOM
vs
PRIORITY
```

La variable particularmente relevante es:

```text
IT2
```

porque representa el camino iniciado por la transición prioritaria `T4`.

No se fija de antemano un porcentaje exacto obligatorio.

El objetivo es determinar experimentalmente cómo cambia la distribución al reemplazar RANDOM por PRIORITY.

La interpretación estadística final pertenece a:

```text
T4.2.3
```

---

## 31. Comparación temporal

Para cada política se compararán:

```text
BASELINE
vs
VARIANT
```

Se analizarán como mínimo:

```text
duración total

distribución de IT1

distribución de IT2

distribución de IT3

efecto de los delays sobre el paralelismo

efecto de la política sobre la utilización
de los recursos compartidos
```

---

## 32. Reproducibilidad

Una corrida queda determinada por:

```text
POLICY
+
TIMING
+
SEED
+
TARGET
```

donde:

```text
TARGET = 200
```

La configuración de la campaña debe ser generada automáticamente.

No se deben modificar manualmente:

```text
seed
target
paths
policy
timing
```

entre corridas formales.

---

## 33. Fases experimentales

Las ejecuciones se dividen en:

```text
1. calibración

2. campaña formal

3. análisis
```

---

### 33.1 Calibración

Se utiliza para:

```text
verificar comportamiento de las políticas

comprobar invariantes

validar terminación

medir duración

fijar VARIANT_SCALE
```

Las corridas de calibración no forman parte de la muestra formal utilizada en `T4.2.3`.

---

### 33.2 Campaña formal

Una vez fijadas las configuraciones:

```text
BASELINE

VARIANT_SCALE = 1.05
```

se ejecutan las 20 combinaciones de la matriz experimental.

Durante esta fase no deben modificarse:

```text
Policy

VARIANT_SCALE

BASELINE

seeds

target

regla de parada

formato del log

métricas

mecanismo de arbitraje del Monitor
```

Si se modifica alguno de estos elementos, la campaña debe considerarse distinta y deberá repetirse.

---

### 33.3 Análisis

Los resultados de la campaña formal se utilizan posteriormente para:

```text
T4.2.3 — Interpretar distribución, invariantes y tiempos
```

Las conclusiones estadísticas o comparativas no forman parte de la ejecución de `T4.2.2`.

---

## 34. Primera campaña ejecutada antes de la corrección

Antes de corregir el arbitraje del `Monitor` se ejecutó una primera matriz completa de:

```text
20 corridas
```

El resultado técnico fue:

```text
SUCCESS = 20
INVALID_RESULT = 0
ERROR = 0
```

y todas las corridas cumplían:

```text
200 invariantes

P-invariantes válidos

T-invariantes válidos

Workers terminados
```

Sin embargo, la distribución reveló un problema.

En PRIORITY se obtuvo:

```text
IT2 = 0
```

en las 10 corridas correspondientes.

En RANDOM también se observaron resultados altamente dependientes del scheduling, incluyendo corridas con:

```text
IT2 = 0
```

y una corrida con:

```text
IT2 = 200
```

La investigación posterior demostró que el scheduler de threads podía sustituir parcialmente el arbitraje de `Policy`.

Por lo tanto, esta primera campaña se clasifica como:

```text
evidencia diagnóstica pre-corrección
```

y NO debe utilizarse como la campaña formal definitiva para `T4.2.3`.

---

## 35. Corrección entre campañas

La investigación de la primera campaña produjo:

```text
MonitorConflictArbitrationTest
```

con dos pruebas de regresión.

Se corrigieron:

```text
1. bypass de Policy por una transición
   que adquiría directamente el lock;

2. falta de persistencia de la prioridad
   mientras T4 esperaba recursos;

3. interacción entre grants y prioridad
   de transiciones inmediatas/temporales.
```

La solución definitiva utiliza:

```text
grant lógico por ConflictGroup
```

sin modificar el marcado de la Red de Petri.

La decisión de diseño se encuentra documentada en:

```text
docs/ADR/Policy-Fairness.md
```

---

## 36. Validación posterior a la corrección

Después de estabilizar el arbitraje se ejecutó la suite completa:

```text
Tests run: 171
Failures: 0
Errors: 0
Skipped: 0
```

También se repitieron las cuatro calibraciones oficiales.

Sus resultados constituyen la evidencia utilizada para congelar:

```text
VARIANT_SCALE = 1.05
```

antes de ejecutar la campaña formal definitiva.

---

## 37. Campaña formal definitiva

La campaña utilizada como evidencia final de `T4.2.2` debe ejecutarse utilizando exactamente:

```text
Monitor con grant lógico corregido

RANDOM

PRIORITY

BASELINE

VARIANT_SCALE = 1.05

seeds = 2026, 2027, 2028, 2029, 2030

TARGET = 200
```

Cantidad esperada:

```text
20 corridas
```

Cada una debe conservar:

```text
log individual

resultado consolidado

conteos IT1 / IT2 / IT3

duración

estado de invariantes

estado de Workers
```

---

## 38. Criterio de aceptación de la campaña

La campaña formal definitiva se considera correctamente ejecutada cuando:

```text
Recorded runs = 20

ERROR = 0

INVALID_RESULT = 0

SUCCESS = 20
```

y cada corrida cumple las validaciones formales definidas en este protocolo.

La evaluación de distribución y tiempos entre grupos pertenece a `T4.2.3`.

---

## 39. Evidencia que debe conservarse

Al cerrar `T4.2.2` deberán conservarse:

```text
docs/protocolo-experimentos.md

docs/ADR/Policy-Fairness.md

results/calibration/logs/

results/logs/

results/summary/formal-campaign.csv
```

Además del código correspondiente a:

```text
ExperimentRunner

CampaignRunRecord

CampaignSummaryWriter

CampaignPlan

CampaignExecutor

CalibrationMain

FormalCampaignMain
```

y sus tests.

---

## 40. Relación con las siguientes tareas

Este protocolo constituye la referencia para:

```text
T4.2.2 — Ejecutar campañas por política/timing
```

Los resultados definitivos serán utilizados por:

```text
T4.2.3 — Interpretar distribución, invariantes y tiempos
```

La tarea `T4.2.3` deberá trabajar exclusivamente con la campaña formal definitiva ejecutada después de:

```text
corrección del arbitraje
+
recalibración
+
VARIANT_SCALE = 1.05
```

y no con la campaña diagnóstica previa a la corrección.
