# Protocolo experimental

## 1. Objetivo

Este documento define el protocolo utilizado para ejecutar de forma reproducible las campañas experimentales del sistema de procesamiento concurrente de transacciones de pago.

El protocolo establece:

- cuándo se considera completado un T-invariante;
- cuándo termina una corrida;
- qué políticas se comparan;
- qué configuraciones temporales se utilizan;
- qué seeds se utilizan;
- qué métricas deben conservarse;
- cómo se identifican y almacenan los resultados de cada ejecución.

Este documento corresponde a `T4.2.1` y constituye el contrato para las campañas de `T4.2.2` y su posterior análisis en `T4.2.3`.

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

---

## 3. Regla de finalización

Cada corrida experimental debe finalizar cuando se hayan completado:

```text
IT1 + IT2 + IT3 = 200
```

T-invariantes.

La condición de parada depende de la cantidad total de invariantes completados y no de un tipo particular.

El sistema no debe iniciar nuevos ciclos una vez solicitado el cierre.

Los ciclos que ya hayan comenzado deben finalizar de acuerdo con la estrategia de terminación implementada por los workers, evitando abandonar operaciones lógicas a mitad de ejecución.

Al finalizar la corrida no deben quedar threads activos.

---

## 4. Políticas experimentales

Se evaluarán de manera independiente las dos políticas definidas para el sistema.

### RANDOM

Política aleatoria.

Los conflictos se resuelven seleccionando de forma aleatoria entre las alternativas disponibles.

La seed de la corrida debe quedar registrada para permitir reproducibilidad.

### PRIORITY

Política priorizada.

Cuando participa en un conflicto, se prioriza el flujo de pago de alto riesgo correspondiente a IT2.

---

## 5. Transiciones temporales

Las transiciones temporales de la red son:

```text
T2
T3
T5
T7
T8
```

Las restantes transiciones son inmediatas.

---

## 6. Configuración temporal BASELINE

La configuración base implementada actualmente es:

| Transición | Delay |
| --- | ---: |
| T2 | 120 ms |
| T3 | 80 ms |
| T5 | 150 ms |
| T7 | 100 ms |
| T8 | 120 ms |

Las transiciones:

```text
T0 T1 T4 T6 T9
```

poseen delay `0 ms`.

Esta configuración se identifica como:

```text
BASELINE
```

---

## 7. Configuración temporal VARIANT

Para estudiar el efecto de modificar la temporalidad se define una segunda configuración experimental.

Valores iniciales propuestos:

| Transición | Delay |
| --- | ---: |
| T2 | 145 ms |
| T3 | 95 ms |
| T5 | 180 ms |
| T7 | 120 ms |
| T8 | 145 ms |

Esta configuración se identifica como:

```text
VARIANT
```

Los valores deberán validarse mediante una corrida de calibración antes de iniciar las campañas oficiales.

Si la duración total queda fuera del rango requerido de 20 a 40 segundos, los valores podrán ajustarse antes de ejecutar la campaña formal.

Cualquier ajuste deberá quedar documentado y mantenerse fijo durante todas las corridas oficiales.

---

## 8. Seeds

Para las campañas formales se utilizarán cinco seeds:

```text
2026
2027
2028
2029
2030
```

La misma colección de seeds debe utilizarse para todas las combinaciones de política y configuración temporal.

Esto permite realizar comparaciones utilizando condiciones reproducibles.

---

## 9. Matriz experimental

Se evaluarán las siguientes combinaciones:

| Política | Temporalidad | Seeds | Corridas |
| --- | --- | --- | ---: |
| RANDOM | BASELINE | 2026-2030 | 5 |
| RANDOM | VARIANT | 2026-2030 | 5 |
| PRIORITY | BASELINE | 2026-2030 | 5 |
| PRIORITY | VARIANT | 2026-2030 | 5 |

Cantidad total prevista:

```text
2 políticas
x
2 configuraciones temporales
x
5 seeds
=
20 corridas
```

Cada corrida debe alcanzar 200 T-invariantes completados.

---

## 10. Identificación de una corrida

Cada corrida debe poder identificarse de forma inequívoca mediante:

```text
POLICY
TIMING
SEED
```

Ejemplo:

```text
RANDOM_BASELINE_2026
PRIORITY_VARIANT_2028
```

---

## 11. Archivos de log

Los logs deberán almacenarse utilizando la convención:

```text
results/logs/<POLICY>_<TIMING>_<SEED>.log
```

Ejemplos:

```text
results/logs/RANDOM_BASELINE_2026.log
results/logs/PRIORITY_VARIANT_2028.log
```

El formato interno de los archivos corresponde al contrato definido en:

```text
docs/log-format.md
```

---

## 12. Resultados

Los resultados resumidos de cada corrida deberán almacenarse de forma que puedan ser procesados posteriormente.

Convención propuesta:

```text
results/summary/<POLICY>_<TIMING>_<SEED>.txt
```

También podrá utilizarse un formato estructurado equivalente definido por el runner experimental.

---

## 13. Métricas por corrida

Cada corrida debe conservar como mínimo:

| Métrica | Descripción |
| --- | --- |
| policy | Política utilizada |
| seed | Seed utilizada |
| timing | Configuración temporal |
| IT1 | Cantidad de invariantes de tarjeta |
| IT2 | Cantidad de invariantes de alto riesgo |
| IT3 | Cantidad de invariantes de transferencia |
| total | Cantidad total de T-invariantes |
| duration | Duración total de la corrida |
| pInvariantsValid | Resultado de verificación de P-invariantes |
| tInvariantsValid | Resultado del análisis del log |
| logFile | Archivo de log correspondiente |

La condición esperada para todas las corridas válidas es:

```text
IT1 + IT2 + IT3 = 200
```

---

## 14. Medición temporal

La duración de una corrida se medirá utilizando un reloj monotónico.

Para medir intervalos de ejecución se deberá preferir:

```java
System.nanoTime()
```

sobre:

```java
System.currentTimeMillis()
```

El tiempo total deberá reportarse finalmente en milisegundos o segundos.

El timestamp almacenado por el logger cumple una función de trazabilidad y no reemplaza la medición monotónica utilizada para calcular la duración de la corrida.

---

## 15. Requisito de duración

Las ejecuciones utilizadas para el análisis final deberán respetar el rango:

```text
20 s <= duración <= 40 s
```

Antes de iniciar las campañas oficiales se realizará una calibración de las configuraciones temporales.

La configuración definitiva utilizada en las campañas deberá quedar fija una vez terminada dicha calibración.

---

## 16. Validaciones posteriores

Al terminar cada corrida se deberá comprobar:

```text
IT1 + IT2 + IT3 = 200
```

Además:

```text
P-invariantes válidos
T-invariantes válidos
sin threads activos
log generado correctamente
```

El análisis de T-invariantes deberá realizarse sobre el archivo de log mediante el analizador desarrollado en `T4.1.3`.

---

## 17. Comparación entre políticas

La campaña deberá permitir comparar la distribución:

```text
IT1 / 200
IT2 / 200
IT3 / 200
```

entre `RANDOM` y `PRIORITY`.

En particular, la política priorizada deberá analizarse respecto del flujo de alto riesgo correspondiente a IT2.

No se fijará de antemano una distribución numérica esperada. Las conclusiones se obtendrán a partir de los resultados observados.

---

## 18. Comparación temporal

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
```

---

## 19. Reproducibilidad

Una corrida queda definida por:

```text
POLICY + TIMING + SEED + TARGET
```

donde:

```text
TARGET = 200
```

Utilizando esos parámetros debe ser posible repetir el mismo experimento sin modificar manualmente el código fuente.

La selección de estos parámetros será responsabilidad del runner experimental.

---

## 20. Fases experimentales

Las ejecuciones se separarán en dos fases.

### Calibración

Se utiliza para comprobar que las configuraciones temporales permiten mantener la duración dentro del rango requerido.

Los resultados de calibración no forman parte de la comparación formal.

### Campaña formal

Una vez fijados los tiempos definitivos, se ejecutan las 20 combinaciones establecidas en la matriz experimental.

Durante esta fase no se modifican:

```text
delays
seeds
regla de parada
formato del log
métricas
```

---

## 21. Criterio de aceptación de una corrida

Una corrida se considera válida para el análisis solamente si:

```text
total de T-invariantes = 200
P-invariantes válidos
T-invariantes válidos
log completo y parseable
terminación sin threads activos
```

Una corrida que no cumpla estas condiciones deberá registrarse como fallida y no reemplazarse silenciosamente.

Si se repite, tanto la ejecución fallida como su repetición deberán quedar identificadas.

---

## 22. Relación con las siguientes tareas

Este protocolo constituye la entrada para:

```text
T4.2.2 — Ejecutar campañas por política y configuración temporal
```

Los resultados obtenidos serán posteriormente utilizados por:

```text
T4.2.3 — Interpretar distribución, invariantes y tiempos
```