# Determinación de hilos y responsabilidades

## 1. Objetivo

Determinar la cantidad de hilos necesarios para ejecutar la Red de Petri con el mayor paralelismo posible, aplicando las reglas del enunciado sobre conflictos y joins.

La asignación de hilos no se realiza de manera arbitraria ni creando un hilo por transición. Se parte de los T-invariantes, de los conflictos estructurales y de los puntos de convergencia de la red para definir secuencias coherentes de ejecución.

---

## 2. T-invariantes de referencia

La red posee tres T-invariantes, cada uno asociado a un flujo completo de procesamiento:

```text
IT1 = {T0, T1, T2, T3, T9}
IT2 = {T0, T4, T5, T9}
IT3 = {T0, T6, T7, T8, T9}
```

Interpretación:

| Invariante | Flujo | Secuencia |
|---|---|---|
| IT1 | Pago con tarjeta | `T0 -> T1 -> T2 -> T3 -> T9` |
| IT2 | Pago de alto riesgo | `T0 -> T4 -> T5 -> T9` |
| IT3 | Transferencia bancaria | `T0 -> T6 -> T7 -> T8 -> T9` |

Los tres invariantes comparten `T0` al comienzo y `T9` al final, pero difieren en la secuencia intermedia correspondiente a cada flujo.

---

## 3. Regla aplicada para conflictos

El enunciado establece que, cuando un invariante de transición presenta un conflicto con otro invariante, debe existir:

1. un hilo encargado de ejecutar las transiciones anteriores al conflicto;
2. un hilo por cada invariante luego del conflicto.

En esta red, el conflicto estructural aparece en `P1`.

Desde `P1` compiten las transiciones:

```text
T1
T4
T6
```

Cada una conduce a un T-invariante diferente:

```text
T1 -> IT1
T4 -> IT2
T6 -> IT3
```

Antes de llegar a `P1`, los tres invariantes comparten la transición:

```text
T0
```

Por lo tanto, se necesita un hilo independiente para ejecutar `T0`.

Después del conflicto se requiere un hilo para cada uno de los tres caminos.

Esto produce inicialmente:

```text
1 hilo previo al conflicto
+
3 hilos correspondientes a los tres flujos
=
4 hilos
```

---

## 4. Conflicto en P1

`P1` representa una transacción admitida y lista para ser ruteada.

Desde esa plaza existen tres posibles transiciones de salida:

```text
P1 -> T1
P1 -> T4
P1 -> T6
```

Las tres alternativas son mutuamente excluyentes para una misma transacción.

La elección determina el flujo que seguirá la operación:

- `T1`: pago con tarjeta;
- `T4`: pago de alto riesgo;
- `T6`: transferencia bancaria.

La resolución de este conflicto corresponde a la política del sistema y no debe quedar embebida en los workers.

Por este motivo, los workers se separan en tres secuencias diferentes luego del conflicto.

---

## 5. Regla aplicada para joins

El enunciado establece que, cuando varios T-invariantes presentan un join y luego continúan por un único camino, deben existir tantos hilos posteriores al join como tokens simultáneos puedan encontrarse en la plaza de convergencia.

En esta red, los tres flujos convergen en:

```text
P9
```

Las transiciones que depositan tokens en `P9` son:

```text
T3
T5
T8
```

Luego de `P9` existe un único camino:

```text
P9 -> T9 -> P0
```

Por lo tanto, la cantidad de hilos necesarios para ejecutar `T9` depende del máximo número de tokens que puedan encontrarse simultáneamente en `P9`.

---

## 6. Máximo marcado de P9

La inspección del grafo de alcanzabilidad obtenido mediante PIPE muestra que el máximo marcado alcanzable de `P9` es:

```text
M(P9) = 1
```

No se encontraron estados alcanzables con dos o tres tokens simultáneos en `P9`.

Por lo tanto, de acuerdo con la regla de joins, es suficiente un único hilo posterior al join para ejecutar:

```text
T9
```

Esto agrega un hilo adicional a los cuatro determinados previamente.

---

## 7. Cantidad estructural de hilos

La cantidad estructural resultante es:

```text
1 hilo previo al conflicto
+
3 hilos correspondientes a los tres T-invariantes
+
1 hilo posterior al join
=
5 hilos
```

Por lo tanto:

```text
Cantidad estructural de hilos = 5
```

---

## 8. Asignación de responsabilidades

| Hilo | Transiciones | Responsabilidad | T-invariante relacionado |
|---|---|---|---|
| H0 | `T0` | Admisión previa al conflicto. Toma una transacción de P0 y la coloca en P1. | IT1, IT2, IT3 |
| H1 | `T1 -> T2 -> T3` | Ejecuta el flujo de pago con tarjeta utilizando P7. | IT1 |
| H2 | `T4 -> T5` | Ejecuta el flujo de alto riesgo utilizando simultáneamente P7 y P8. | IT2 |
| H3 | `T6 -> T7 -> T8` | Ejecuta el flujo de transferencia bancaria utilizando P8. | IT3 |
| H4 | `T9` | Retira una transacción de P9 y devuelve el token a P0. | IT1, IT2, IT3 |

---

## 9. Responsabilidad detallada de cada hilo

### H0 — Admisión

Secuencia:

```text
T0
```

Responsabilidad:

- consumir una transacción de `P0`;
- depositarla en `P1`;
- dejarla disponible para la resolución del conflicto entre `T1`, `T4` y `T6`.

H0 existe porque `T0` es común a los tres T-invariantes y se encuentra antes del conflicto estructural en `P1`.

---

### H1 — Flujo de pago con tarjeta

Secuencia:

```text
T1 -> T2 -> T3
```

Responsabilidad:

1. `T1`: seleccionar el flujo de tarjeta y adquirir `P7`;
2. `T2`: completar la autorización;
3. `T3`: capturar fondos, depositar la transacción en `P9` y liberar `P7`.

T-invariante relacionado:

```text
IT1 = {T0, T1, T2, T3, T9}
```

---

### H2 — Flujo de alto riesgo

Secuencia:

```text
T4 -> T5
```

Responsabilidad:

1. `T4`: seleccionar el flujo de alto riesgo y adquirir simultáneamente `P7` y `P8`;
2. `T5`: completar el procesamiento, depositar la transacción en `P9` y liberar ambos recursos.

T-invariante relacionado:

```text
IT2 = {T0, T4, T5, T9}
```

---

### H3 — Flujo de transferencia bancaria

Secuencia:

```text
T6 -> T7 -> T8
```

Responsabilidad:

1. `T6`: seleccionar transferencia bancaria y adquirir `P8`;
2. `T7`: validar la cuenta de destino;
3. `T8`: ejecutar la transferencia, depositar la transacción en `P9` y liberar `P8`.

T-invariante relacionado:

```text
IT3 = {T0, T6, T7, T8, T9}
```

---

### H4 — Salida posterior al join

Secuencia:

```text
T9
```

Responsabilidad:

- consumir una transacción procesada de `P9`;
- devolver el token a `P0`;
- cerrar el ciclo de procesamiento.

H4 existe porque los tres T-invariantes convergen en `P9` y luego continúan por un único camino mediante `T9`.

Como el máximo marcado alcanzable de `P9` es uno, un único hilo H4 es suficiente.

---

## 10. Paralelismo efectivo

La existencia de cinco hilos estructurales no implica que los cinco puedan ejecutar acciones temporales simultáneamente.

Los P-invariantes asociados a los recursos compartidos limitan el paralelismo efectivo.

Puede coexistir:

```text
H1 — pago con tarjeta
+
H3 — transferencia bancaria
```

porque H1 utiliza `P7` mientras H3 utiliza `P8`.

En cambio, H2 necesita simultáneamente ambos recursos:

```text
P7 + P8
```

por lo que el flujo de alto riesgo excluye temporalmente a los otros dos flujos de procesamiento.

El máximo de operaciones temporales de procesamiento simultáneas observado en PIPE es dos.

---

## 11. Relación con la implementación

Esta asignación debe utilizarse como referencia para la implementación de los workers.

Los workers no deben decidir qué transición gana un conflicto. Esa responsabilidad corresponde al monitor y a la política de resolución de conflictos.

La distribución definida es:

```text
H0 -> T0

H1 -> T1 -> T2 -> T3

H2 -> T4 -> T5

H3 -> T6 -> T7 -> T8

H4 -> T9
```

Esta separación permite que cada worker represente una secuencia coherente de la Red de Petri y mantiene desacoplada la resolución de conflictos de la ejecución de los flujos.

---

## 12. Entregables a otros desarrolladores

### DEV 3

Se entrega la tabla final de workers y sus secuencias:

```text
H0 = {T0}
H1 = {T1, T2, T3}
H2 = {T4, T5}
H3 = {T6, T7, T8}
H4 = {T9}
```

DEV 3 deberá utilizar esta definición como referencia para implementar los workers reales.

### DEV 4

Se entregan los T-invariantes que deberán contabilizarse durante las ejecuciones:

```text
IT1 = {T0, T1, T2, T3, T9}
IT2 = {T0, T4, T5, T9}
IT3 = {T0, T6, T7, T8, T9}
```

Estos invariantes representan los tres tipos de ciclo completo que deberán detectarse posteriormente en el log.

---

## 13. Conclusión

Aplicando las reglas del enunciado a los conflictos y joins de la Red de Petri se obtiene una cantidad estructural de cinco hilos.

La distribución queda formada por:

- un hilo anterior al conflicto de `P1`;
- tres hilos asociados a los tres flujos de procesamiento;
- un hilo posterior al join de `P9`.

La asignación final es:

```text
H0 -> T0
H1 -> T1 -> T2 -> T3
H2 -> T4 -> T5
H3 -> T6 -> T7 -> T8
H4 -> T9
```

Esta división surge de la estructura formal de la Red de Petri y no de una decisión de conveniencia de la implementación.
