# Developer 1 — Modelo formal, invariantes y validación


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

DEV 1 es responsable de transformar la Red de Petri del enunciado en una **especificación formal utilizable por el código**.

Su trabajo debe responder concretamente:

- cuál es el marcado inicial;
- cuál es la matriz de incidencia;
- qué transiciones están asociadas a cada flujo;
- cuáles son los P-invariantes;
- cuáles son los T-invariantes;
- cuántos hilos son necesarios;
- qué secuencia ejecuta cada tipo de hilo;
- cómo comprobar posteriormente que la implementación respeta esos invariantes.

---

# Semana 1 — Obtener y documentar el modelo formal

## T1.1.1 — `[SPIKE] Validar la red oficial en PIPE`

### Qué hay que hacer
1. Abrir la red oficial en PIPE.
2. Verificar que coincida con la figura del enunciado.
3. Ejecutar los análisis de:
   - deadlock;
   - vivacidad;
   - seguridad.
4. Guardar capturas o exportes de los resultados.

### Resultado esperado
Un documento `docs/pipe-analysis.md` con:
- propiedades obtenidas;
- capturas;
- conclusión breve de cada propiedad.

### Depende de
Nadie.

### Entrega a otro developer
- **A DEV 2**
  - Entregable: confirmación de que la red usada es la correcta.
  - **Proviene de:** `T1.1.1`.
  - Motivo: DEV 2 debe implementar exactamente esa red y no una interpretación distinta.

### Criterio de terminado
La red fue validada en PIPE y las tres propiedades solicitadas quedaron documentadas.

---

## T1.1.2 — `[TASK] Documentar matrices, marcado, estados, eventos e invariantes`

### Qué hay que hacer
Extraer y documentar:

1. Marcado inicial `M0`.
2. Matriz de incidencia.
3. Lista de plazas P0..P9.
4. Lista de transiciones T0..T9.
5. Tabla:
   - transición;
   - evento/acción representada;
   - plazas de entrada;
   - plazas de salida.
6. P-invariantes.
7. T-invariantes.
8. Interpretación de cada invariante en el sistema de pagos.

### Resultado esperado
Archivo `docs/modelo-rdp.md` con toda la información anterior en formato utilizable por el equipo.

### Depende de
`T1.1.1`.

### Entrega a otro developer
- **A DEV 2**
  - Entregable: `M0`, matriz de incidencia y cantidad de plazas/transiciones.
  - **Proviene de:** `T1.1.2`.
  - Motivo: son los datos que DEV 2 necesita para implementar `PetriNet`.

- **A DEV 3**
  - Entregable: listado de transiciones por flujo.
  - **Proviene de:** `T1.1.2`.
  - Motivo: DEV 3 necesita saber qué secuencias terminarán ejecutando los workers.

- **A DEV 4**
  - Entregable: identificación formal del flujo de alto riesgo y conflictos de ruteo.
  - **Proviene de:** `T1.1.2`.
  - Motivo: DEV 4 necesita saber qué alternativa debe priorizar la política.

### Criterio de terminado
Otra persona del equipo puede reconstruir la red usando únicamente `docs/modelo-rdp.md`.

---

# Semana 2 — Determinar hilos y secuencias

## T1.2.1 — `[SPIKE] Determinar cantidad y responsabilidad de hilos`

### Qué hay que hacer
Aplicar las reglas del enunciado sobre conflictos y joins.

Para cada hilo identificado, registrar:

- nombre temporal;
- transición inicial;
- secuencia de transiciones;
- T-invariante relacionado;
- conflicto que resuelve o camino que ejecuta;
- motivo por el cual ese hilo existe.

### Resultado esperado
Tabla `docs/hilos-rdp.md` con la cantidad exacta de tipos de hilo y sus secuencias.

### Depende de
`T1.1.2`.

### Entrega a otro developer
- **A DEV 3**
  - Entregable: tabla final de workers y secuencias.
  - **Proviene de:** `T1.2.1`.
  - Motivo: DEV 3 implementa los workers reales y no debe deducir nuevamente la topología.

- **A DEV 4**
  - Entregable: definición de los T-invariantes que deben contarse durante una corrida.
  - **Proviene de:** `T1.2.1`.
  - Motivo: DEV 4 necesita definir qué significa completar 200 invariantes.

### Criterio de terminado
La cantidad de hilos queda justificada con reglas del enunciado y no por conveniencia del código.

---

## T1.2.2 — `[DOCS] Dibujar responsabilidades de hilos`

### Qué hay que hacer
Crear la imagen solicitada por el enunciado:

- una copia de la RdP;
- un color por tipo de hilo;
- flechas que indiquen qué transiciones ejecuta cada hilo;
- leyenda con nombre de cada hilo.

### Resultado esperado
`docs/img/responsabilidad-hilos.png`.

### Depende de
`T1.2.1`.

### Entrega a otro developer
- **A DEV 3**
  - Entregable: gráfico final de responsabilidades.
  - **Proviene de:** `T1.2.2`.
  - Motivo: sirve como referencia visual al implementar las secuencias.

- **A DEV 4**
  - Entregable: imagen final para informe.
  - **Proviene de:** `T1.2.2`.
  - Motivo: DEV 4 integra los entregables académicos.

### Criterio de terminado
Cada transición ejecutada por un worker está identificada visualmente.

---

# Semana 3 — Verificadores de invariantes

## T4.1.2 — `[TASK] Verificar P-invariantes después de cada disparo`

### Qué hay que hacer
Implementar un componente que:

1. reciba el marcado actual;
2. evalúe todos los P-invariantes;
3. devuelva éxito/fallo;
4. indique cuál invariante falló y con qué marcado.

### Resultado esperado
Clase/componente `InvariantChecker` con tests.

### Depende de
`T1.1.2`.

### Entrega a otro developer
- **A DEV 2**
  - Entregable: API del `InvariantChecker`.
  - **Proviene de:** `T4.1.2`.
  - Motivo: DEV 2 debe invocarlo luego de cada disparo exitoso dentro del Monitor.

### Criterio de terminado
Un marcado correcto pasa; un marcado alterado deliberadamente falla.

---

## T4.1.3 — `[TASK] Verificar T-invariantes desde el log usando regex`

### Qué hay que hacer
1. Tomar el formato de log definitivo.
2. Construir regex para reconocer las secuencias correspondientes a T-invariantes.
3. Contar cuántos invariantes de cada tipo se completaron.
4. Detectar secuencias incompletas o inválidas.

### Resultado esperado
Analizador de log ejecutable con tests.

### Depende de
- `T1.2.1`.
- `T4.1.1` de DEV 4: formato definitivo del log.

### Entrega a otro developer
- **A DEV 4**
  - Entregable: analizador y contador de T-invariantes.
  - **Proviene de:** `T4.1.3`.
  - Motivo: DEV 4 lo usa para validar campañas de 200 invariantes.

### Criterio de terminado
El analizador reconoce correctamente logs válidos e inválidos de prueba.

---

# Semana 4 — Validación negativa y soporte a campañas

## T4.1.4 — `[TEST] Probar los verificadores con errores intencionales`

### Qué hay que hacer
Crear al menos estos casos:

1. marcado que viola un P-invariante;
2. log con una transición faltante;
3. log con transición fuera de secuencia;
4. log con invariante incompleto.

### Resultado esperado
Suite de tests negativos.

### Depende de
`T4.1.2` y `T4.1.3`.

### Entrega a otro developer
- **A DEV 4**
  - Entregable: evidencia de que los verificadores detectan errores.
  - **Proviene de:** `T4.1.4`.
  - Motivo: se incorpora como evidencia de validación en el informe.

### Criterio de terminado
Todos los errores intencionales son detectados.

---

# Semana 5 — Informe y defensa

## Trabajo de cierre

### Qué hay que hacer
Redactar y entregar a DEV 4:

- propiedades PIPE;
- matriz y marcado;
- tablas de estados/eventos;
- P/T-invariantes;
- interpretación;
- determinación de hilos;
- gráfico de hilos;
- validación formal.

### Entrega a otro developer
- **A DEV 4**
  - Entregable: sección formal del informe.
  - **Proviene de:** `T1.1.1`, `T1.1.2`, `T1.2.1`, `T1.2.2`, `T4.1.2`, `T4.1.3`, `T4.1.4`.
  - Motivo: DEV 4 consolida el informe final.

---

# Resumen de dependencias de DEV 1

| Necesita | De | Proviene de tarea | Para ejecutar |
|---|---|---|---|
| Formato definitivo de log | DEV 4 | T4.1.1 | T4.1.3 |
| Logs reales de campañas | DEV 4 | T4.2.2 | Validación final |
