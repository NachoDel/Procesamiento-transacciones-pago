# Modelo formal de la Red de Petri

## 1. Objetivo

Este documento concentra la información formal de la Red de Petri utilizada para modelar el sistema de procesamiento de transacciones de pago.

Su objetivo es establecer una referencia común para la implementación, incluyendo:

- marcado inicial;
- propiedades obtenidas mediante PIPE;
- invariantes de transición;
- invariantes de plaza;
- matriz de incidencia combinada;
- estados representados por las plazas;
- eventos representados por las transiciones.

La lógica de la red deberá derivarse de esta representación formal y no de reglas particulares codificadas para cada transición.

---

## 2. Marcado inicial

El marcado inicial de la red es:

```text
M0 = (3, 0, 0, 0, 0, 0, 0, 1, 1, 0)
```

El orden de las componentes es:

```text
(P0, P1, P2, P3, P4, P5, P6, P7, P8, P9)
```

Por lo tanto:

| Plaza | P0 | P1 | P2 | P3 | P4 | P5 | P6 | P7 | P8 | P9 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| M0 | 3 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | 1 | 0 |

Los tres tokens de `P0` representan las tres transacciones que circulan por el sistema.

El token de `P7` representa la disponibilidad del gateway de tarjetas y el token de `P8` representa la disponibilidad del motor antifraude.

En el marcado inicial, la única transición sensibilizada es `T0`.

---

## 3. Propiedades de la red

El análisis mediante PIPE 4.3.0 produjo los siguientes resultados:

| Propiedad | Resultado | Interpretación |
|---|---|---|
| Acotada (Bounded) | `true` | Todas las plazas poseen una cantidad finita de tokens. |
| Segura (Safe) | `false` | La red no es 1-acotada, ya que P0 contiene tres tokens desde el marcado inicial. |
| Deadlock | `false` | No existe un marcado alcanzable desde el cual el sistema no pueda continuar su ejecución. |

Que `Safe = false` no representa una falla del sistema. En una Red de Petri, una red es segura cuando ninguna plaza puede contener más de un token. La existencia de tres tokens en `P0` hace que esta propiedad no se cumpla, aunque la red continúa siendo acotada.

### 3.1. Vivacidad

PIPE obtuvo un grafo con 33 marcados alcanzables, identificados desde `S0` hasta `S32`.

El grafo no contiene estados terminales de deadlock. Desde los distintos marcados alcanzables es posible finalizar los flujos activos, recuperar los recursos `P7` y `P8`, devolver las transacciones a `P0` y volver a seleccionar cualquiera de los tres caminos desde `P1`.

Por este motivo, el modelo conserva la posibilidad de ejecutar nuevamente todas sus transiciones y se interpreta como una red viva.

### 3.2. Paralelismo observado

El máximo de operaciones temporales de procesamiento simultáneas observado es dos.

Una transacción puede utilizar `P7` dentro del flujo de tarjeta mientras otra utiliza `P8` dentro del flujo de transferencia.

El flujo de alto riesgo no puede coexistir con esos procesamientos porque necesita adquirir simultáneamente ambos recursos, `P7` y `P8`.

---

## 4. Invariantes de transición

Un invariante de transición representa una secuencia de disparos cuyo efecto neto sobre el marcado es nulo.

En esta red, cada T-invariante representa un ciclo completo de procesamiento de una transacción.

| Invariante | Transiciones | Flujo | Interpretación |
|---|---|---|---|
| IT1 | `{T0, T1, T2, T3, T9}` | Pago con tarjeta | Admisión, autorización, captura y retorno a P0. |
| IT2 | `{T0, T4, T5, T9}` | Pago de alto riesgo | Admisión, procesamiento utilizando ambos recursos y retorno a P0. |
| IT3 | `{T0, T6, T7, T8, T9}` | Transferencia bancaria | Admisión, validación, ejecución y retorno a P0. |

Los tres T-invariantes cubren todas las transiciones de la red.

Esta propiedad refleja el carácter cíclico del sistema y será utilizada posteriormente para contabilizar los invariantes completos mediante el archivo de log de ejecución.

---

## 5. Invariantes de plaza

Los invariantes de plaza expresan relaciones entre plazas cuyo número ponderado de tokens permanece constante para cualquier secuencia válida de disparos.

### IP1 — Conservación del gateway de tarjetas

```text
M(P2) + M(P3) + M(P4) + M(P7) = 1
```

El token correspondiente al gateway se encuentra disponible en `P7` o retenido por una transacción de tarjeta en `P2/P3` o por una transacción de alto riesgo en `P4`.

Este invariante expresa la conservación y el uso exclusivo del gateway.

### IP2 — Conservación del motor antifraude

```text
M(P4) + M(P5) + M(P6) + M(P8) = 1
```

El token correspondiente al motor antifraude se encuentra disponible en `P8`, ocupado por una transacción de alto riesgo en `P4` o utilizado por una transferencia en `P5/P6`.

### IP3 — Conservación de las transacciones

```text
M(P0) + M(P1) + M(P2) + M(P3) + M(P4) + M(P5) + M(P6) + M(P9) = 3
```

Los tres tokens iniciales que representan las transacciones permanecen siempre dentro del sistema.

Pueden encontrarse esperando, admitidos, dentro de cualquiera de los flujos de procesamiento o en el buffer de salida, pero la Red de Petri no crea ni destruye transacciones.

### Aplicación en la implementación

Las tres ecuaciones deberán comprobarse después de cada disparo de transición.

Un incumplimiento de cualquiera de estos invariantes indicará que el marcado generado por la implementación no respeta el modelo formal de la Red de Petri.

---

## 6. Matriz de incidencia

La matriz de incidencia combinada se obtiene mediante:

```text
I = I+ - I-
```

donde `I+` es la matriz de incidencia posterior e `I-` la matriz de incidencia anterior.

La matriz combinada de la red tiene dimensiones `10 x 10`:

|    | T0 | T1 | T2 | T3 | T4 | T5 | T6 | T7 | T8 | T9 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| P0 | -1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 1 |
| P1 | 1 | -1 | 0 | 0 | -1 | 0 | -1 | 0 | 0 | 0 |
| P2 | 0 | 1 | -1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| P3 | 0 | 0 | 1 | -1 | 0 | 0 | 0 | 0 | 0 | 0 |
| P4 | 0 | 0 | 0 | 0 | 1 | -1 | 0 | 0 | 0 | 0 |
| P5 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | -1 | 0 | 0 |
| P6 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | -1 | 0 |
| P7 | 0 | -1 | 0 | 1 | -1 | 1 | 0 | 0 | 0 | 0 |
| P8 | 0 | 0 | 0 | 0 | -1 | 1 | -1 | 0 | 1 | 0 |
| P9 | 0 | 0 | 0 | 1 | 0 | 1 | 0 | 0 | 1 | -1 |

La interpretación de sus elementos es:

```text
-1 -> la transición consume un token de la plaza
+1 -> la transición produce un token en la plaza
 0 -> la transición no modifica la plaza
```

Por ejemplo, la columna correspondiente a `T4` contiene valores `-1` en `P1`, `P7` y `P8`, y un valor `+1` en `P4`.

Por lo tanto, `T4` consume simultáneamente una transacción admitida, el recurso gateway y el recurso antifraude, para iniciar el procesamiento de alto riesgo.

---

## 7. Ecuación de estado

La evolución del marcado se representa mediante:

```text
M(k+1) = M(k) + I * sigma
```

donde:

- `M(k)` representa el marcado actual;
- `I` representa la matriz de incidencia;
- `sigma` representa el vector de disparo;
- `M(k+1)` representa el marcado resultante.

El vector `sigma` contiene un único valor `1` en la posición de la transición que se desea disparar y `0` en las demás posiciones.

Esta representación permite calcular el marcado siguiente utilizando la matriz de incidencia, sin codificar reglas específicas para `T0`, `T1`, ..., `T9`.

---

## 8. Estados del sistema

Las plazas representan estados de las transacciones o recursos compartidos.

| Plaza | Estado o recurso | Descripción |
|---|---|---|
| P0 | Cola de arribo / estado IDLE | Transacciones disponibles para ingresar al sistema. |
| P1 | Transacción admitida | Payload, esquema y autenticación validados; lista para ser ruteada. |
| P2 | Tarjeta - etapa de autorización | Solicitud de autorización enviada al emisor y espera de respuesta. |
| P3 | Tarjeta - etapa de captura | Autorización obtenida; captura o confirmación de fondos. |
| P4 | Procesamiento de alto riesgo | Scoring antifraude con consulta simultánea al gateway. |
| P5 | Transferencia - validación | Validación de la cuenta de destino y controles iniciales. |
| P6 | Transferencia - ejecución | Ejecución de la transferencia en la red bancaria. |
| P7 | Gateway disponible | Sesión exclusiva con la red de tarjetas. |
| P8 | Motor antifraude disponible | Slot exclusivo para evaluación de riesgo. |
| P9 | Buffer de salida | Transacciones procesadas, listas para confirmación y liquidación. |

---

## 9. Eventos del sistema

Las transiciones representan los eventos que modifican el estado de la Red de Petri.

| Transición | Evento | Efecto en el modelo |
|---|---|---|
| T0 | Admitir transacción | Consume una transacción de P0 y la coloca en P1. |
| T1 | Seleccionar pago con tarjeta | Rutea la transacción a P2 y adquiere P7. |
| T2 | Completar autorización | Mueve la operación de P2 a P3. |
| T3 | Capturar fondos | Envía la transacción a P9 y libera P7. |
| T4 | Seleccionar pago de alto riesgo | Rutea la transacción a P4 y adquiere simultáneamente P7 y P8. |
| T5 | Completar procesamiento de alto riesgo | Envía la transacción a P9 y libera P7 y P8. |
| T6 | Seleccionar transferencia bancaria | Rutea la transacción a P5 y adquiere P8. |
| T7 | Validar cuenta destino | Mueve la transferencia de P5 a P6. |
| T8 | Ejecutar transferencia | Envía la transacción a P9 y libera P8. |
| T9 | Retirar del buffer de salida | Confirma el procesamiento y devuelve el token a P0 para cerrar el ciclo. |

---

## 10. Datos que deben consumir los demás componentes

La implementación de la Red de Petri deberá utilizar como configuración formal:

```text
Marcado inicial:
M0 = (3, 0, 0, 0, 0, 0, 0, 1, 1, 0)

Cantidad de plazas:
10

Cantidad de transiciones:
10

Matriz de incidencia:
la matriz I definida en la sección 6.

Invariantes de plaza:
IP1 = M(P2) + M(P3) + M(P4) + M(P7) = 1
IP2 = M(P4) + M(P5) + M(P6) + M(P8) = 1
IP3 = M(P0) + M(P1) + M(P2) + M(P3) + M(P4) + M(P5) + M(P6) + M(P9) = 3
```

Estos datos constituyen el contrato formal entre el análisis de la Red de Petri y su implementación en Java.
