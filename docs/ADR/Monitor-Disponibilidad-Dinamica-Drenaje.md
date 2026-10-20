# ADR — Disponibilidad dinámica de transiciones durante el drenaje

**Estado:** Aceptada — implementación y pruebas de integración realizadas  
**Área:** Monitor, semántica temporal y ciclo de vida de Workers  
**Relación con el backlog:** T2.1.3, T2.2.2, T2.2.4 y T3.2.2

## Contexto

El sistema de procesamiento de pagos utiliza una Red de Petri con un `Monitor` que serializa los disparos mediante `ReentrantLock`, coordina esperas con `Condition` y aplica prioridad de transiciones inmediatas sobre transiciones temporales.

Se eligió `ReentrantLock` en lugar de depender exclusivamente de un método `synchronized` porque las colas de espera por transición y la condición temporal requieren señalización y suspensión explícitas mediante `Condition`. Las esperas liberan el lock y permiten que otros Workers progresen; el Monitor lo recupera antes de volver a comprobar la condición.

La ejecución del modelo cuenta con cinco Workers oficiales:

| Worker | Secuencia de transiciones | Responsabilidad |
|---|---|---|
| H0 | T0 | Admitir una transacción |
| H1 | T1 → T2 → T3 | Procesar pago con tarjeta |
| H2 | T4 → T5 | Procesar pago de alto riesgo |
| H3 | T6 → T7 → T8 | Procesar transferencia bancaria |
| H4 | T9 | Completar y retornar una transacción al circuito |

El protocolo de *shutdown por drenaje* establece que `H0` deje de admitir transacciones después de exactamente `TARGET` disparos exitosos de `T0`, mientras `H1`–`H4` siguen activos hasta alcanzar `TARGET` disparos exitosos de `T9`. Luego se solicita la parada global y se espera la terminación de todos los threads.

El stop específico de `H0` ya estaba implementado mediante una condición compuesta:

```text
stop efectivo = stop global OR stop específico del Worker
```

## Problema observado

El primer test de integración de drenaje utilizó la Red de Petri, el Monitor, los tiempos y los cinco Workers reales, con `TARGET = 12`.

El sistema alcanzó correctamente el límite de admisiones, pero no completó el drenaje. El diagnóstico registrado fue:

```text
Admissions T0 = 12
Completions T9 = 10
Admission closed = true
Current marking = [1, 0, 1, 0, 0, 1, 0, 0, 0, 0]
H1 -> WAITING
H2 -> WAITING
H3 -> WAITING
H4 -> WAITING
```

El marcado indica que una transacción permanecía en `P2` (pendiente de `T2`) y otra en `P5` (pendiente de `T7`). Los recursos `P7` y `P8` continuaban ocupados por esas operaciones. Al mismo tiempo, `P0` conservaba un token, de modo que **`T0` seguía sensibilizada en la Red de Petri**, aunque `H0` ya hubiera finalizado por su stop selectivo.

En la implementación original, la prioridad se evaluaba buscando cualquier transición inmediata sensibilizada:

```java
transitionSemantics.isImmediate(t) && petriNet.isEnabled(t)
```

La consecuencia era que `T0` seguía bloqueando las transiciones temporales `T2` y `T7`, pese a no estar disponible en la fase de drenaje. Ya no quedaba ningún Worker capaz de disparar `T0` y cambiar el marcado para liberar esa espera.

**Precisión:** se trataba de un bloqueo de la ejecución por una regla de planificación incompatible con el cierre de admisiones; no de una ausencia estructural de transiciones sensibilizadas en la Red de Petri.

## Decisión

Se separan dos conceptos que antes el Monitor trataba como equivalentes:

1. **Sensibilización estructural:** `petriNet.isEnabled(t)` indica que el marcado contiene los tokens necesarios para disparar `t`.
2. **Disponibilidad durante la ejecución:** `transitionAvailability.test(t)` indica que la transición puede participar en la fase actual del ciclo de vida.

Se inyecta en el constructor del Monitor una dependencia genérica:

```java
IntPredicate transitionAvailability
```

Los constructores existentes delegan utilizando:

```java
transition -> true
```

De este modo se conserva el comportamiento de las ejecuciones que no requieren disponibilidad dinámica.

Las reglas de ejecución pasan a ser:

```text
canFireNow(t) =
    PetriNet.isEnabled(t)
    AND transitionAvailability.test(t)
    AND (
        t es inmediata
        OR no existe otra transición inmediata
           sensibilizada Y disponible
    )
```

Esta es una representación conceptual: la implementación real utiliza los métodos privados `canFireNow(...)` y `hasEnabledImmediateTransition()`.

**La regla inmediata > temporal no desaparece:** sólo se aplica sobre transiciones inmediatas que todavía pertenecen a la fase de ejecución activa.

## Aplicación al cierre por drenaje

Para el modelo de pagos, la capa de integración conoce cuál es la transición de admisión y puede suministrar un predicado equivalente a:

```java
transition ->
        transition != ADMISSION_TRANSITION
                || !tracker.isAdmissionClosed()
```

El Monitor no contiene índices de transiciones, nombres de Workers ni nociones de `TARGET`.

El orden requerido al llegar a la admisión número `TARGET` es:

1. `H0` realiza el disparo exitoso de `T0` bajo exclusión mutua.
2. `PostFireObserver` recibe el disparo ya comprometido y actualiza el contador de admisiones.
3. Al alcanzar `TARGET`, el estado `admissionClosed` pasa a `true` **antes de que `fireTransition(T0)` retorne**.
4. El Monitor vuelve a evaluar las condiciones y señaliza posibles esperas después del observer.
5. `H0` observa su condición de stop y no comienza un nuevo ciclo.
6. `T0` deja de participar de la prioridad de inmediatas aunque conserve sensibilización estructural.
7. `H1`–`H4` continúan hasta completar las operaciones previamente admitidas.
8. Después de `T9 == TARGET`, el coordinador de la corrida solicita el stop global e interrumpe, si hace falta, a los Workers bloqueados.

El predicado debe consultar estado **seguro entre threads** (por ejemplo, un `AtomicBoolean`) y no debe introducir esperas ni operaciones bloqueantes mientras el Monitor mantiene su lock.

## Justificación

La solución mantiene las responsabilidades separadas:

| Componente | Responsabilidad |
|---|---|
| `PetriNet` | Sensibilización y evolución del marcado |
| `TransitionSemantics` | Clasificación inmediata / temporal |
| `Monitor` | Exclusión mutua, espera, prioridad temporal y disparo atómico |
| `IntPredicate transitionAvailability` | Disponibilidad de cada transición según la fase de ejecución |
| `PostFireObserver` | Observación sincronizada de los disparos exitosos |
| `WorkerCoordinator` | Inicio, parada selectiva/global e interrupción de Workers |
| `RunProgressTracker` / `ExperimentRunner` (DEV4) | Contadores productivos, cierre por `TARGET` y resultado de cada corrida |

Se preservan expresamente estas propiedades:

- `MonitorInterface` mantiene `boolean fireTransition(int transition)`.
- El Monitor no añade otra operación pública de ejecución.
- El Monitor sigue siendo agnóstico al modelo de pagos.
- `Policy` continúa resolviendo únicamente conflictos estructurales entre candidatos válidos.
- No se implementan reservas de recursos futuros ni una excepción hardcodeada para `T0`.
- Los constructores anteriores conservan el comportamiento por defecto.

## Alternativas descartadas

| Alternativa | Motivo del descarte |
|---|---|
| Aumentar el timeout del test | El problema era un bloqueo lógico, no una demora insuficiente |
| Reducir `TARGET` hasta que `P0` quedara vacío | Ocultaría la falla y no funcionaría para todos los objetivos de corrida |
| Quitar globalmente la prioridad de inmediatas | Cambiaría la semántica temporal acordada |
| Insertar `if (transition == 0)` dentro de `Monitor` | Acoplaría el Monitor a una red concreta |
| Usar únicamente el stop selectivo de `H0` | No modificaba la prioridad ejercida por una inmediata todavía sensibilizada |

## Evidencia de verificación

Se añadieron y ejecutaron:

- `MonitorTest.inactiveImmediateTransitionShouldNotBlockTimedTransition`: comprueba que una inmediata sensibilizada pero declarada inactiva deja progresar a una temporal.
- `PaymentWorkersIntegrationTest.shouldDrainExactlyTargetAdmissionsAndReturnToInitialMarking`: utiliza los Workers y la Red de Petri oficiales, con cierre selectivo de `H0` y drenaje hasta el marcado inicial.

La prueba de integración verifica:

```text
T0 == TARGET
T9 == TARGET
admissionClosed == true
marcado final == M0
todos los Workers terminados
```

El usuario ejecutó repetidamente el test concurrente sin fallas y confirmó **106 tests verdes** en la suite completa, sin errores ni fallos. Esto valida el escenario reducido de integración; **no constituye todavía una campaña de 200 invariantes ni una medición de la ventana de 20–40 segundos**.

## Consecuencias y límites

### Positivas

- Se elimina el bloqueo observado durante el drenaje.
- Se mantiene el comportamiento normal de inmediata > temporal.
- La solución es reutilizable para otras redes y otros cambios de fase.
- El estado de cierre de admisiones puede gestionarse sin detener prematuramente operaciones en vuelo.

### Consideraciones

- El predicado externo se evalúa mientras el Monitor mantiene el lock; debe ser rápido, thread-safe y sin bloqueo.
- El mecanismo actual despierta esperas tras un disparo exitoso y su `PostFireObserver`. **No debe asumirse que modificar el predicado arbitrariamente desde otro thread, sin disparos ni señalización, despertará a quienes ya están bloqueados.** El cierre de admisión previsto se realiza sincrónicamente dentro del observer de `T0 #TARGET` y sí utiliza la señalización existente.
- La propiedad de terminación completa también depende de que la capa experimental solicite stop global, interrumpa los Workers bloqueados cuando corresponda y ejecute `awaitTermination`.
- La validación de 200 invariantes, de las dos políticas y del tiempo total de ejecución pertenece a la integración y campaña experimental posterior.

## Conclusión

La **disponibilidad dinámica de transiciones** es parte del contrato del Monitor para ejecuciones con fases de admisión y drenaje. Permite que una transición inmediata, aunque continúe sensibilizada en la Red de Petri, deje de bloquear el avance de operaciones temporales una vez que la ejecución declara que ya no debe participar.

La lógica concreta de cierre por cantidad de admisiones permanece fuera del Monitor, en la capa de progreso y orquestación de la corrida.
