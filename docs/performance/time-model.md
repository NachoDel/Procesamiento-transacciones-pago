# Modelo temporal analítico

## Objetivo

Documentar la configuración temporal inicial de la Red de Petri
del sistema de procesamiento de pagos y construir una estimación
analítica de la duración de una corrida.

Este documento corresponde a la tarea T3.2.1.

La validación experimental y el ajuste definitivo se realizarán
posteriormente, una vez definida la regla exacta de finalización
de 200 T-invariantes.

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

Estos efectos deberán medirse experimentalmente.

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

No se considera todavía una configuración definitiva.

La selección final deberá realizarse mediante múltiples ejecuciones,
comparando el modelo analítico con los tiempos observados.

---

## Validación experimental pendiente

Cuando se encuentre definida la regla exacta de 200 T-invariantes,
se deberán comparar al menos dos configuraciones temporales.

Para cada configuración se registrará:

- política utilizada;
- seed;
- tiempos de T2, T3, T5, T7 y T8;
- cantidad de invariantes de cada tipo;
- tiempo total medido con reloj monotónico;
- diferencia entre tiempo analítico y observado;
- cumplimiento de la ventana de 20 a 40 segundos.

La campaña experimental corresponde a una etapa posterior y no forma
parte de T3.2.1.