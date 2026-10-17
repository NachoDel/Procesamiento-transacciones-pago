# Modelo temporal analítico

## Objetivo

Documentar la configuración temporal inicial de la Red de Petri
del sistema de procesamiento de pagos y construir una estimación
analítica de la duración de una corrida.

Este documento corresponde a la tarea T3.2.1.

El modelo se elaboró originalmente antes de la campaña experimental.
La regla de cierre de 200 T-invariantes y la comparación experimental
posterior ya se encuentran implementadas y documentadas en
[`../protocolo-experimentos.md`](../protocolo-experimentos.md) y
[`../analisis-T4.2.3.md`](../analisis-T4.2.3.md).

---

## Transiciones temporales

De acuerdo con el modelo, las transiciones temporales son:

| Transición | Tiempo base |
|---|---:|
| T2 | 120 ms |
| T3 | 80 ms |
| T5 | 150 ms |
| T7 | 100 ms |
| T8 | 120 ms |

Las restantes transiciones se consideran inmediatas.

---

## Duración temporal por flujo

### Pago con tarjeta

Secuencia relevante:

T1 -> T2 -> T3

Las transiciones temporales son T2 y T3.

Tiempo temporal acumulado:

120 ms + 80 ms = 200 ms

Durante este flujo se mantiene ocupado el recurso P7.

---

### Pago de alto riesgo

Secuencia relevante:

T4 -> T5

La transición temporal es T5.

Tiempo temporal acumulado:

150 ms

Durante este flujo se utilizan simultáneamente los recursos
P7 y P8.

---

### Transferencia bancaria

Secuencia relevante:

T6 -> T7 -> T8

Las transiciones temporales son T7 y T8.

Tiempo temporal acumulado:

100 ms + 120 ms = 220 ms

Durante este flujo se mantiene ocupado el recurso P8.

---

## Modelo de carga sobre recursos

Se definen:

- C: cantidad de invariantes de pago con tarjeta.
- H: cantidad de invariantes de pago de alto riesgo.
- B: cantidad de invariantes de transferencia bancaria.

Para una corrida objetivo:

C + H + B = 200

La carga temporal acumulada sobre P7 es:

P7 = 200*C + 150*H [ms]

La carga temporal acumulada sobre P8 es:

P8 = 150*H + 220*B [ms]

Debido a que P7 y P8 son recursos diferentes, parte del
procesamiento puede realizarse en paralelo.

Por lo tanto, una primera cota inferior para la duración es:

T >= max(P7, P8)

Esta expresión no pretende predecir exactamente el tiempo de
ejecución. No incluye:

- scheduling de la JVM;
- espera en Conditions;
- adquisición del lock del Monitor;
- escritura del log;
- verificación de invariantes;
- cambios de sensibilización;
- distribución real producida por cada política.

Estos efectos fueron considerados en las mediciones de la campaña formal;
la expresión anterior continúa siendo una estimación analítica y no una
predicción exacta de cada corrida.

---

## Escenario aproximadamente uniforme

Como escenario analítico de referencia se considera:

C = 67
H = 66
B = 67

Total:

67 + 66 + 67 = 200

Carga de P7:

200*67 + 150*66
= 23.300 ms
= 23,3 s

Carga de P8:

150*66 + 220*67
= 24.640 ms
= 24,64 s

Por lo tanto:

T >= 24,64 s

La duración real será superior o cercana a esta cota dependiendo
del overhead y de la planificación concreta de los threads.

---

## Escenario extremo de alto riesgo

Si los 200 invariantes utilizaran el flujo de alto riesgo:

H = 200
C = 0
B = 0

Entonces ambos recursos permanecerían ocupados durante:

150*200
= 30.000 ms
= 30 s

Este escenario también se encuentra dentro de la ventana requerida
de 20 a 40 segundos.

---

## Justificación de la configuración baseline

La configuración inicial:

- T2 = 120 ms
- T3 = 80 ms
- T5 = 150 ms
- T7 = 100 ms
- T8 = 120 ms

produce cargas analíticas compatibles con la ventana objetivo
de 20 a 40 segundos para 200 invariantes.

Esta fue la configuración **BASELINE** utilizada como referencia de la
campaña formal. La segunda configuración experimental se denominó
**VARIANT** y aplica el factor `1.05` a los delays base.

Ambas configuraciones fueron evaluadas en múltiples corridas por política.
Los resultados observados se encuentran en
[`../evidencia-T4.2.2.md`](../evidencia-T4.2.2.md).

---

## Contraste con la campaña experimental finalizada

La campaña formal ejecutó 20 corridas en total:

- políticas: `RANDOM` y `PRIORITY`;
- configuraciones: `BASELINE` y `VARIANT` (`1.05` de escala);
- cinco seeds por combinación;
- 200 T-invariantes completos por corrida.

El análisis de resultados documenta los siguientes **tiempos promedio**:

| Política | Timing | Duración promedio observada |
|---|---|---:|
| RANDOM | BASELINE | 32,142 s |
| RANDOM | VARIANT | 33,519 s |
| PRIORITY | BASELINE | 36,184 s |
| PRIORITY | VARIANT | 38,032 s |

Son promedios empíricos de cinco corridas por escenario, tomados del
[`../analisis-T4.2.3.md`](../analisis-T4.2.3.md), **no** predicciones
individuales de la cota `max(P7, P8)`.

La prioridad del flujo de alto riesgo incrementó la participación de IT2 y,
con ello, la carga compartida sobre P7 y P8. Por su parte, el aumento de un
5 % en las demoras elevó el tiempo observado en ambas políticas.

El análisis detallado —incluidas dispersión, distribuciones e interpretación
del modelo analítico— pertenece a los documentos experimentales:

- [`../protocolo-experimentos.md`](../protocolo-experimentos.md)
- [`../evidencia-T4.2.2.md`](../evidencia-T4.2.2.md)
- [`../analisis-T4.2.3.md`](../analisis-T4.2.3.md)

## Alcance de esta estimación

Los tiempos de cada camino suman los delays configurados, pero una corrida
concurrente también está limitada por la contención de recursos, el
interleaving y el costo de monitorización y registro. Por eso no corresponde
igualar la suma de tiempos por flujo con la duración real de la corrida.

Se conserva este documento como **modelo analítico de T3.2.1**, actualizado
con referencias al contraste experimental ya realizado en T3.2.3 / T4.2.2.
