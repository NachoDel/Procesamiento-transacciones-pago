# Developer 3 — Workers, finalización y tiempo


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

DEV 3 implementa **quién llama al Monitor y en qué secuencia**.

Debe transformar la cantidad/responsabilidad de hilos definida formalmente por DEV 1 en threads/workers reales.

También es responsable de:

- lifecycle;
- finalización;
- interrupciones;
- semántica temporal;
- análisis de duración;
- garantizar que no queden hilos vivos.

---

# Semana 1 — Worker base con MockMonitor

## Preparación de T2.2.2 — Worker genérico

### Qué hay que hacer
Crear una clase Worker que reciba:

- `MonitorInterface`;
- secuencia de transiciones;
- identificador/nombre del worker;
- señal o criterio de finalización.

Mientras DEV 2 termina el Monitor, usar `FakeMonitor`.

### Resultado esperado
Worker capaz de recorrer una secuencia ficticia sin depender de la implementación del Monitor.

### Depende de
- Solo necesita que DEV 2 confirme la semántica de retorno de `fireTransition`.

### Entrega a otro developer
- **A DEV 2**
  - Entregable: test/fixture de un Worker llamando repetidamente a `fireTransition`.
  - **Proviene de:** preparación de `T2.2.2`.
  - Motivo: DEV 2 puede usarlo como consumidor real de su interfaz.

### Criterio de terminado
El Worker se puede ejecutar contra un mock y terminar limpiamente.

---

# Semana 2 — Workers reales y cierre

## T2.2.2 — `[TASK] Implementar cantidad real de workers y secuencias`

### Qué hay que hacer
Usando la tabla de DEV 1:

1. crear la cantidad correcta de workers;
2. asignar a cada uno su secuencia;
3. iniciar todos;
4. coordinar objetivo global de ejecución;
5. detenerlos cuando se alcance el criterio de finalización;
6. hacer `join`/espera equivalente desde `Main`.

### Resultado esperado
Todos los threads reales ejecutan las secuencias definidas por el modelo.

### Depende de
- `T1.2.1` de DEV 1.
- `T2.2.1` de DEV 2.

### Entrega a otro developer
- **A DEV 2**
  - Entregable: escenario de ejecución con workers reales.
  - **Proviene de:** `T2.2.2`.
  - Motivo: permite probar bloqueo/reactivación del Monitor en condiciones reales.

- **A DEV 4**
  - Entregable: forma de arrancar y esperar una corrida completa.
  - **Proviene de:** `T2.2.2`.
  - Motivo: DEV 4 necesita invocar ejecuciones desde el runner experimental.

### Criterio de terminado
El programa inicia todos los workers y termina sin usar `System.exit()`.

---

## T2.2.3 — `[TEST] Validar carga, interrupciones y cierre`

### Qué hay que hacer
Crear tests para comprobar:

1. todos los workers terminan;
2. no quedan threads activos;
3. interrupción durante espera;
4. múltiples corridas consecutivas;
5. ejecución bajo alta contención.

### Resultado esperado
Suite de pruebas de lifecycle.

### Depende de
- `T2.2.2`.
- Monitor real de DEV 2.

### Entrega a otro developer
- **A DEV 2**
  - Entregable: escenarios que generan contención y cierre.
  - **Proviene de:** `T2.2.3`.
  - Motivo: sirven para validar deadlocks y pérdida de señales.

- **A DEV 4**
  - Entregable: evidencia de ausencia de threads residuales.
  - **Proviene de:** `T2.2.3`.
  - Motivo: es un requisito explícito del enunciado.

### Criterio de terminado
Después de cada test no queda ningún hilo de aplicación activo.

---

# Semana 3 — Semántica temporal

## T3.2.1 — `[SPIKE] Definir tiempos y estimar duración`

### Qué hay que hacer
Para T2, T3, T5, T7 y T8:

1. elegir un valor inicial en ms;
2. documentar los valores;
3. calcular una estimación analítica de la duración de una corrida;
4. indicar qué transiciones dominan el tiempo total.

### Resultado esperado
`docs/analisis-temporal.md` con configuración inicial y cálculo.

### Depende de
- `T1.2.1` de DEV 1 para conocer secuencias.

### Entrega a otro developer
- **A DEV 4**
  - Entregable: configuración temporal inicial.
  - **Proviene de:** `T3.2.1`.
  - Motivo: DEV 4 debe poder seleccionar esa configuración desde el runner.

### Criterio de terminado
La duración esperada puede explicarse sin depender únicamente de mediciones.

---

## T3.2.2 — `[TASK] Implementar tiempos configurables`

### Qué hay que hacer
Implementar la semántica temporal de:

- T2
- T3
- T5
- T7
- T8

Los tiempos deben:

- estar centralizados;
- poder modificarse sin tocar la lógica;
- no convertir el Monitor completo en una sección crítica durante la espera.

### Resultado esperado
Configuración temporal seleccionable.

### Depende de
- `T2.1.3` de DEV 2 para acordar dónde se aplica el tiempo.

### Entrega a otro developer
- **A DEV 4**
  - Entregable: API/parámetro para seleccionar configuración temporal.
  - **Proviene de:** `T3.2.2`.
  - Motivo: necesario para automatizar campañas.

### Criterio de terminado
Se pueden ejecutar dos juegos de tiempos distintos sin modificar el código fuente.

---

# Semana 4 — Ajustar a 20–40 segundos

## T3.2.3 — `[PERF] Comparar tiempos y ajustar duración`

### Qué hay que hacer
Ejecutar varias configuraciones y registrar:

- tiempo esperado;
- tiempo real;
- diferencia;
- efecto sobre throughput;
- efecto de las políticas.

Elegir una configuración final donde una corrida requerida dure entre 20 y 40 segundos.

### Resultado esperado
Tabla comparativa y configuración final.

### Depende de
- `T4.2.1` de DEV 4: protocolo de corrida.
- runner de DEV 4.
- núcleo estable de DEV 2.

### Entrega a otro developer
- **A DEV 4**
  - Entregable: configuración temporal final.
  - **Proviene de:** `T3.2.3`.
  - Motivo: DEV 4 la usa en las campañas definitivas.

### Criterio de terminado
La duración total medida está dentro de 20–40 s en corridas repetidas.

---

# Semana 5 — Informe y defensa

## Trabajo de cierre

### Qué hay que hacer
Preparar:

- explicación de workers;
- mecanismo de finalización;
- evidencia de no dejar threads;
- tiempos elegidos;
- análisis analítico;
- resultados prácticos;
- conclusiones.

### Entrega a otro developer
- **A DEV 4**
  - Entregable: sección de workers y temporalidad del informe.
  - **Proviene de:** `T2.2.2`, `T2.2.3`, `T3.2.1`, `T3.2.2`, `T3.2.3`.
  - Motivo: DEV 4 integra el documento final.

---

# Resumen de dependencias de DEV 3

| Necesita | De | Proviene de tarea | Para ejecutar |
|---|---|---|---|
| Secuencias + cantidad de hilos | DEV 1 | T1.2.1 | T2.2.2 |
| Monitor con bloqueo/reactivación | DEV 2 | T2.2.1 | T2.2.2/T2.2.3 |
| Semántica de fireTransition | DEV 2 | T2.1.3 | Worker |
| Protocolo experimental | DEV 4 | T4.2.1 | T3.2.3 |
| Runner | DEV 4 | T4.2.2 preparación | T3.2.3 |
