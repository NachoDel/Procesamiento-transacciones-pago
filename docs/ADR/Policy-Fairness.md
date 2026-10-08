# ADR — Prioridad y fairness en la resolución de conflictos

**Estado:** Aceptada  
**Proyecto:** TP Final — Programación Concurrente 2026  
**Alcance:** T3.1.1, T3.1.2 y T3.1.3

## 1. Contexto

El sistema de procesamiento concurrente de pagos modela los caminos de ejecución mediante una Red de Petri. En el punto de encaminamiento existen tres transiciones alternativas:

| Transición | Camino |
|---|---|
| `T1` | Pago con tarjeta |
| `T4` | Pago de alto riesgo |
| `T6` | Transferencia bancaria |

Estas transiciones integran el grupo de conflicto estructural `{T1, T4, T6}`. El proyecto requiere comparar dos estrategias de selección: una **aleatoria** y otra que **priorice el camino de alto riesgo**.

La responsabilidad de la política es elegir **entre los candidatos que recibe**, no decidir por sí misma qué transiciones están sensibilizadas ni administrar todos los hilos del sistema.

## 2. Decisión: `RandomPolicy`

`RandomPolicy` selecciona de forma equiprobable una **posición** dentro de la lista de candidatos disponible en cada invocación. Si recibe, por ejemplo, `[1, 4, 6]`, cada elemento tiene probabilidad teórica `1/3` de ser elegido en esa invocación.

Se admite construir la política con una semilla (`seed`) para reproducir su secuencia pseudoaleatoria **cuando recibe la misma secuencia de listas de candidatos**.

**Limitación de reproducibilidad:** una misma semilla **no garantiza una corrida concurrente idéntica**. El planificador de hilos, las condiciones de espera, el marcado y los tiempos de transición pueden modificar qué candidatos están disponibles en cada decisión. La distribución final de los T-invariantes puede variar entre ejecuciones incluso si se reutiliza la semilla.

## 3. Decisión: `PriorityPolicy`

Se adopta **prioridad estricta** para la transición configurada como prioritaria, que en el modelo oficial es `T4`.

La regla es:

```text
Si T4 pertenece a la lista de candidatos recibida:
    elegir T4
En caso contrario:
    delegar en la política fallback
```

`PriorityPolicy` es genérica: recibe la transición prioritaria por parámetro y **no contiene `T4` hardcodeada**. La política de respaldo determina qué candidato se elige cuando la prioridad no está disponible.

La prioridad se aplica en los puntos donde el `Monitor` consulta la política para resolver un conflicto entre alternativas habilitadas y en espera. **No debe interpretarse como una garantía de que T4 siempre dispare por delante de cualquier otra solicitud en toda planificación concurrente**: el comportamiento también depende de los candidatos presentes en ese momento y del mecanismo de reactivación del Monitor.

## 4. Fairness y posible inanición (*starvation*)

Se decide **no implementar** mecanismos adicionales de equidad, tales como:

- *Aging* (aumento gradual de prioridad por tiempo de espera).
- Cuotas mínimas para cada alternativa.
- Rotación obligatoria o round-robin.
- Reservas anticipadas de recursos.

Por lo tanto, `PriorityPolicy` **no garantiza fairness** entre alternativas del mismo conflicto.

En el escenario hipotético donde la lista `[1, 4, 6]` se presenta de manera sostenida, la política elegirá `T4` en todas las decisiones. Las alternativas `T1` y `T6` podrían no ser elegidas nunca en ese escenario: existe **posibilidad de starvation**.

Esta es una propiedad de la **regla de selección**, no una afirmación de que la ejecución completa de la Red de Petri necesariamente sufrirá starvation. La disponibilidad real de `T4` depende del marcado y de los recursos compartidos.

### Justificación

El requisito de la estrategia priorizada es favorecer al flujo de alto riesgo. No se definió una condición obligatoria de equidad, un porcentaje mínimo por camino ni una política de aging. Incorporar esos mecanismos alteraría la estrategia de prioridad estricta que se pretende comparar con la política aleatoria.

Se acepta y documenta explícitamente este compromiso. En las campañas experimentales deberá analizarse la distribución observada de los caminos, sin presuponer que la prioridad garantiza equidad.

## 5. No reservar recursos de transiciones no habilitadas

La política solamente selecciona entre **alternativas candidatas en el momento de la decisión**.

En particular:

```text
T4 no está habilitada / no es candidata
    -> la política no puede elegirla
    -> no se reservan P7 ni P8 para una eventual T4 futura
```

No se introduce un mecanismo de *grant* persistente ni una reserva anticipada de tokens. Esto respeta la separación de responsabilidades:

```text
PetriNet       -> marcado y sensibilización
Monitor        -> exclusión mutua, espera y candidatos ejecutables
ConflictGroup  -> alternativas que compiten
Policy         -> elección entre los candidatos recibidos
```

## 6. La política no es un scheduler global

La política de encaminamiento se utiliza en el conflicto `{T1, T4, T6}`; no arbitra indiscriminadamente todas las transiciones de la Red de Petri.

Las transiciones independientes pueden continuar sin ser comparadas mediante `Policy`. La preferencia de transiciones inmediatas frente a temporales es parte de la semántica de ejecución del `Monitor`, no de `PriorityPolicy`.

## 7. Evidencia y pruebas

### `RandomPolicyTest`

Se verifican casos de selección válida, lista vacía, semillas y reproducibilidad, además de una prueba de **distribución razonablemente uniforme** sobre una muestra grande.

La muestra no exige obtener proporciones exactas: el margen tolerado sirve como detección de sesgos evidentes, no como demostración estadística formal de uniformidad.

### `PriorityPolicyTest`

Se verifica la elección de la transición prioritaria cuando está presente, el uso de la política fallback cuando no lo está, y el comportamiento bajo decisiones repetidas.

En particular, una prueba sostenida muestra que con `T4` presente en todos los conjuntos de candidatos se elige `T4` en todas las iteraciones. Otra prueba explicita la consecuencia de posible inanición para `T1` y `T6` bajo esa condición controlada.

### `MonitorTest`

La integración valida que la política participe en la reactivación de hilos ante un conflicto entre transiciones en espera y que **no se utilice para transiciones independientes**.

Estas pruebas verifican contratos concretos de selección y coordinación. **No sustituyen** las campañas experimentales de 200 operaciones ni prueban ausencia de inanición en todos los entrelazados posibles.

## 8. Consecuencias de la decisión

**Ventajas:**

- Reglas simples, comprobables y configurables.
- Separación clara entre Red de Petri, Monitor y políticas.
- No se agregan estados ocultos, cuotas ni reservas de recursos.
- Es posible contrastar directamente selección aleatoria y prioridad estricta.

**Limitaciones:**

- La prioridad estricta no garantiza fairness.
- Alternativas no prioritarias pueden experimentar starvation bajo conflicto sostenido.
- La semilla no garantiza reproducibilidad completa del orden de eventos concurrentes.
- Las pruebas unitarias de políticas no describen por sí solas la distribución real de los flujos.

## 9. Conclusión

Se mantiene la siguiente definición:

```text
RandomPolicy
    -> selección uniforme entre candidatos actuales
    -> semilla configurable

PriorityPolicy
    -> prioridad estricta de T4 (configurada externamente)
    -> fallback cuando T4 no es candidata
    -> sin aging ni cuotas
    -> sin reservas futuras
    -> posible starvation, aceptada y documentada

Monitor
    -> invoca Policy sólo para conflictos pertinentes
    -> no convierte Policy en scheduler global
```

La decisión queda documentada para el cierre de **T3.1.1, T3.1.2 y T3.1.3**. El impacto práctico sobre la distribución de los caminos se evaluará más adelante mediante los registros y experimentos del proyecto.
