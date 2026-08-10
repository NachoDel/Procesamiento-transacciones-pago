# Developer 4 — Políticas, logging, experimentos e integración


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

DEV 4 implementa las piezas que permiten:

- elegir cómo resolver conflictos;
- registrar lo que ocurre;
- repetir ejecuciones;
- medir resultados;
- compilar y ejecutar el proyecto de forma portable;
- integrar el informe y los entregables.

No debe convertirse en owner del código de los demás.

---

# Semana 1 — Build común y contrato de políticas

## T5.1.1 — `[TASK] Preparar proyecto ejecutable común`

### Qué hay que hacer
Definir:

- versión de Java;
- Maven o Gradle;
- estructura de carpetas;
- comando de build;
- comando de test;
- `Main` mínimo;
- `.gitignore`;
- README inicial.

### Resultado esperado
Todo el equipo puede clonar y ejecutar:

```bash
<build-command>
<test-command>
<run-command>
```

sin depender del IDE.

### Depende de
Nadie.

### Entrega a otro developer
- **A DEV 1, DEV 2 y DEV 3**
  - Entregable: estructura inicial del repositorio.
  - **Proviene de:** `T5.1.1`.
  - Motivo: todos deben trabajar desde la misma base.

### Criterio de terminado
Un segundo integrante clona y ejecuta el proyecto desde consola.

---

## T3.1.1 — `[TASK] Definir Policy e implementar política aleatoria`

### Qué hay que hacer
Crear una interfaz `Policy` que reciba las alternativas válidas y devuelva una elección.

Implementar `RandomPolicy`:

- elige entre candidatos;
- permite seed fija;
- no modifica el estado de la RdP;
- no conoce detalles internos del Monitor.

### Resultado esperado
`Policy` + `RandomPolicy` con tests.

### Depende de
Necesita acordar contrato con DEV 2, pero no requiere el Monitor terminado.

### Entrega a otro developer
- **A DEV 2**
  - Entregable: interfaz `Policy`.
  - **Proviene de:** `T3.1.1`.
  - Motivo: DEV 2 la inyecta dentro del Monitor en `T2.1.3`.

### Criterio de terminado
Con una seed fija, el test produce siempre la misma secuencia de elecciones.

---

# Semana 2 — Prioridad y logging

## T3.1.2 — `[TASK] Implementar política priorizada`

### Qué hay que hacer
Cuando el conflicto incluya el flujo de alto riesgo:

1. elegir alto riesgo;
2. si no está disponible, elegir entre alternativas restantes;
3. documentar qué sucede con fairness/starvation.

### Resultado esperado
`PriorityPolicy` con tests.

### Depende de
- `T1.1.2` de DEV 1 para saber qué transición representa alto riesgo.

### Entrega a otro developer
- **A DEV 2**
  - Entregable: `PriorityPolicy`.
  - **Proviene de:** `T3.1.2`.
  - Motivo: DEV 2 valida la integración real del Monitor con la política.

### Criterio de terminado
Cuando existe conflicto y alto riesgo está habilitado, siempre se elige ese flujo.

---

## T3.1.3 — `[TEST] Probar ambas políticas`

### Qué hay que hacer
Probar:

- aleatoria con seed fija;
- aleatoria con distintas seeds;
- prioridad cuando hay conflicto;
- prioridad cuando alto riesgo no puede ejecutarse;
- ausencia de candidatos.

### Resultado esperado
Tests unitarios de políticas.

### Depende de
`T3.1.1` y `T3.1.2`.

### Entrega a otro developer
- **A DEV 2**
  - Entregable: casos de prueba esperados para selección.
  - **Proviene de:** `T3.1.3`.
  - Motivo: DEV 2 puede comprobar que el Monitor le entrega correctamente los candidatos.

### Criterio de terminado
Las políticas están validadas sin depender de ejecución concurrente real.

---

## T4.1.1 — `[TASK] Implementar log parseable y thread-safe`

### Qué hay que hacer
Definir un formato estable que registre, como mínimo:

- orden del evento;
- thread;
- transición disparada;
- timestamp;
- política;
- seed/configuración de corrida.

El archivo debe:

- ser thread-safe;
- mantener un orden inequívoco;
- poder parsearse automáticamente.

### Resultado esperado
Logger + especificación del formato.

### Depende de
Nadie para comenzar.

### Entrega a otro developer
- **A DEV 1**
  - Entregable: especificación del log.
  - **Proviene de:** `T4.1.1`.
  - Motivo: DEV 1 construye las regex de `T4.1.3`.

- **A DEV 2**
  - Entregable: API del logger.
  - **Proviene de:** `T4.1.1`.
  - Motivo: DEV 2 lo integra en el punto donde el disparo ya fue confirmado.

### Criterio de terminado
Dos threads pueden registrar eventos sin corromper el archivo.

---

# Semana 3 — Protocolo experimental y runner

## T4.2.1 — `[SPIKE] Definir cómo se ejecutarán los experimentos`

### Qué hay que hacer
Definir por escrito:

1. qué significa “1 T-invariante completado”;
2. cómo se detiene la ejecución al llegar a 200;
3. número de corridas por política;
4. seeds;
5. configuraciones temporales;
6. métricas a guardar;
7. nombres de archivos.

### Resultado esperado
`docs/protocolo-experimentos.md`.

### Depende de
- `T1.2.1` de DEV 1 para definición de T-invariantes.

### Entrega a otro developer
- **A DEV 3**
  - Entregable: protocolo exacto de corrida.
  - **Proviene de:** `T4.2.1`.
  - Motivo: DEV 3 debe usar el mismo criterio al ajustar tiempos.

- **A DEV 1**
  - Entregable: forma exacta en que se generan y nombran logs.
  - **Proviene de:** `T4.2.1`.
  - Motivo: DEV 1 valida los mismos archivos que se usarán en campañas.

### Criterio de terminado
Dos integrantes pueden ejecutar el mismo experimento y obtener datos comparables.

---

## Runner experimental

### Qué hay que hacer
Permitir seleccionar por argumentos/configuración:

- política;
- seed;
- tiempos;
- objetivo de invariantes;
- ubicación del log.

### Resultado esperado
Ejecución reproducible desde consola.

### Depende de
- `T2.2.2` de DEV 3: cómo iniciar una corrida.
- `T3.2.2` de DEV 3: configuración de tiempos.

### Entrega a otro developer
- **A DEV 3**
  - Entregable: runner reproducible.
  - **Proviene de:** implementación operativa asociada a `T4.2.1`.
  - Motivo: DEV 3 lo usa en `T3.2.3`.

### Criterio de terminado
Una corrida puede repetirse con los mismos parámetros sin tocar código.

---

# Semana 4 — Campañas y portabilidad

## T4.2.2 — `[PERF] Ejecutar campañas`

### Qué hay que hacer
Ejecutar las combinaciones acordadas:

- política aleatoria;
- política priorizada;
- configuración temporal A;
- configuración temporal B;
- 200 invariantes por corrida.

Guardar por corrida:

- parámetros;
- log;
- duración;
- conteo por invariante;
- resultado de verificación.

### Resultado esperado
Carpeta `results/` organizada por ejecución.

### Depende de
- `T4.1.3` de DEV 1.
- `T3.2.3` de DEV 3.
- núcleo estable de DEV 2.

### Entrega a otro developer
- **A DEV 1**
  - Entregable: logs reales.
  - **Proviene de:** `T4.2.2`.
  - Motivo: DEV 1 ejecuta verificación final de invariantes.

- **A DEV 3**
  - Entregable: tiempos medidos de las campañas.
  - **Proviene de:** `T4.2.2`.
  - Motivo: DEV 3 compara estimación analítica y práctica.

### Criterio de terminado
Todas las campañas definidas tienen resultados completos y verificables.

---

## T4.2.3 — `[DOCS] Analizar resultados`

### Qué hay que hacer
Preparar tablas con:

- cantidad de cada T-invariante;
- porcentaje de distribución;
- comparación entre políticas;
- tiempos;
- efecto de cambiar la temporalidad;
- conclusiones.

### Resultado esperado
Sección experimental lista para informe.

### Depende de
`T4.2.2`.

### Entrega a otro developer
- No bloquea implementación; se integra al informe final.

### Criterio de terminado
Cada conclusión está respaldada por una tabla/log identificable.

---

## T5.1.2 — `[TEST] Validar portabilidad`

### Qué hay que hacer
Probar desde entorno limpio:

1. clone;
2. build;
3. tests;
4. ejecución;
5. generación de log.

Eliminar rutas absolutas y pasos manuales.

### Resultado esperado
README reproducible.

### Depende de
Proyecto integrado.

### Entrega a otro developer
- **A TODO EL EQUIPO**
  - Entregable: procedimiento final de ejecución.
  - **Proviene de:** `T5.1.2`.
  - Motivo: todos deben poder defender cómo ejecutar el proyecto.

### Criterio de terminado
El proyecto corre sin IDE y sin configuración adicional específica del equipo.

---

# Semana 5 — Diagramas, informe y entrega

## T5.1.3 — `[DOCS] Crear diagramas`

### Qué hay que hacer
Crear:

1. diagrama de clases;
2. diagrama de secuencia de `fireTransition`.

Pedir revisión a cada owner.

### Depende de
Código estabilizado.

### Entrega a otro developer
- **A DEV 2**
  - Entregable: borrador del diagrama de secuencia.
  - **Proviene de:** `T5.1.3`.
  - Motivo: DEV 2 debe confirmar que representa correctamente el Monitor.

### Criterio de terminado
Los diagramas coinciden con el código real.

---

## T5.1.4 — `[DOCS] Integrar informe y trazabilidad`

### Qué hay que hacer
Integrar aportes de DEV 1, 2 y 3.

Crear matriz:

`Requisito → issue → clase/archivo → prueba → evidencia`

### Entrega recibida de otros developers
- DEV 1:
  - proviene de `T1.1.1`, `T1.1.2`, `T1.2.1`, `T1.2.2`, `T4.1.2`, `T4.1.3`, `T4.1.4`.
- DEV 2:
  - proviene de `T2.1.1`, `T2.1.2`, `T2.1.3`, `T2.2.1`.
- DEV 3:
  - proviene de `T2.2.2`, `T2.2.3`, `T3.2.1`, `T3.2.2`, `T3.2.3`.

### Criterio de terminado
Todo requisito del enunciado apunta a evidencia concreta.

---

## T5.1.5 — `[TASK] Preparar entrega y defensa`

### Qué hay que hacer
1. build limpio;
2. ejecutar caso final;
3. comprobar logs;
4. comprobar diagramas;
5. comprobar informe;
6. preparar paquete de entrega;
7. realizar ensayo de defensa.

### Resultado esperado
Versión final entregable.

### Depende de
Todas las tareas de cierre.

### Criterio de terminado
El equipo puede demostrar el sistema desde cero siguiendo solo el README.

---

# Resumen de dependencias de DEV 4

| Necesita | De | Proviene de tarea | Para ejecutar |
|---|---|---|---|
| Identificación de alto riesgo | DEV 1 | T1.1.2 | T3.1.2 |
| T-invariantes y conteo | DEV 1 | T1.2.1/T4.1.3 | T4.2.1/T4.2.2 |
| Integración Policy/Logger | DEV 2 | T2.1.3 | Campañas |
| Inicio/fin de corrida | DEV 3 | T2.2.2 | Runner |
| Configuración temporal | DEV 3 | T3.2.2/T3.2.3 | Campañas |
| Núcleo estable | DEV 2 | T2.2.1 + stress | T4.2.2 |
