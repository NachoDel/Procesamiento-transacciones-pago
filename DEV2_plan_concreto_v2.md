# Developer 2 — PetriNet y Monitor concurrente


# Convenciones del plan

Este documento usa los IDs del backlog general.

Cada tarea incluye:

- **Qué hay que hacer:** acción concreta.
- **Resultado esperado:** qué debe existir al terminar.
- **Depende de:** solo cuando necesita trabajo de otro developer.
- **Entrega a otro developer:** qué se entrega, a quién y **de qué tarea proviene**.
- **Criterio de terminado:** condición verificable para cerrar la tarea.

## Reglas de coordinación

1. No modificar directamente módulos cuyo owner sea otro developer sin acordarlo.
2. Si una tarea entrega información a otro developer, el entregable debe subirse al repositorio o adjuntarse al issue correspondiente.
3. Toda dependencia entre developers debe quedar vinculada en GitHub con `blocked by` / `blocks`.
4. Si una tarea puede comenzar con mocks o datos temporales, debe hacerse así para no esperar innecesariamente.
5. Los contratos compartidos se congelan al final de la Semana 1:
   - `MonitorInterface`
   - `Policy`
   - estructura de configuración
   - formato de log
6. Cada semana debe terminar con algo integrable en `main`.

---



## Responsabilidad general

DEV 2 es owner del **núcleo compartido y sincronizado**.

Debe implementar:

- representación genérica de la RdP;
- cálculo de sensibilización;
- disparo;
- Monitor;
- exclusión mutua;
- espera y reactivación;
- integración de Política;
- integración de chequeo de invariantes;
- integración del logger.

El Monitor no debe contener referencias específicas como `if (transition == 4)` para resolver la red concreta.

---

# Semana 1 — PetriNet genérica y contrato del Monitor

## T2.1.1 — `[TASK] Implementar PetriNet genérica`

### Qué hay que hacer
Crear una clase que reciba por configuración:

- matriz de incidencia;
- marcado inicial;
- cantidad de plazas;
- cantidad de transiciones.

Debe permitir, internamente:

- obtener marcado;
- evaluar si una transición está sensibilizada;
- calcular el marcado siguiente;
- aplicar un disparo.

### Resultado esperado
Clase `PetriNet` sin conocimiento del sistema de pagos.

### Depende de
- Puede empezar con fixtures propios.
- Para cerrar necesita `T1.1.2` de DEV 1.

### Entrega a otro developer
- No entrega todavía un componente obligatorio a otro developer.

### Criterio de terminado
La misma clase funciona con una red de prueba y con la red real cargada desde datos.

---

## T2.1.2 — `[TEST] Probar sensibilización y disparo`

### Qué hay que hacer
Crear tests para:

1. transición sensibilizada;
2. transición no sensibilizada;
3. marcado siguiente correcto;
4. disparo inválido no modifica el marcado;
5. varias transiciones consecutivas.

### Resultado esperado
Tests unitarios de `PetriNet`.

### Depende de
`T2.1.1`.

### Entrega a otro developer
- **A DEV 1**
  - Entregable: resultados de tests usando la matriz real.
  - **Proviene de:** `T2.1.2`.
  - Motivo: permite confirmar que la codificación reproduce el modelo formal.

### Criterio de terminado
Los resultados coinciden con los ejemplos calculados desde la red.

---

## T2.1.3 — `[TASK] Implementar MonitorInterface y disparo atómico`

### Qué hay que hacer
Implementar exactamente:

```java
public interface MonitorInterface {
    boolean fireTransition(int transition);
}
```

Dentro del Monitor:

1. entrar en exclusión mutua;
2. consultar sensibilización;
3. aplicar política si existe conflicto;
4. disparar;
5. verificar invariantes;
6. registrar el disparo;
7. despertar hilos que puedan continuar;
8. liberar exclusión mutua.

### Resultado esperado
`Monitor` con un único método público funcional.

### Depende de
- `T2.1.1`.
- contrato `Policy` de `T3.1.1` de DEV 4.

### Entrega a otro developer
- **A DEV 3**
  - Entregable: `MonitorInterface` estable y comportamiento documentado.
  - **Proviene de:** `T2.1.3`.
  - Motivo: DEV 3 implementa workers contra esa interfaz.

### Criterio de terminado
Dos threads pueden llamar al Monitor sin modificar simultáneamente el marcado.

---

# Semana 2 — Bloqueo y reactivación

## T2.2.1 — `[TASK] Implementar espera y reactivación sin busy waiting`

### Qué hay que hacer
Cuando una transición solicitada no puede dispararse:

1. bloquear el hilo;
2. colocarlo en la condición/cola apropiada;
3. reactivarlo cuando un disparo cambie el marcado;
4. volver a comprobar la condición;
5. evitar espera activa.

Validar:

- que no se pierdan señales;
- que no haya threads esperando para siempre cuando pueden continuar;
- que no se despierte innecesariamente a todos si puede evitarse.

### Resultado esperado
Monitor capaz de manejar contención real.

### Depende de
`T2.1.3`.

### Entrega a otro developer
- **A DEV 3**
  - Entregable: Monitor funcional con bloqueo/reactivación.
  - **Proviene de:** `T2.2.1`.
  - Motivo: DEV 3 necesita probar workers reales bajo contención.

### Criterio de terminado
Un test con varios threads demuestra bloqueo y posterior continuación sin busy waiting.

---

# Semana 3 — Integración de invariantes y logging

## Integración de T4.1.2 de DEV 1

### Qué hay que hacer
Invocar el checker de P-invariantes inmediatamente después de actualizar el marcado.

### Depende de
- `T4.1.2` de DEV 1.

### Entrega a otro developer
- **A DEV 1**
  - Entregable: evidencia de integración post-disparo.
  - **Proviene de:** integración de `T4.1.2` dentro de `T2.1.3`.
  - Motivo: DEV 1 valida que la comprobación se ejecute en el punto correcto.

### Criterio de terminado
Cada disparo exitoso genera exactamente una validación del marcado resultante.

---

## Integración de T4.1.1 de DEV 4

### Qué hay que hacer
Registrar desde el Monitor solo después de un disparo exitoso.

El log debe conservar el orden real de disparos.

### Depende de
- `T4.1.1` de DEV 4.

### Entrega a otro developer
- **A DEV 4**
  - Entregable: log real generado desde el Monitor.
  - **Proviene de:** integración de `T4.1.1` dentro de `T2.1.3`.
  - Motivo: DEV 4 lo usa para validar el runner y campañas.

### Criterio de terminado
Una corrida concurrente genera un log ordenado y parseable.

---

# Semana 4 — Stress del núcleo

## Soporte a T2.2.3 — `[TEST] Stress de Monitor`

### Qué hay que hacer
Junto con DEV 3 ejecutar:

- muchos workers;
- varias corridas;
- alta contención;
- interrupciones;
- cierre.

Analizar específicamente:

- deadlock;
- race conditions;
- lost wakeups;
- marcado inconsistente.

### Resultado esperado
Informe corto de stress y correcciones realizadas.

### Depende de
- `T2.2.2` de DEV 3.
- `T2.2.3` de DEV 3.

### Entrega a otro developer
- **A DEV 4**
  - Entregable: versión del núcleo declarada estable para campañas.
  - **Proviene de:** validación conjunta sobre `T2.2.3`.
  - Motivo: DEV 4 no debe ejecutar campañas finales sobre un Monitor inestable.

### Criterio de terminado
No aparecen deadlocks ni violaciones de marcado en las corridas de stress definidas.

---

# Semana 5 — Cierre y documentación

## Trabajo de cierre

### Qué hay que hacer
Documentar:

- cómo se logra exclusión mutua;
- qué condición bloquea un hilo;
- cuándo se reactiva;
- cómo se usa `Policy`;
- cómo se mantiene el Monitor agnóstico;
- cómo se garantiza atomicidad.

Revisar el diagrama de secuencia de DEV 4.

### Entrega a otro developer
- **A DEV 4**
  - Entregable: sección técnica del Monitor + revisión del diagrama.
  - **Proviene de:** `T2.1.1`, `T2.1.2`, `T2.1.3`, `T2.2.1`.
  - Motivo: DEV 4 integra documentación y diagramas.

---

# Resumen de dependencias de DEV 2

| Necesita | De | Proviene de tarea | Para ejecutar |
|---|---|---|---|
| Matriz + marcado inicial | DEV 1 | T1.1.2 | T2.1.1 |
| Contrato Policy | DEV 4 | T3.1.1 | T2.1.3 |
| P-invariant checker | DEV 1 | T4.1.2 | Integración post-disparo |
| Logger | DEV 4 | T4.1.1 | Logging del Monitor |
| Workers reales | DEV 3 | T2.2.2 | Stress final |
