# Backlog GitHub — TP Final Programación Concurrente 2026

**Versión:** 1.0 — BORRADOR  
**Fecha de elaboración:** 2026-08-04  
**Alcance:** vista previa; no se crearon issues ni se ejecutaron comandos sobre un repositorio.

## Fuentes analizadas

| Fuente | Tipo | Uso en esta versión | Autoridad |
|---|---|---|---|
| Prompt de backlog para GitHub | Instrucción del usuario | Define jerarquía, plantillas, metadatos, control de duplicados y formato de esta entrega. | Máxima para el formato |
| Enunciado TP Final Concurrente 2026.pdf | Enunciado oficial FCEFyN-UNC | Define dominio PSP, red de Petri, monitor, políticas, temporalidad, pruebas, entregables y condiciones. | Máxima para requisitos |
| Mapa maestro de contenidos del proyecto.pdf | Instrucción de proyecto | Define protocolo de fuentes, separación entre evidencia e interpretación, riesgos, aprendizaje y registro de decisiones. | Alta |
| Utilización de Redes de Petri para la enseñanza de la concurrencia en la Ingeniería en Computación | Artículo académico UNC | Sustenta la arquitectura Monitor–RdP–Política–colas de bloqueados–hilos–acciones y la validación por invariantes. | Alta conceptual |
| Ecuación de estado generalizada para redes de Petri no autónomas y con distintos tipos de arcos | Artículo académico UNC | Sustenta ecuación de estado, sensibilización, disparo y extensiones temporales/eventos. | Alta conceptual |
| CONCURRENTEcontenido .pdf | Programa oficial de la asignatura | Aporta resultados de aprendizaje y criterios de evaluación: diseño, organización, implementación, documentación y defensa. | Alta académica |
| Conversaciones del proyecto | Contexto del proyecto | Se solicitó comenzar el desarrollo según el mapa maestro; no se encontraron decisiones técnicas adicionales confirmadas. | Media |
| Código/repositorio GitHub | Fuente prevista | No fue proporcionado ni conectado; no fue posible revisar estructura, código ni issues existentes. | POR DEFINIR |

No se realizó búsqueda web: la primera versión puede construirse con las fuentes del proyecto. Las decisiones de JDK, build, fairness y protocolo experimental permanecen abiertas.

## Interpretación del proyecto

El trabajo consiste en simular y ejecutar en Java una red de Petri que representa un núcleo simplificado de procesamiento de pagos. Varias instancias concurrentes recorren tres flujos y compiten por dos recursos compartidos. El comportamiento debe estar gobernado por un monitor genérico, una política intercambiable y semántica temporal, preservando invariantes y permitiendo terminar una carga de prueba reproducible. Además del código, la solución debe producir evidencia formal, experimental y documental suficiente para la evaluación individual y la defensa.

### Actores y componentes detectados

- Actores: equipo de estudiantes, docente/evaluador y operador de la ejecución desde `Main`.
- Componentes: modelo de Red de Petri, `Monitor`, `MonitorInterface`, `Política`, hilos trabajadores, temporización, logger, verificadores de invariantes, analizador regex, PIPE, build y documentación.

### Datos faltantes o ambiguos

- Archivo PIPE oficial y versión de PIPE.
- Repositorio/código/issues existentes: no fue posible comprobar duplicados.
- Versión de Java, sistema de build y framework de pruebas.
- Semántica exacta del `boolean` de `fireTransition`.
- Probabilidad objetivo de la política aleatoria y garantía de fairness de la priorizada.
- Regla exacta para contar ‘200 invariantes completados’ y cantidad de corridas.
- El requisito 9 menciona registrar resultados del punto 7, aunque el análisis de distribución aparece en el punto 8.
- Fecha efectiva de entrega: el documento indica 10/06/2026, anterior a esta versión.

## Requisitos detectados

- **R01:** Implementar el proyecto en Java utilizando objetos y colecciones.
- **R02:** Modelar el sistema de transacciones de pago con tres flujos y recursos compartidos P7/P8, salida P9 y arribo/admisión P0/P1.
- **R03:** Determinar con PIPE deadlock, vivacidad y seguridad de la red.
- **R04:** Identificar e interpretar invariantes de plaza e invariantes de transición.
- **R05:** Documentar tabla de estados y tabla de eventos del sistema.
- **R06:** Determinar y justificar la cantidad de hilos para el mayor paralelismo posible; producir gráfico de responsabilidades por hilo.
- **R07:** Implementar MonitorInterface.fireTransition(int); fireTransition debe ser el único método público de Monitor y Monitor debe ser agnóstico a transiciones concretas.
- **R08:** Implementar temporalidad en T2, T3, T5, T7 y T8 con tiempos en milisegundos elegidos por el grupo.
- **R09:** Analizar tiempos analítica y experimentalmente, variar configuraciones y mantener la ejecución entre 20 y 40 segundos.
- **R10:** Implementar y analizar por separado una política aleatoria y una política que priorice el flujo de alto riesgo.
- **R11:** Proveer una clase Main que inicie el programa.
- **R12:** Finalizar sin hilos activos residuales.
- **R13:** Producir diagramas de clases y de secuencia; el de secuencia debe mostrar fireTransition y el uso de la política.
- **R14:** Realizar múltiples ejecuciones de 200 invariantes completados por corrida y demostrar distribución de políticas y cantidad por tipo de invariante.
- **R15:** Registrar resultados en un archivo de log para análisis posterior.
- **R16:** Verificar invariantes de plaza luego de cada disparo.
- **R17:** Verificar invariantes de transición desde el log mediante expresiones regulares.
- **R18:** El proyecto debe correr independientemente del sistema operativo y del IDE, sin configuración adicional; incluir dependencias externas si se usan.
- **R19:** Entregar imágenes de diagramas, código fuente Java e informe obligatorio; todos los integrantes deben subir el trabajo al LEV.
- **R20:** Explicar correctamente los problemas y conceptos de concurrencia; la evaluación es individual y contempla defensa.

# Bloque 1: resumen del backlog

| ID | Tipo | Título | Padre | Prioridad | Tamaño | Dependencias | Requisito | Definición |
|---|---|---|---|---|---|---|---|---|
| E1 | EPIC | [EPIC] Validar el modelo formal y la estrategia de ejecución | — | P0 | XL | Archivo PIPE oficial y acceso a PIPE | R02–R06 | BORRADOR |
| E2 | EPIC | [EPIC] Ejecutar la red mediante un monitor concurrente genérico | — | P0 | XL | E1 | R01, R07, R11, R12, R18 | DEFINIDA |
| E3 | EPIC | [EPIC] Resolver conflictos con políticas y semántica temporal | — | P1 | XL | E1, E2 | R08–R10 | BORRADOR |
| E4 | EPIC | [EPIC] Producir evidencia verificable de corrección y rendimiento | — | P1 | XL | E1–E3 | R14–R17 | BORRADOR |
| E5 | EPIC | [EPIC] Entregar un proyecto portable, documentado y defendible | — | P1 | XL | E1–E4 | R11, R13, R18–R20 | BORRADOR |
| HU1.1 | STORY | [STORY] Validar la red y sus propiedades formales | E1 | P0 | L | Archivo PIPE oficial | R02–R05 | BORRADOR |
| HU1.2 | STORY | [STORY] Definir la cantidad y responsabilidad de los hilos | E1 | P0 | L | HU1.1 | R06 | BORRADOR |
| HU2.1 | STORY | [STORY] Representar y ejecutar la red sin acoplarla al monitor | E2 | P0 | L | HU1.1 | R01, R07 | DEFINIDA |
| HU2.2 | STORY | [STORY] Ejecutar trabajadores y finalizar sin hilos residuales | E2 | P0 | L | HU1.2, HU2.1 | R11, R12, R14 | BORRADOR |
| HU3.1 | STORY | [STORY] Resolver conflictos mediante políticas intercambiables | E3 | P1 | L | HU2.1 | R10 | BORRADOR |
| HU3.2 | STORY | [STORY] Incorporar semántica temporal y analizar tiempos | E3 | P1 | L | HU2.1 | R08, R09 | BORRADOR |
| HU4.1 | STORY | [STORY] Registrar y verificar formalmente cada ejecución | E4 | P1 | L | HU2.1 | R15–R17 | DEFINIDA |
| HU4.2 | STORY | [STORY] Demostrar políticas, conteos y tiempos con múltiples corridas | E4 | P1 | L | HU3.1, HU3.2, HU4.1 | R14, R09 | BORRADOR |
| HU5.1 | STORY | [STORY] Preparar la entrega portable y la defensa académica | E5 | P1 | L | E1–E4 | R11, R13, R18–R20 | BORRADOR |
| T1.1.1 | SPIKE | [SPIKE] Recuperar y validar la red oficial en PIPE | HU1.1 | P0 | S | Acceso al enlace/archivo oficial | R02, R03 | BORRADOR |
| T1.1.2 | TASK | [TASK] Documentar matrices, estados, eventos, propiedades e invariantes | HU1.1 | P0 | M | T1.1.1 | R03–R05 | BORRADOR |
| T1.2.1 | SPIKE | [SPIKE] Derivar secuencias, conflictos, joins y cantidad de hilos | HU1.2 | P0 | M | T1.1.2 | R06 | BORRADOR |
| T1.2.2 | DOCS | [DOCS] Crear el gráfico de responsabilidades y registrar la decisión de hilos | HU1.2 | P1 | S | T1.2.1 | R06 | BORRADOR |
| T2.1.1 | TASK | [TASK] Implementar el modelo genérico de Red de Petri | HU2.1 | P0 | M | T1.1.2 | R01, R07 | DEFINIDA |
| T2.1.2 | TEST | [TEST] Validar sensibilización y ecuación de estado | HU2.1 | P0 | S | T2.1.1 | R04, R07 | DEFINIDA |
| T2.1.3 | TASK | [TASK] Implementar MonitorInterface con disparo atómico y política inyectada | HU2.1 | P0 | L | T2.1.1, T3.1.1 parcialmente | R07 | DEFINIDA |
| T2.2.1 | TASK | [TASK] Implementar espera y reactivación de hilos bloqueados | HU2.2 | P0 | L | T2.1.3 | R07, R12 | DEFINIDA |
| T2.2.2 | TASK | [TASK] Implementar trabajadores, secuencias y contador de finalización | HU2.2 | P0 | M | T1.2.2, T2.2.1 | R06, R11, R12, R14 | BORRADOR |
| T2.2.3 | TEST | [TEST] Someter el núcleo a carga, interrupciones y cierre | HU2.2 | P0 | M | T2.2.2 | R12, R14 | DEFINIDA |
| T3.1.1 | TASK | [TASK] Definir Política e implementar selección aleatoria reproducible | HU3.1 | P1 | M | T2.1.1 | R10 | BORRADOR |
| T3.1.2 | TASK | [TASK] Implementar prioridad de alto riesgo y decidir fairness | HU3.1 | P1 | M | T3.1.1 | R10 | BORRADOR |
| T3.1.3 | TEST | [TEST] Validar políticas por separado y bajo conflicto | HU3.1 | P1 | S | T3.1.1, T3.1.2 | R10, R14 | DEFINIDA |
| T3.2.1 | SPIKE | [SPIKE] Elegir tiempos y construir el modelo analítico de duración | HU3.2 | P1 | M | HU1.2, T2.2.2 | R08, R09 | BORRADOR |
| T3.2.2 | TASK | [TASK] Implementar semántica temporal configurable | HU3.2 | P1 | L | T3.2.1, T2.1.3 | R08 | BORRADOR |
| T3.2.3 | PERF | [PERF] Comparar configuraciones temporales y ajustar la ventana total | HU3.2 | P1 | M | T3.2.2, HU4.1 | R09 | BORRADOR |
| T4.1.1 | TASK | [TASK] Definir e implementar un log concurrente parseable | HU4.1 | P1 | M | T2.1.3 | R15 | DEFINIDA |
| T4.1.2 | TASK | [TASK] Verificar P-invariantes después de cada disparo | HU4.1 | P0 | M | T1.1.2, T2.1.3 | R16 | BORRADOR |
| T4.1.3 | TASK | [TASK] Analizar T-invariantes del log mediante expresiones regulares | HU4.1 | P1 | M | T1.1.2, T4.1.1 | R17 | BORRADOR |
| T4.1.4 | TEST | [TEST] Inyectar violaciones y validar los verificadores | HU4.1 | P1 | S | T4.1.2, T4.1.3 | R16, R17 | DEFINIDA |
| T4.2.1 | SPIKE | [SPIKE] Definir el protocolo experimental y la regla de 200 invariantes | HU4.2 | P0 | S | T1.1.2 | R14 | BORRADOR |
| T4.2.2 | PERF | [PERF] Ejecutar campañas por política y configuración temporal | HU4.2 | P1 | M | T3.2.3, T4.1.3, T4.2.1 | R09, R10, R14 | BORRADOR |
| T4.2.3 | DOCS | [DOCS] Interpretar distribución, invariantes y tiempos | HU4.2 | P1 | M | T4.2.2 | R09, R14 | BORRADOR |
| T5.1.1 | TASK | [TASK] Definir JDK, build y punto de entrada Main | HU5.1 | P1 | M | T2.2.2 | R11, R18, R20 | BORRADOR |
| T5.1.2 | TEST | [TEST] Validar portabilidad y ausencia de configuración adicional | HU5.1 | P1 | M | T5.1.1 | R18 | BORRADOR |
| T5.1.3 | DOCS | [DOCS] Crear diagramas de clases y secuencia coherentes con el código | HU5.1 | P1 | M | T2.1.3, T3.1.2 | R13, R19 | DEFINIDA |
| T5.1.4 | DOCS | [DOCS] Redactar informe y matriz de trazabilidad | HU5.1 | P1 | L | E1–E4 | R03–R20 | DEFINIDA |
| T5.1.5 | TASK | [TASK] Preparar paquete final y ensayo de defensa | HU5.1 | P1 | M | T5.1.2–T5.1.4 | R19, R20 | BORRADOR |

# Bloque 2: árbol jerárquico

E1 — Validar el modelo formal y la estrategia de ejecución
├── HU1.1 — Validar la red y sus propiedades formales
│   ├── T1.1.1 — Recuperar y validar la red oficial en PIPE
│   └── T1.1.2 — Documentar matrices, estados, eventos, propiedades e invariantes
└── HU1.2 — Definir la cantidad y responsabilidad de los hilos
    ├── T1.2.1 — Derivar secuencias, conflictos, joins y cantidad de hilos
    └── T1.2.2 — Crear el gráfico de responsabilidades y registrar la decisión de hilos

E2 — Ejecutar la red mediante un monitor concurrente genérico
├── HU2.1 — Representar y ejecutar la red sin acoplarla al monitor
│   ├── T2.1.1 — Implementar el modelo genérico de Red de Petri
│   ├── T2.1.2 — Validar sensibilización y ecuación de estado
│   └── T2.1.3 — Implementar MonitorInterface con disparo atómico y política inyectada
└── HU2.2 — Ejecutar trabajadores y finalizar sin hilos residuales
    ├── T2.2.1 — Implementar espera y reactivación de hilos bloqueados
    ├── T2.2.2 — Implementar trabajadores, secuencias y contador de finalización
    └── T2.2.3 — Someter el núcleo a carga, interrupciones y cierre

E3 — Resolver conflictos con políticas y semántica temporal
├── HU3.1 — Resolver conflictos mediante políticas intercambiables
│   ├── T3.1.1 — Definir Política e implementar selección aleatoria reproducible
│   ├── T3.1.2 — Implementar prioridad de alto riesgo y decidir fairness
│   └── T3.1.3 — Validar políticas por separado y bajo conflicto
└── HU3.2 — Incorporar semántica temporal y analizar tiempos
    ├── T3.2.1 — Elegir tiempos y construir el modelo analítico de duración
    ├── T3.2.2 — Implementar semántica temporal configurable
    └── T3.2.3 — Comparar configuraciones temporales y ajustar la ventana total

E4 — Producir evidencia verificable de corrección y rendimiento
├── HU4.1 — Registrar y verificar formalmente cada ejecución
│   ├── T4.1.1 — Definir e implementar un log concurrente parseable
│   ├── T4.1.2 — Verificar P-invariantes después de cada disparo
│   ├── T4.1.3 — Analizar T-invariantes del log mediante expresiones regulares
│   └── T4.1.4 — Inyectar violaciones y validar los verificadores
└── HU4.2 — Demostrar políticas, conteos y tiempos con múltiples corridas
    ├── T4.2.1 — Definir el protocolo experimental y la regla de 200 invariantes
    ├── T4.2.2 — Ejecutar campañas por política y configuración temporal
    └── T4.2.3 — Interpretar distribución, invariantes y tiempos

E5 — Entregar un proyecto portable, documentado y defendible
└── HU5.1 — Preparar la entrega portable y la defensa académica
    ├── T5.1.1 — Definir JDK, build y punto de entrada Main
    ├── T5.1.2 — Validar portabilidad y ausencia de configuración adicional
    ├── T5.1.3 — Crear diagramas de clases y secuencia coherentes con el código
    ├── T5.1.4 — Redactar informe y matriz de trazabilidad
    └── T5.1.5 — Preparar paquete final y ensayo de defensa


# Bloque 3: issues listas para copiar en GitHub


## E1

### Título

`[EPIC] Validar el modelo formal y la estrategia de ejecución`

### Descripción

#### Objetivo
Obtener una definición formal verificable de la red y una estrategia de hilos trazable al modelo.

#### Motivación
Las matrices, propiedades, invariantes, secuencias y cantidad de hilos son la base de toda la implementación y de la defensa académica.

#### Resultado esperado
Modelo validado en PIPE, tablas de estados/eventos, invariantes interpretados y diagrama de responsabilidades de hilos.

#### Alcance
Red oficial, propiedades, invariantes, conflictos, joins y máximo paralelismo.

#### Fuera de alcance
Implementación completa del monitor y decisiones de temporalidad.

#### Historias incluidas
HU1.1, HU1.2

#### Requisitos relacionados
R02–R06

#### Conceptos de programación concurrente
Redes de Petri, marcado, sensibilización, ecuación de estado, deadlock, vivacidad, seguridad, P/T-invariantes, paralelismo.

#### Riesgos principales
Trabajar con una reconstrucción incorrecta de la red; contar mal invariantes; derivar una cantidad de hilos incompatible con el modelo.

#### Dependencias
Archivo PIPE oficial y acceso a PIPE

#### Criterios de finalización de la épica
La red analizada coincide con la fuente oficial; resultados reproducibles en PIPE; invariantes y responsabilidades documentados; ambigüedades registradas.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; artículo UNC sobre RdP y cantidad/responsabilidad de hilos

#### Metadatos sugeridos
Tipo: EPIC · Prioridad: P0 · Tamaño: XL · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: epic, priority::p0, draft · Milestone: M1 — Modelo formal · Responsable sugerido: Analista de Redes de Petri


---


## E2

### Título

`[EPIC] Ejecutar la red mediante un monitor concurrente genérico`

### Descripción

#### Objetivo
Implementar el núcleo concurrente que ejecute cualquier red representada por el modelo sin referencias a transiciones concretas.

#### Motivación
El monitor es el mecanismo evaluado para preservar atomicidad, sincronización y progreso de la red.

#### Resultado esperado
Modelo Java, monitor genérico, hilos trabajadores y finalización ordenada con pruebas concurrentes.

#### Alcance
Estado compartido, disparo atómico, bloqueo/reactivación, trabajadores y cierre.

#### Fuera de alcance
Políticas concretas, análisis temporal y documentación final.

#### Historias incluidas
HU2.1, HU2.2

#### Requisitos relacionados
R01, R07, R11, R12, R18

#### Conceptos de programación concurrente
Monitor, exclusión mutua, variables de condición, atomicidad, visibilidad de memoria, estado compartido, finalización ordenada.

#### Riesgos principales
Condiciones de carrera, pérdida de señales, deadlock, corrupción del marcado, hilos residuales y acoplamiento de Monitor a la red.

#### Dependencias
E1

#### Criterios de finalización de la épica
API pública conforme; disparos atómicos; monitor agnóstico; pruebas de carga aprobadas; ejecución termina sin hilos vivos.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; artículo UNC sobre arquitectura Monitor–RdP–Política; bibliografía de monitores/Java

#### Metadatos sugeridos
Tipo: EPIC · Prioridad: P0 · Tamaño: XL · Estado inicial: Backlog · Estado de definición: DEFINIDA · Labels: epic, priority::p0 · Milestone: M2 — Núcleo concurrente · Responsable sugerido: Responsable del núcleo concurrente


---


## E3

### Título

`[EPIC] Resolver conflictos con políticas y semántica temporal`

### Descripción

#### Objetivo
Permitir ejecutar la misma red con políticas intercambiables y transiciones temporales medibles.

#### Motivación
El enunciado exige comparar política aleatoria y priorizada, además de justificar el comportamiento temporal.

#### Resultado esperado
Dos políticas ejecutables por separado y temporalidad configurable que permite cumplir la ventana de 20–40 segundos.

#### Alcance
Contrato de política, selección de transiciones, prioridad, fairness, tiempos y experimentos.

#### Fuera de alcance
Nuevos flujos de negocio o políticas no solicitadas.

#### Historias incluidas
HU3.1, HU3.2

#### Requisitos relacionados
R08–R10

#### Conceptos de programación concurrente
Resolución de conflictos, no determinismo controlado, prioridad, starvation, fairness, semántica temporal, timeouts.

#### Riesgos principales
Inanición de flujos no prioritarios; sesgo no documentado de la aleatoriedad; temporizadores iniciados en el momento incorrecto; pruebas frágiles.

#### Dependencias
E1, E2

#### Criterios de finalización de la épica
Ambas políticas son configurables y se ensayan por separado; temporalidad validada analítica y prácticamente; ventana total cumplida.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; ecuación de estado generalizada y material de RdP temporales

#### Metadatos sugeridos
Tipo: EPIC · Prioridad: P1 · Tamaño: XL · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: epic, priority::p1, draft · Milestone: M3 — Políticas y tiempo · Responsable sugerido: Responsable de políticas y rendimiento


---


## E4

### Título

`[EPIC] Producir evidencia verificable de corrección y rendimiento`

### Descripción

#### Objetivo
Generar logs, verificaciones y campañas experimentales que demuestren corrección formal y comportamiento bajo carga.

#### Motivación
En concurrencia, una ejecución exitosa no demuestra corrección; el TP exige verificación de invariantes y análisis de múltiples corridas.

#### Resultado esperado
Logs íntegros, chequeo online de P-invariantes, análisis regex de T-invariantes y resultados comparables de 200 invariantes.

#### Alcance
Observabilidad, verificadores, protocolo experimental, métricas y conclusiones.

#### Fuera de alcance
Plataforma de observabilidad externa o benchmarking de producción.

#### Historias incluidas
HU4.1, HU4.2

#### Requisitos relacionados
R14–R17

#### Conceptos de programación concurrente
Logging concurrente, invariantes, expresiones regulares, reproducibilidad, testing no determinista, métricas.

#### Riesgos principales
Eventos perdidos o reordenados en log; falsos positivos de regex; definición ambigua de ‘200 invariantes’; muestras insuficientes.

#### Dependencias
E1–E3

#### Criterios de finalización de la épica
Cada requisito de verificación tiene evidencia; fallos artificiales son detectados; campañas son reproducibles y sus conclusiones están justificadas.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: EPIC · Prioridad: P1 · Tamaño: XL · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: epic, priority::p1, draft · Milestone: M4 — Evidencia · Responsable sugerido: Responsable de testing y evidencia


---


## E5

### Título

`[EPIC] Entregar un proyecto portable, documentado y defendible`

### Descripción

#### Objetivo
Preparar una entrega ejecutable desde consola, con diagramas, informe, trazabilidad y material de defensa.

#### Motivación
La calidad del diseño, implementación, documentación y defensa forma parte explícita de la evaluación.

#### Resultado esperado
Paquete final reproducible, diagramas en buena calidad, informe obligatorio y guion de defensa.

#### Alcance
Build, Main, dependencias, portabilidad, UML, informe, trazabilidad y empaquetado.

#### Fuera de alcance
Despliegue en nube, interfaz gráfica o integración con un PSP real.

#### Historias incluidas
HU5.1

#### Requisitos relacionados
R11, R13, R18–R20

#### Conceptos de programación concurrente
Reproducibilidad, arquitectura, UML, documentación académica, trazabilidad y gestión de dependencias.

#### Riesgos principales
Dependencias no incluidas, instrucciones incompletas, divergencia entre diagramas y código, entrega fuera de fecha.

#### Dependencias
E1–E4

#### Criterios de finalización de la épica
Ejecución limpia documentada; artefactos completos; trazabilidad requisito–código–prueba–evidencia; revisión cruzada final.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: EPIC · Prioridad: P1 · Tamaño: XL · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: epic, priority::p1, draft · Milestone: M5 — Entrega · Responsable sugerido: Responsable de integración y documentación


---


## HU1.1

### Título

`[STORY] Validar la red y sus propiedades formales`

### Historia

Como equipo de desarrollo, necesitamos una red de Petri formalmente validada, para implementar exactamente el comportamiento solicitado y poder justificarlo.

### Descripción

#### Contexto
La Figura 1 define la topología, pero el archivo PIPE enlazado no está incluido entre las fuentes accesibles.

#### Resultado observable
Modelo oficial/importado, matrices y marcado inicial, tabla de estados/eventos, propiedades e invariantes documentados.

#### Alcance
Topología, marcado, sensibilización, propiedades e interpretación del dominio PSP.

#### Fuera de alcance
Código Java del monitor.

#### Criterios de aceptación
- Dado el archivo oficial, cuando se abre en PIPE, entonces la red coincide con la Figura 1.
- Se documentan matriz de incidencia, marcado inicial y correspondencia P0–P9/T0–T9.
- PIPE reporta deadlock, vivacidad y seguridad con evidencia adjunta.
- P/T-invariantes se listan e interpretan en términos de transacciones y recursos.

#### Escenarios de error
Archivo ausente o corrupto; discrepancia entre diagrama y PIPE; propiedad no calculable sin versión/herramienta compatible.

#### Requisitos relacionados
R02–R05

#### Conceptos de programación concurrente
RdP, ecuación de estado, sensibilización, marcado, propiedades e invariantes.

#### Riesgos concurrentes
Modelo incorrecto propagado a toda la solución.

#### Estrategia de pruebas
Reapertura del archivo, comparación visual y algebraica, revisión cruzada por otro integrante.

#### Evidencia esperada
Archivo PIPE o captura; tablas; salidas de análisis; documento de interpretación.

#### Dependencias
Archivo PIPE oficial

#### Sub-issues necesarias
T1.1.1, T1.1.2

#### Definición de terminado
Resultados revisados, fuentes identificadas y sin discrepancias abiertas de severidad alta.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; artículo UNC sobre RdP y cantidad/responsabilidad de hilos

#### Metadatos sugeridos
Tipo: STORY · Prioridad: P0 · Tamaño: L · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: story, priority::p0, draft · Milestone: M1 — Modelo formal · Responsable sugerido: Analista de Redes de Petri


---


## HU1.2

### Título

`[STORY] Definir la cantidad y responsabilidad de los hilos`

### Historia

Como equipo de desarrollo, necesitamos asignar secuencias de transición a hilos, para ejecutar la red con el mayor paralelismo permitido y justificar la arquitectura.

### Descripción

#### Contexto
El enunciado fija reglas específicas ante conflictos y joins y exige un gráfico coloreado.

#### Resultado observable
Cantidad de hilos, secuencias, transiciones compartidas y diagrama de responsabilidades con justificación.

#### Alcance
T-invariantes, conflictos, joins y tokens simultáneos.

#### Fuera de alcance
Implementación de Thread/Runnable.

#### Criterios de aceptación
- Cada transición queda cubierta por al menos una responsabilidad válida.
- Conflictos y joins se resuelven según los casos del enunciado.
- La cantidad de hilos se deriva del modelo y no de una preferencia de implementación.
- El diagrama distingue responsabilidades con colores y leyenda.

#### Escenarios de error
Invariante mal identificado; transición sin dueño; dos hilos consumen un recurso sin coordinación.

#### Requisitos relacionados
R06

#### Conceptos de programación concurrente
Paralelismo, secuencias de disparo, conflictos, joins y responsabilidad de hilos.

#### Riesgos concurrentes
Sobreparalelización, serialización innecesaria o deadlock estructural.

#### Estrategia de pruebas
Simulación manual de secuencias y revisión contra invariantes.

#### Evidencia esperada
Tabla hilo–secuencia–transiciones y gráfico versionado.

#### Dependencias
HU1.1

#### Sub-issues necesarias
T1.2.1, T1.2.2

#### Definición de terminado
Asignación aprobada por revisión técnica y lista para implementar.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; artículo UNC sobre RdP y cantidad/responsabilidad de hilos

#### Metadatos sugeridos
Tipo: STORY · Prioridad: P0 · Tamaño: L · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: story, priority::p0, draft · Milestone: M1 — Modelo formal · Responsable sugerido: Arquitecto de concurrencia


---


## HU2.1

### Título

`[STORY] Representar y ejecutar la red sin acoplarla al monitor`

### Historia

Como equipo de desarrollo, necesitamos encapsular la lógica de la red fuera de Monitor, para cambiar la red sin modificar el mecanismo de sincronización.

### Descripción

#### Contexto
Monitor debe ser agnóstico a transiciones puntuales y exponer únicamente fireTransition como método público.

#### Resultado observable
Modelo de red con cálculo de sensibilización y próximo marcado; Monitor genérico con disparo atómico.

#### Alcance
Datos de RdP, ecuación de estado, política inyectada y sección crítica.

#### Fuera de alcance
Trabajadores y campañas de 200 invariantes.

#### Criterios de aceptación
- Monitor no contiene condicionales ni constantes para T0–T9.
- fireTransition(int) es el único método público de instancia declarado en Monitor.
- Un disparo inválido no modifica el marcado.
- Un disparo válido actualiza el marcado de forma atómica.

#### Escenarios de error
Índice inválido; transición no sensibilizada; interrupción durante la espera; excepción de política.

#### Requisitos relacionados
R01, R07

#### Conceptos de programación concurrente
Encapsulamiento, monitor, atomicidad, visibilidad y estado compartido.

#### Riesgos concurrentes
Corrupción del marcado y API accidentalmente ampliada.

#### Estrategia de pruebas
Unitarias de RdP y pruebas concurrentes de múltiples solicitantes.

#### Evidencia esperada
Código, pruebas y reporte de reflexión/API.

#### Dependencias
HU1.1

#### Sub-issues necesarias
T2.1.1, T2.1.2, T2.1.3

#### Definición de terminado
Modelo y Monitor pasan pruebas funcionales/concurrentes y revisión de agnosticidad.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; artículo UNC sobre arquitectura Monitor–RdP–Política; bibliografía de monitores/Java

#### Metadatos sugeridos
Tipo: STORY · Prioridad: P0 · Tamaño: L · Estado inicial: Backlog · Estado de definición: DEFINIDA · Labels: story, priority::p0 · Milestone: M2 — Núcleo concurrente · Responsable sugerido: Responsable del núcleo concurrente


---


## HU2.2

### Título

`[STORY] Ejecutar trabajadores y finalizar sin hilos residuales`

### Historia

Como evaluador, quiero que la simulación complete la carga requerida y termine, para poder repetirla y verificar que no quedan hilos activos.

### Descripción

#### Contexto
La cantidad exacta de hilos depende de E1; el criterio ‘200 invariantes’ necesita una regla de conteo confirmada.

#### Resultado observable
Trabajadores con secuencias definidas, contador de finalización y cierre coordinado desde Main.

#### Alcance
Creación, ejecución, interrupción cooperativa, join y cierre de recursos.

#### Fuera de alcance
Lógica de políticas y análisis estadístico.

#### Criterios de aceptación
- Main inicia todos los componentes y espera su finalización.
- Al alcanzar el objetivo, no se aceptan nuevos ciclos y los hilos terminan cooperativamente.
- No quedan hilos no-daemon del proyecto activos.
- La misma corrida puede repetirse sin estado residual.

#### Escenarios de error
Trabajador bloqueado al finalizar; excepción no controlada; contador duplicado; cierre durante una transición.

#### Requisitos relacionados
R11, R12, R14

#### Conceptos de programación concurrente
Ciclo de vida de hilos, cancelación, interrupción, join, progreso y finalización ordenada.

#### Riesgos concurrentes
Thread leak, cierre anticipado, deadlock de shutdown.

#### Estrategia de pruebas
Timeout de suite, enumeración de hilos, ejecución repetida y fallos inyectados.

#### Evidencia esperada
Logs de inicio/cierre, prueba automatizada y recuento final.

#### Dependencias
HU1.2, HU2.1

#### Sub-issues necesarias
T2.2.1, T2.2.2, T2.2.3

#### Definición de terminado
Objetivo completado, recursos cerrados y pruebas de ciclo de vida aprobadas.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; artículo UNC sobre arquitectura Monitor–RdP–Política; bibliografía de monitores/Java

#### Metadatos sugeridos
Tipo: STORY · Prioridad: P0 · Tamaño: L · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: story, priority::p0, draft · Milestone: M2 — Núcleo concurrente · Responsable sugerido: Responsable del núcleo concurrente


---


## HU3.1

### Título

`[STORY] Resolver conflictos mediante políticas intercambiables`

### Historia

Como equipo de análisis, queremos ejecutar la misma red con una política aleatoria y otra priorizada, para comparar su efecto en la distribución de los invariantes.

### Descripción

#### Contexto
La probabilidad de la política aleatoria y la garantía de fairness de la priorizada no están especificadas.

#### Resultado observable
Contrato de Política, implementación aleatoria reproducible e implementación priorizada con decisión explícita sobre inanición.

#### Alcance
Selección entre transiciones sensibilizadas en conflicto.

#### Fuera de alcance
Cambios en topología o nuevos criterios de negocio.

#### Criterios de aceptación
- La política se puede cambiar sin modificar Monitor.
- Cada política se ejecuta y analiza de forma independiente.
- La aleatoria nunca elige una transición no candidata.
- La priorizada favorece el flujo de alto riesgo cuando compite por ambos recursos.
- La decisión sobre fairness queda documentada y probada.

#### Escenarios de error
Lista vacía; candidato inválido; semilla no controlable; inanición indefinida.

#### Requisitos relacionados
R10

#### Conceptos de programación concurrente
Política, no determinismo, prioridad, fairness y starvation.

#### Riesgos concurrentes
Sesgo, resultados irreproducibles o bloqueo de flujos secundarios.

#### Estrategia de pruebas
Casos deterministas con semilla, conflictos controlados y prueba prolongada.

#### Evidencia esperada
Pruebas, distribución observada y ADR de fairness.

#### Dependencias
HU2.1

#### Sub-issues necesarias
T3.1.1, T3.1.2, T3.1.3

#### Definición de terminado
Ambas políticas cumplen criterios y pueden seleccionarse por configuración documentada.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; ecuación de estado generalizada y material de RdP temporales

#### Metadatos sugeridos
Tipo: STORY · Prioridad: P1 · Tamaño: L · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: story, priority::p1, draft · Milestone: M3 — Políticas y tiempo · Responsable sugerido: Responsable de políticas


---


## HU3.2

### Título

`[STORY] Incorporar semántica temporal y analizar tiempos`

### Historia

Como evaluador, quiero observar tiempos configurables y justificados en las transiciones temporales, para contrastar el análisis analítico con la ejecución real.

### Descripción

#### Contexto
Los valores en milisegundos son elección del grupo y deben permitir una duración total entre 20 y 40 segundos.

#### Resultado observable
T2, T3, T5, T7 y T8 temporizadas, estimación analítica y comparación de configuraciones.

#### Alcance
Instante de inicio del temporizador, espera, revalidación y métricas.

#### Fuera de alcance
Tiempo real estricto o garantías hard real-time.

#### Criterios de aceptación
- Solo las transiciones indicadas reciben tiempo.
- El temporizador comienza según una semántica documentada.
- Se prueban al menos dos configuraciones de tiempos.
- La configuración final completa una corrida válida entre 20 y 40 segundos.
- Las diferencias entre tiempo estimado y real se explican.

#### Escenarios de error
Desensibilización durante la espera; interrupción; reloj no monotónico; espera dentro de la sección crítica.

#### Requisitos relacionados
R08, R09

#### Conceptos de programación concurrente
RdP temporal, reloj monotónico, espera condicional, timeout y rendimiento.

#### Riesgos concurrentes
Serialización por dormir con lock, medición sesgada o semántica incorrecta.

#### Estrategia de pruebas
Pruebas con reloj controlado cuando sea posible y campañas reales.

#### Evidencia esperada
Tabla de tiempos, fórmula/estimación, logs y gráficos/tablas comparativas.

#### Dependencias
HU2.1

#### Sub-issues necesarias
T3.2.1, T3.2.2, T3.2.3

#### Definición de terminado
Temporalidad validada y análisis incorporado al informe.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; ecuación de estado generalizada y material de RdP temporales

#### Metadatos sugeridos
Tipo: STORY · Prioridad: P1 · Tamaño: L · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: story, priority::p1, draft · Milestone: M3 — Políticas y tiempo · Responsable sugerido: Responsable de rendimiento


---


## HU4.1

### Título

`[STORY] Registrar y verificar formalmente cada ejecución`

### Historia

Como equipo de validación, necesitamos un registro íntegro y verificadores automáticos, para detectar violaciones de invariantes durante y después de la ejecución.

### Descripción

#### Contexto
El enunciado exige P-invariantes después de cada disparo y T-invariantes por regex sobre el log.

#### Resultado observable
Log parseable, chequeo online de P-invariantes y analizador regex de secuencias de transición.

#### Alcance
Eventos mínimos, identificación de hilo/política/tiempo, verificadores y fallos detectables.

#### Fuera de alcance
Observabilidad distribuida o base de datos.

#### Criterios de aceptación
- Cada disparo exitoso produce exactamente un registro completo.
- La verificación de P-invariantes se ejecuta tras la actualización atómica.
- El analizador regex clasifica los T-invariantes requeridos.
- Una violación artificial es detectada y reportada.

#### Escenarios de error
Escrituras intercaladas; log incompleto; regex ambigua; fallo del logger.

#### Requisitos relacionados
R15–R17

#### Conceptos de programación concurrente
Logging concurrente, integridad de eventos, invariantes y expresiones regulares.

#### Riesgos concurrentes
Evidencia no confiable o verificación fuera de orden.

#### Estrategia de pruebas
Pruebas de alta concurrencia, log truncado y secuencias inválidas.

#### Evidencia esperada
Archivos de ejemplo, pruebas y reporte del analizador.

#### Dependencias
HU2.1

#### Sub-issues necesarias
T4.1.1, T4.1.2, T4.1.3, T4.1.4

#### Definición de terminado
Verificadores detectan casos positivos y negativos y el formato está documentado.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: STORY · Prioridad: P1 · Tamaño: L · Estado inicial: Backlog · Estado de definición: DEFINIDA · Labels: story, priority::p1 · Milestone: M4 — Evidencia · Responsable sugerido: Responsable de testing y evidencia


---


## HU4.2

### Título

`[STORY] Demostrar políticas, conteos y tiempos con múltiples corridas`

### Historia

Como evaluador, quiero resultados repetibles de múltiples corridas, para comprobar la distribución de carga, el conteo de invariantes y el comportamiento temporal.

### Descripción

#### Contexto
El número de corridas y la semántica exacta de ‘200 invariantes completados’ están POR DEFINIR.

#### Resultado observable
Protocolo experimental, campañas por política/configuración y conclusiones respaldadas por datos.

#### Alcance
Semillas, parámetros, repeticiones, métricas, tablas y análisis.

#### Fuera de alcance
Inferencia estadística avanzada no solicitada.

#### Criterios de aceptación
- Cada corrida válida alcanza el objetivo de 200 según la regla documentada.
- Se ejecutan y separan resultados de ambas políticas.
- Se informa cantidad de cada tipo de invariante y distribución de carga.
- Se comparan configuraciones temporales y se justifican variaciones.
- Los resultados se pueden regenerar con instrucciones y parámetros guardados.

#### Escenarios de error
Corrida abortada; log inválido; semilla faltante; duración fuera de ventana.

#### Requisitos relacionados
R14, R09

#### Conceptos de programación concurrente
Diseño experimental, reproducibilidad, métricas y no determinismo.

#### Riesgos concurrentes
Conclusiones con pocas muestras o métricas inconsistentes.

#### Estrategia de pruebas
Validación automática de completitud antes de incluir una corrida.

#### Evidencia esperada
Dataset de logs, resumen tabular y sección de conclusiones.

#### Dependencias
HU3.1, HU3.2, HU4.1

#### Sub-issues necesarias
T4.2.1, T4.2.2, T4.2.3

#### Definición de terminado
Campaña completa, reproducible y revisada.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: STORY · Prioridad: P1 · Tamaño: L · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: story, priority::p1, draft · Milestone: M4 — Evidencia · Responsable sugerido: Responsable de rendimiento y evidencia


---


## HU5.1

### Título

`[STORY] Preparar la entrega portable y la defensa académica`

### Historia

Como docente evaluador, quiero ejecutar y comprender el proyecto sin depender del IDE del grupo, para revisar diseño, corrección y decisiones técnicas.

### Descripción

#### Contexto
Versión de Java, herramienta de build, repositorio y fecha efectiva de entrega están POR DEFINIR; la fecha oficial del documento ya pasó.

#### Resultado observable
Proyecto ejecutable desde Main, dependencias resueltas, UML coherente, informe trazable y paquete de entrega.

#### Alcance
Build, instrucciones, diagramas, informe, LEV y preparación de defensa.

#### Fuera de alcance
GUI, despliegue remoto o automatización de GitHub no solicitada.

#### Criterios de aceptación
- Un clon/paquete limpio compila y ejecuta desde consola siguiendo el README.
- No requiere configuración manual del IDE.
- Diagramas reflejan la versión entregada del código.
- El informe explica modelo, concurrencia, pruebas, políticas, tiempos y resultados.
- Cada requisito se vincula con código, prueba y evidencia.
- Todos los artefactos exigidos están incluidos.

#### Escenarios de error
Dependencia ausente; versión incompatible; diagrama desactualizado; instrucciones ambiguas.

#### Requisitos relacionados
R11, R13, R18–R20

#### Conceptos de programación concurrente
Portabilidad, gestión de dependencias, UML, escritura académica y trazabilidad.

#### Riesgos concurrentes
Entrega no reproducible o defensa inconsistente entre integrantes.

#### Estrategia de pruebas
Construcción desde entorno limpio y revisión cruzada de artefactos.

#### Evidencia esperada
README, comandos, imágenes, informe y paquete final.

#### Dependencias
E1–E4

#### Sub-issues necesarias
T5.1.1, T5.1.2, T5.1.3, T5.1.4, T5.1.5

#### Definición de terminado
Ensayo de entrega y defensa completado sin bloqueadores críticos.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: STORY · Prioridad: P1 · Tamaño: L · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: story, priority::p1, draft · Milestone: M5 — Entrega · Responsable sugerido: Responsable de integración y documentación


---


## T1.1.1

### Título

`[SPIKE] Recuperar y validar la red oficial en PIPE`

### Descripción

#### Objetivo
Obtener el archivo de la red y confirmar que coincide con la Figura 1.

#### Resultado esperado
Archivo versionado y evidencia de apertura/validación.

#### Trabajo incluido
Descarga o solicitud del archivo, identificación de versión de PIPE, comparación de plazas, transiciones, arcos y marcado.

#### Trabajo no incluido
Reimplementar la red en Java.

#### Componentes o archivos afectados
docs/model/, archivo .xml/.pipe: POR DEFINIR.

#### Consideraciones técnicas
No reconstruir silenciosamente el modelo desde la imagen si existe un archivo oficial.

#### Riesgos concurrentes
Topología o marcado incorrectos; incompatibilidad de versión.

#### Pasos de implementación sugeridos
1. Localizar el enlace oficial o solicitarlo.
1. Abrirlo en PIPE y registrar versión.
1. Comparar con la Figura 1.
1. Versionar una copia permitida o documentar su obtención.

#### Pruebas requeridas
Reapertura en un entorno limpio y comparación por segundo integrante.

#### Evidencia de finalización
Archivo/capturas y nota de validación.

#### Dependencias y bloqueadores
Acceso al enlace/archivo oficial

#### Definición de terminado
Fuente identificada, modelo abre sin errores y discrepancias resueltas o registradas. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; artículo UNC sobre RdP y cantidad/responsabilidad de hilos

#### Metadatos sugeridos
Tipo: SPIKE · Prioridad: P0 · Tamaño: S · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: spike, priority::p0, draft · Milestone: M1 — Modelo formal · Responsable sugerido: Analista de RdP


---


## T1.1.2

### Título

`[TASK] Documentar matrices, estados, eventos, propiedades e invariantes`

### Descripción

#### Objetivo
Transformar la red validada en artefactos formales utilizables por implementación y pruebas.

#### Resultado esperado
Matriz de incidencia, marcado inicial, tablas, propiedades e invariantes interpretados.

#### Trabajo incluido
Cálculo con PIPE, exportación de resultados y explicación de cada invariante.

#### Trabajo no incluido
Asignación definitiva de hilos.

#### Componentes o archivos afectados
docs/model/formal-analysis.md; archivos PIPE: POR DEFINIR.

#### Consideraciones técnicas
Distinguir evidencia de PIPE de interpretación del equipo.

#### Riesgos concurrentes
Error de transcripción; confundir seguridad con ausencia de deadlock; interpretación incompleta.

#### Pasos de implementación sugeridos
1. Exportar matrices y marcado.
1. Construir tabla P/T ↔ estado/evento del dominio.
1. Registrar deadlock, vivacidad y seguridad.
1. Listar P/T-invariantes y su significado.

#### Pruebas requeridas
Recalcular invariantes por método independiente o revisión manual.

#### Evidencia de finalización
Documento, tablas y capturas/salidas.

#### Dependencias y bloqueadores
T1.1.1

#### Definición de terminado
Artefactos revisados y referenciados por requisitos. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; artículo UNC sobre RdP y cantidad/responsabilidad de hilos

#### Metadatos sugeridos
Tipo: TASK · Prioridad: P0 · Tamaño: M · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: task, priority::p0, draft · Milestone: M1 — Modelo formal · Responsable sugerido: Analista de RdP


---


## T1.2.1

### Título

`[SPIKE] Derivar secuencias, conflictos, joins y cantidad de hilos`

### Descripción

#### Objetivo
Aplicar las reglas del enunciado y el artículo de referencia para determinar hilos y secuencias.

#### Resultado esperado
Propuesta técnica justificable de cantidad y responsabilidad de hilos.

#### Trabajo incluido
T-invariantes, transiciones previas a conflictos, joins y tokens simultáneos.

#### Trabajo no incluido
Código de trabajadores.

#### Componentes o archivos afectados
docs/architecture/thread-responsibilities.md.

#### Consideraciones técnicas
Registrar alternativas si existe más de una descomposición válida.

#### Riesgos concurrentes
Transición duplicada o no cubierta; secuencias que no preservan invariantes.

#### Pasos de implementación sugeridos
1. Mapear T-invariantes.
1. Marcar conflictos y joins.
1. Aplicar Caso 1/Caso 2.
1. Simular secuencias y seleccionar la propuesta.

#### Pruebas requeridas
Recorrido manual y simulación en PIPE.

#### Evidencia de finalización
Tabla de derivación y decisión técnica.

#### Dependencias y bloqueadores
T1.1.2

#### Definición de terminado
Cantidad y secuencias revisadas y sin transiciones huérfanas. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; artículo UNC sobre RdP y cantidad/responsabilidad de hilos

#### Metadatos sugeridos
Tipo: SPIKE · Prioridad: P0 · Tamaño: M · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: spike, priority::p0, draft · Milestone: M1 — Modelo formal · Responsable sugerido: Arquitecto de concurrencia


---


## T1.2.2

### Título

`[DOCS] Crear el gráfico de responsabilidades y registrar la decisión de hilos`

### Descripción

#### Objetivo
Comunicar visualmente la responsabilidad de cada tipo y cantidad de hilos.

#### Resultado esperado
Imagen en buena calidad, leyenda y ADR.

#### Trabajo incluido
Colores, flechas, secuencias, multiplicidad y justificación.

#### Trabajo no incluido
Diagrama de clases.

#### Componentes o archivos afectados
docs/diagrams/thread-responsibilities.*; docs/adr/ADR-threads.md.

#### Consideraciones técnicas
El gráfico debe ser legible en el informe y en la defensa.

#### Riesgos concurrentes
Colores ambiguos o divergencia con la tabla.

#### Pasos de implementación sugeridos
1. Seleccionar notación.
1. Dibujar sobre la red validada.
1. Agregar leyenda y multiplicidades.
1. Revisar contra la decisión.

#### Pruebas requeridas
Revisión cruzada tabla–gráfico–modelo.

#### Evidencia de finalización
PNG/SVG y ADR.

#### Dependencias y bloqueadores
T1.2.1

#### Definición de terminado
Imagen exportada y decisión enlazada desde el informe. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; artículo UNC sobre RdP y cantidad/responsabilidad de hilos

#### Metadatos sugeridos
Tipo: DOCS · Prioridad: P1 · Tamaño: S · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: docs, priority::p1, draft · Milestone: M1 — Modelo formal · Responsable sugerido: Responsable de arquitectura


---


## T2.1.1

### Título

`[TASK] Implementar el modelo genérico de Red de Petri`

### Descripción

#### Objetivo
Representar plazas, transiciones, marcado, matrices y temporalidad sin conocimiento del dominio PSP en Monitor.

#### Resultado esperado
Clases/estructuras inmutables o encapsuladas para consultar sensibilización y calcular próximo marcado.

#### Trabajo incluido
Validaciones de dimensiones, índices y copia defensiva.

#### Trabajo no incluido
Sincronización y política.

#### Componentes o archivos afectados
src/main/java/.../petri/: POR DEFINIR.

#### Consideraciones técnicas
Evitar exponer arrays mutables; separar configuración de estado mutable.

#### Riesgos concurrentes
Aliasing, índices inválidos y mutación fuera del monitor.

#### Pasos de implementación sugeridos
1. Definir modelo de configuración.
1. Definir estado/marcado.
1. Implementar sensibilización y ecuación de estado.
1. Validar entradas.

#### Pruebas requeridas
Redes mínimas, transición sensibilizada/no sensibilizada y dimensiones inválidas.

#### Evidencia de finalización
Código y pruebas unitarias.

#### Dependencias y bloqueadores
T1.1.2

#### Definición de terminado
API interna revisada, sin referencias T0–T9 en Monitor. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; artículo UNC sobre arquitectura Monitor–RdP–Política; bibliografía de monitores/Java

#### Metadatos sugeridos
Tipo: TASK · Prioridad: P0 · Tamaño: M · Estado inicial: Backlog · Estado de definición: DEFINIDA · Labels: task, priority::p0 · Milestone: M2 — Núcleo concurrente · Responsable sugerido: Desarrollador núcleo


---


## T2.1.2

### Título

`[TEST] Validar sensibilización y ecuación de estado`

### Descripción

#### Objetivo
Demostrar que el modelo calcula correctamente transiciones habilitadas y marcados siguientes.

#### Resultado esperado
Suite de pruebas deterministas con casos positivos y negativos.

#### Trabajo incluido
Matriz pequeña conocida y casos de la red oficial cuando esté disponible.

#### Trabajo no incluido
Pruebas multihilo.

#### Componentes o archivos afectados
src/test/java/.../petri/: POR DEFINIR.

#### Consideraciones técnicas
Comparar vectores completos, no solo la transición disparada.

#### Riesgos concurrentes
Pruebas que replican el mismo error del código.

#### Pasos de implementación sugeridos
1. Definir casos manuales.
1. Probar no negatividad y pesos.
1. Probar disparo y conservación esperada.
1. Agregar regresión de la red oficial.

#### Pruebas requeridas
La propia suite es la evidencia.

#### Evidencia de finalización
Reporte de tests.

#### Dependencias y bloqueadores
T2.1.1

#### Definición de terminado
Cobertura de casos límite y resultados revisados. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; artículo UNC sobre arquitectura Monitor–RdP–Política; bibliografía de monitores/Java

#### Metadatos sugeridos
Tipo: TEST · Prioridad: P0 · Tamaño: S · Estado inicial: Backlog · Estado de definición: DEFINIDA · Labels: test, priority::p0 · Milestone: M2 — Núcleo concurrente · Responsable sugerido: Responsable de testing


---


## T2.1.3

### Título

`[TASK] Implementar MonitorInterface con disparo atómico y política inyectada`

### Descripción

#### Objetivo
Implementar fireTransition(int) como única operación pública coordinando red, política y estado compartido.

#### Resultado esperado
Monitor genérico que serializa la decisión/actualización crítica y devuelve resultado conforme al contrato definido.

#### Trabajo incluido
Lock/monitor, verificación de sensibilización, política y actualización atómica.

#### Trabajo no incluido
Secuencias específicas de hilos.

#### Componentes o archivos afectados
MonitorInterface.java, Monitor.java: rutas POR DEFINIR.

#### Consideraciones técnicas
Definir semántica exacta del boolean; mantener fuera de la sección crítica operaciones lentas cuando sea seguro.

#### Riesgos concurrentes
Race TOCTOU, bloqueo prolongado, API pública extra y acoplamiento.

#### Pasos de implementación sugeridos
1. Definir contrato del boolean.
1. Implementar región crítica.
1. Integrar política por abstracción.
1. Revisar API con reflexión.

#### Pruebas requeridas
Concurrencia de solicitantes, transición no habilitada, índice inválido e inspección de métodos públicos.

#### Evidencia de finalización
Código, pruebas y diagrama de secuencia preliminar.

#### Dependencias y bloqueadores
T2.1.1, T3.1.1 parcialmente

#### Definición de terminado
Pruebas de atomicidad y agnosticidad aprobadas. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; artículo UNC sobre arquitectura Monitor–RdP–Política; bibliografía de monitores/Java

#### Metadatos sugeridos
Tipo: TASK · Prioridad: P0 · Tamaño: L · Estado inicial: Backlog · Estado de definición: DEFINIDA · Labels: task, priority::p0 · Milestone: M2 — Núcleo concurrente · Responsable sugerido: Responsable del núcleo concurrente


---


## T2.2.1

### Título

`[TASK] Implementar espera y reactivación de hilos bloqueados`

### Descripción

#### Objetivo
Bloquear eficientemente solicitudes no ejecutables y reactivarlas cuando el marcado cambie.

#### Resultado esperado
Mecanismo de condiciones/colas sin espera ocupada ni pérdida de señales.

#### Trabajo incluido
Predicados en bucle, señalización posterior al disparo e interrupción cooperativa.

#### Trabajo no incluido
Prioridad de negocio dentro de la cola salvo decisión explícita.

#### Componentes o archivos afectados
Monitor.java y soporte de condiciones: POR DEFINIR.

#### Consideraciones técnicas
Reevaluar el predicado al despertar; documentar orden de adquisición.

#### Riesgos concurrentes
Lost wakeup, spurious wakeup, starvation o deadlock.

#### Pasos de implementación sugeridos
1. Definir condición de espera.
1. Elegir una o varias colas.
1. Implementar await/signal con bucle.
1. Integrar cancelación.

#### Pruebas requeridas
Varios hilos bloqueados, despertar correcto, interrupción y cierre global.

#### Evidencia de finalización
Pruebas y trazas de bloqueo/desbloqueo.

#### Dependencias y bloqueadores
T2.1.3

#### Definición de terminado
Sin busy waiting y sin hilos bloqueados al finalizar. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; artículo UNC sobre arquitectura Monitor–RdP–Política; bibliografía de monitores/Java

#### Metadatos sugeridos
Tipo: TASK · Prioridad: P0 · Tamaño: L · Estado inicial: Backlog · Estado de definición: DEFINIDA · Labels: task, priority::p0 · Milestone: M2 — Núcleo concurrente · Responsable sugerido: Desarrollador núcleo


---


## T2.2.2

### Título

`[TASK] Implementar trabajadores, secuencias y contador de finalización`

### Descripción

#### Objetivo
Ejecutar las secuencias asignadas y detener la simulación al alcanzar el objetivo de invariantes.

#### Resultado esperado
Clases Runnable/Thread y coordinador de cierre desde Main.

#### Trabajo incluido
Creación según multiplicidad, bucle de secuencia, registro de completitud y stop cooperativo.

#### Trabajo no incluido
Cálculo de hilos.

#### Componentes o archivos afectados
src/main/java/.../worker/, Main.java: POR DEFINIR.

#### Consideraciones técnicas
El contador debe actualizarse una sola vez por invariante completo; regla exacta POR DEFINIR.

#### Riesgos concurrentes
Doble conteo, parada a mitad de ciclo y visibilidad del flag.

#### Pasos de implementación sugeridos
1. Representar secuencia.
1. Implementar trabajador.
1. Definir contador/objetivo.
1. Coordinar stop y join.

#### Pruebas requeridas
Objetivos pequeños, concurrencia alta y cierre durante bloqueos.

#### Evidencia de finalización
Código, pruebas y log de ciclo de vida.

#### Dependencias y bloqueadores
T1.2.2, T2.2.1

#### Definición de terminado
Main completa y no deja hilos activos. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; artículo UNC sobre arquitectura Monitor–RdP–Política; bibliografía de monitores/Java

#### Metadatos sugeridos
Tipo: TASK · Prioridad: P0 · Tamaño: M · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: task, priority::p0, draft · Milestone: M2 — Núcleo concurrente · Responsable sugerido: Desarrollador de integración


---


## T2.2.3

### Título

`[TEST] Someter el núcleo a carga, interrupciones y cierre`

### Descripción

#### Objetivo
Detectar corrupción, deadlock, starvation accidental y fugas de hilos.

#### Resultado esperado
Suite de stress acotada por timeout y casos de interrupción/fallo.

#### Trabajo incluido
Repetición, barreras de inicio, timeout global, inspección de marcado e hilos.

#### Trabajo no incluido
Campaña académica final.

#### Componentes o archivos afectados
src/test/java/.../concurrency/: POR DEFINIR.

#### Consideraciones técnicas
Evitar tests basados solo en sleeps; conservar semilla y diagnóstico.

#### Riesgos concurrentes
Flakiness y falsos negativos.

#### Pasos de implementación sugeridos
1. Diseñar inicio simultáneo.
1. Ejecutar múltiples iteraciones.
1. Inyectar interrupción/excepción.
1. Verificar invariantes y cierre.

#### Pruebas requeridas
Incluidos en la tarea.

#### Evidencia de finalización
Reporte estable en varias ejecuciones.

#### Dependencias y bloqueadores
T2.2.2

#### Definición de terminado
Sin fallos no explicados en campaña local definida. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; artículo UNC sobre arquitectura Monitor–RdP–Política; bibliografía de monitores/Java

#### Metadatos sugeridos
Tipo: TEST · Prioridad: P0 · Tamaño: M · Estado inicial: Backlog · Estado de definición: DEFINIDA · Labels: test, priority::p0 · Milestone: M2 — Núcleo concurrente · Responsable sugerido: Responsable de testing


---


## T3.1.1

### Título

`[TASK] Definir Política e implementar selección aleatoria reproducible`

### Descripción

#### Objetivo
Desacoplar la resolución de conflictos e implementar la política aleatoria.

#### Resultado esperado
Interfaz/estrategia Política y selección válida con semilla configurable.

#### Trabajo incluido
Contrato, candidatos sensibilizados, generador inyectable y configuración.

#### Trabajo no incluido
Prioridad de alto riesgo.

#### Componentes o archivos afectados
src/main/java/.../policy/: POR DEFINIR.

#### Consideraciones técnicas
La distribución objetivo (uniforme u otra) está POR DEFINIR; se recomienda uniformidad si no hay aclaración.

#### Riesgos concurrentes
Sesgo, selección inválida o irreproducibilidad.

#### Pasos de implementación sugeridos
1. Definir contrato.
1. Definir comportamiento con 0/1 candidatos.
1. Implementar RNG inyectado.
1. Registrar semilla.

#### Pruebas requeridas
Semilla fija, candidatos vacíos y distribución de muestra razonable.

#### Evidencia de finalización
Código, pruebas y ADR breve.

#### Dependencias y bloqueadores
T2.1.1

#### Definición de terminado
Política intercambiable y reproducible. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; ecuación de estado generalizada y material de RdP temporales

#### Metadatos sugeridos
Tipo: TASK · Prioridad: P1 · Tamaño: M · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: task, priority::p1, draft · Milestone: M3 — Políticas y tiempo · Responsable sugerido: Responsable de políticas


---


## T3.1.2

### Título

`[TASK] Implementar prioridad de alto riesgo y decidir fairness`

### Descripción

#### Objetivo
Favorecer el flujo que requiere P7 y P8 simultáneamente sin introducir una política no documentada.

#### Resultado esperado
Política priorizada y decisión explícita sobre prevención o aceptación de inanición.

#### Trabajo incluido
Regla de preferencia, fallback y, si se decide, mecanismo de envejecimiento/cuota.

#### Trabajo no incluido
Cambiar recursos de la red.

#### Componentes o archivos afectados
src/main/java/.../policy/PrioritizedPolicy.java: POR DEFINIR.

#### Consideraciones técnicas
El enunciado exige priorizar, pero no especifica garantía de servicio para otros flujos.

#### Riesgos concurrentes
Starvation de tarjeta/transferencia o violación de prioridad.

#### Pasos de implementación sugeridos
1. Identificar transición(es) del flujo alto riesgo.
1. Definir precedencia.
1. Evaluar fairness.
1. Registrar decisión y parámetros.

#### Pruebas requeridas
Conflictos controlados, carga sostenida y ausencia/presencia documentada de starvation.

#### Evidencia de finalización
Código, pruebas y ADR.

#### Dependencias y bloqueadores
T3.1.1

#### Definición de terminado
Prioridad observable y fairness justificada. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; ecuación de estado generalizada y material de RdP temporales

#### Metadatos sugeridos
Tipo: TASK · Prioridad: P1 · Tamaño: M · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: task, priority::p1, draft · Milestone: M3 — Políticas y tiempo · Responsable sugerido: Responsable de políticas


---


## T3.1.3

### Título

`[TEST] Validar políticas por separado y bajo conflicto`

### Descripción

#### Objetivo
Comprobar que cada política satisface su regla sin elegir transiciones inválidas.

#### Resultado esperado
Suite determinista y prueba prolongada.

#### Trabajo incluido
Casos con 0/1/múltiples candidatos, semillas y conflicto de alto riesgo.

#### Trabajo no incluido
Campaña de 200 invariantes final.

#### Componentes o archivos afectados
src/test/java/.../policy/: POR DEFINIR.

#### Consideraciones técnicas
No exigir proporciones exactas en muestras pequeñas.

#### Riesgos concurrentes
Test estadístico frágil.

#### Pasos de implementación sugeridos
1. Crear candidatos sintéticos.
1. Validar aleatoria.
1. Validar prioridad.
1. Ejecutar prueba prolongada de fairness.

#### Pruebas requeridas
Incluidos.

#### Evidencia de finalización
Reporte de tests.

#### Dependencias y bloqueadores
T3.1.1, T3.1.2

#### Definición de terminado
Resultados estables y documentados. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; ecuación de estado generalizada y material de RdP temporales

#### Metadatos sugeridos
Tipo: TEST · Prioridad: P1 · Tamaño: S · Estado inicial: Backlog · Estado de definición: DEFINIDA · Labels: test, priority::p1 · Milestone: M3 — Políticas y tiempo · Responsable sugerido: Responsable de testing


---


## T3.2.1

### Título

`[SPIKE] Elegir tiempos y construir el modelo analítico de duración`

### Descripción

#### Objetivo
Seleccionar valores iniciales para T2/T3/T5/T7/T8 y estimar la duración total.

#### Resultado esperado
Tabla de tiempos, hipótesis de paralelismo y fórmula/estimación por política.

#### Trabajo incluido
Camino crítico, contención P7/P8 y número de invariantes.

#### Trabajo no incluido
Optimización prematura de código.

#### Componentes o archivos afectados
docs/performance/time-model.md.

#### Consideraciones técnicas
Separar tiempo de transición, espera por recursos y overhead del runtime.

#### Riesgos concurrentes
Modelo analítico demasiado simplificado.

#### Pasos de implementación sugeridos
1. Definir semántica temporal.
1. Asignar valores iniciales.
1. Calcular escenarios mínimo/esperado.
1. Ajustar para ventana 20–40 s.

#### Pruebas requeridas
Contrastar con una ejecución piloto.

#### Evidencia de finalización
Documento y parámetros versionados.

#### Dependencias y bloqueadores
HU1.2, T2.2.2

#### Definición de terminado
Configuración inicial justificada y lista para implementar. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; ecuación de estado generalizada y material de RdP temporales

#### Metadatos sugeridos
Tipo: SPIKE · Prioridad: P1 · Tamaño: M · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: spike, priority::p1, draft · Milestone: M3 — Políticas y tiempo · Responsable sugerido: Responsable de rendimiento


---


## T3.2.2

### Título

`[TASK] Implementar semántica temporal configurable`

### Descripción

#### Objetivo
Aplicar esperas temporales a las transiciones indicadas preservando corrección y concurrencia.

#### Resultado esperado
Componente temporal genérico integrado al disparo.

#### Trabajo incluido
Reloj monotónico, cálculo de tiempo restante, revalidación y cancelación.

#### Trabajo no incluido
Garantías hard real-time.

#### Componentes o archivos afectados
src/main/java/.../time/: POR DEFINIR; integración Monitor/RdP.

#### Consideraciones técnicas
No mantener el lock durante una espera larga salvo justificación formal; definir reserva de tokens según semántica elegida.

#### Riesgos concurrentes
Desensibilización, doble disparo, bloqueo global o medición incorrecta.

#### Pasos de implementación sugeridos
1. Representar tiempos por transición.
1. Implementar reloj/espera.
1. Integrar con condición del monitor.
1. Registrar tiempos reales.

#### Pruebas requeridas
Transición inmediata, temporal, interrupción y competencia por recursos.

#### Evidencia de finalización
Código, pruebas y trazas temporales.

#### Dependencias y bloqueadores
T3.2.1, T2.1.3

#### Definición de terminado
Semántica documentada y pruebas aprobadas. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; ecuación de estado generalizada y material de RdP temporales

#### Metadatos sugeridos
Tipo: TASK · Prioridad: P1 · Tamaño: L · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: task, priority::p1, draft · Milestone: M3 — Políticas y tiempo · Responsable sugerido: Desarrollador núcleo


---


## T3.2.3

### Título

`[PERF] Comparar configuraciones temporales y ajustar la ventana total`

### Descripción

#### Objetivo
Medir al menos dos configuraciones y seleccionar una que complete la corrida entre 20 y 40 segundos.

#### Resultado esperado
Resultados medidos, desviación respecto del modelo y configuración final.

#### Trabajo incluido
Warm-up si aplica, reloj monotónico, varias repeticiones y captura de parámetros.

#### Trabajo no incluido
Benchmark entre JVMs salvo necesidad.

#### Componentes o archivos afectados
scripts/ o tests/perf/: POR DEFINIR; docs/performance/results.md.

#### Consideraciones técnicas
No alterar la lógica funcional para forzar un tiempo.

#### Riesgos concurrentes
Ruido del sistema y selección oportunista de resultados.

#### Pasos de implementación sugeridos
1. Definir configuraciones A/B.
1. Ejecutar repeticiones.
1. Calcular resumen.
1. Ajustar y justificar configuración final.

#### Pruebas requeridas
Validación automática de duración y 200 invariantes.

#### Evidencia de finalización
Logs, tabla y conclusión.

#### Dependencias y bloqueadores
T3.2.2, HU4.1

#### Definición de terminado
Ventana cumplida en el protocolo acordado. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; ecuación de estado generalizada y material de RdP temporales

#### Metadatos sugeridos
Tipo: PERF · Prioridad: P1 · Tamaño: M · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: perf, priority::p1, draft · Milestone: M3 — Políticas y tiempo · Responsable sugerido: Responsable de rendimiento


---


## T4.1.1

### Título

`[TASK] Definir e implementar un log concurrente parseable`

### Descripción

#### Objetivo
Registrar cada disparo y contexto suficiente para análisis posterior sin líneas corruptas.

#### Resultado esperado
Formato documentado y escritor thread-safe cerrado al finalizar.

#### Trabajo incluido
Secuencia global o timestamp, hilo, transición, marcado/resultado mínimo, política y parámetros de corrida.

#### Trabajo no incluido
Base de datos o dashboard.

#### Componentes o archivos afectados
src/main/java/.../logging/ y logs/: POR DEFINIR.

#### Consideraciones técnicas
El orden del archivo debe representar el orden de commit de disparos.

#### Riesgos concurrentes
Interleaving de líneas, pérdida por buffer o impacto excesivo.

#### Pasos de implementación sugeridos
1. Definir esquema.
1. Elegir mecanismo de escritura.
1. Integrar en punto de commit.
1. Cerrar/flush en shutdown.

#### Pruebas requeridas
Alta concurrencia, excepción de E/S simulada y parseo completo.

#### Evidencia de finalización
Especificación y logs de prueba.

#### Dependencias y bloqueadores
T2.1.3

#### Definición de terminado
Cada disparo exitoso tiene un registro único y parseable. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: TASK · Prioridad: P1 · Tamaño: M · Estado inicial: Backlog · Estado de definición: DEFINIDA · Labels: task, priority::p1 · Milestone: M4 — Evidencia · Responsable sugerido: Responsable de observabilidad


---


## T4.1.2

### Título

`[TASK] Verificar P-invariantes después de cada disparo`

### Descripción

#### Objetivo
Detectar de inmediato cualquier marcado que viole los invariantes de plaza.

#### Resultado esperado
Verificador integrado tras la actualización atómica con diagnóstico.

#### Trabajo incluido
Representación de invariantes, cálculo y política de fallo/registro.

#### Trabajo no incluido
Cálculo automático de invariantes en runtime.

#### Componentes o archivos afectados
src/main/java/.../verification/PlaceInvariantChecker.java: POR DEFINIR.

#### Consideraciones técnicas
La verificación debe observar el mismo marcado comprometido y no una copia intermedia.

#### Riesgos concurrentes
Chequeo fuera de la sección crítica o constante mal transcripta.

#### Pasos de implementación sugeridos
1. Codificar invariantes validados.
1. Implementar evaluación.
1. Invocar después del disparo.
1. Definir respuesta ante violación.

#### Pruebas requeridas
Marcados válidos e inválidos inyectados.

#### Evidencia de finalización
Pruebas y log de violación controlada.

#### Dependencias y bloqueadores
T1.1.2, T2.1.3

#### Definición de terminado
Violaciones se detectan de forma determinista. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: TASK · Prioridad: P0 · Tamaño: M · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: task, priority::p0, draft · Milestone: M4 — Evidencia · Responsable sugerido: Responsable de verificación


---


## T4.1.3

### Título

`[TASK] Analizar T-invariantes del log mediante expresiones regulares`

### Descripción

#### Objetivo
Clasificar y contar secuencias completas de transición usando regex como exige el enunciado.

#### Resultado esperado
Analizador de log, patrones documentados y resumen por tipo de invariante.

#### Trabajo incluido
Normalización de log a cadena/secuencia, regex por invariante y detección de residuo.

#### Trabajo no incluido
Parser formal que reemplace el requisito de regex.

#### Componentes o archivos afectados
src/main/java/.../analysis/ o scripts/: POR DEFINIR.

#### Consideraciones técnicas
La regex debe evitar coincidencias parciales o solapadas incorrectas.

#### Riesgos concurrentes
Falsos positivos, pérdida de orden o invariantes entrelazados difíciles de linealizar.

#### Pasos de implementación sugeridos
1. Definir representación lineal.
1. Diseñar patrones.
1. Implementar conteo.
1. Validar residuo y totales.

#### Pruebas requeridas
Secuencias válidas, truncadas, mezcladas e inválidas.

#### Evidencia de finalización
Patrones, pruebas y reporte de ejemplo.

#### Dependencias y bloqueadores
T1.1.2, T4.1.1

#### Definición de terminado
Conteos reproducibles y explicables en el informe. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: TASK · Prioridad: P1 · Tamaño: M · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: task, priority::p1, draft · Milestone: M4 — Evidencia · Responsable sugerido: Responsable de análisis


---


## T4.1.4

### Título

`[TEST] Inyectar violaciones y validar los verificadores`

### Descripción

#### Objetivo
Demostrar que los verificadores no solo aceptan ejecuciones correctas sino que detectan errores relevantes.

#### Resultado esperado
Suite negativa controlada.

#### Trabajo incluido
Marcado corrupto, transición faltante, secuencia truncada y log malformado.

#### Trabajo no incluido
Introducir fallos en producción.

#### Componentes o archivos afectados
src/test/java/.../verification/: POR DEFINIR.

#### Consideraciones técnicas
Los mecanismos de inyección deben quedar confinados a tests.

#### Riesgos concurrentes
Pruebas que no alcanzan el punto de verificación real.

#### Pasos de implementación sugeridos
1. Diseñar fallos.
1. Inyectarlos por fixtures.
1. Verificar diagnóstico.
1. Restaurar camino correcto.

#### Pruebas requeridas
Incluidos.

#### Evidencia de finalización
Reporte y ejemplos de error.

#### Dependencias y bloqueadores
T4.1.2, T4.1.3

#### Definición de terminado
Cada tipo de fallo esperado produce una detección inequívoca. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: TEST · Prioridad: P1 · Tamaño: S · Estado inicial: Backlog · Estado de definición: DEFINIDA · Labels: test, priority::p1 · Milestone: M4 — Evidencia · Responsable sugerido: Responsable de testing


---


## T4.2.1

### Título

`[SPIKE] Definir el protocolo experimental y la regla de 200 invariantes`

### Descripción

#### Objetivo
Eliminar ambigüedad sobre qué se cuenta, cuántas corridas se harán y qué métricas se aceptan.

#### Resultado esperado
Protocolo escrito y aprobado por el equipo/docente si corresponde.

#### Trabajo incluido
Unidad de completitud, corrida válida, número de repeticiones, semillas, políticas y configuraciones.

#### Trabajo no incluido
Ejecución de la campaña.

#### Componentes o archivos afectados
docs/testing/experimental-protocol.md.

#### Consideraciones técnicas
El enunciado dice ‘múltiples’ y ‘200 invariantes’ sin especificar cantidad ni regla exacta.

#### Riesgos concurrentes
Resultados no comparables o incumplimiento de la intención docente.

#### Pasos de implementación sugeridos
1. Proponer interpretación.
1. Buscar aclaración oficial si es posible.
1. Definir métricas y criterios de descarte.
1. Versionar protocolo.

#### Pruebas requeridas
Revisión de que el protocolo cubra R09, R10 y R14–R17.

#### Evidencia de finalización
Documento y, si existe, aclaración docente.

#### Dependencias y bloqueadores
T1.1.2

#### Definición de terminado
No quedan ambigüedades materiales para ejecutar la campaña. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: SPIKE · Prioridad: P0 · Tamaño: S · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: spike, priority::p0, draft · Milestone: M4 — Evidencia · Responsable sugerido: Product Owner/analista


---


## T4.2.2

### Título

`[PERF] Ejecutar campañas por política y configuración temporal`

### Descripción

#### Objetivo
Recolectar datos completos de múltiples corridas válidas.

#### Resultado esperado
Conjunto de logs y resumen automático por corrida.

#### Trabajo incluido
Ambas políticas separadas, 200 invariantes, tiempos, conteos y validaciones.

#### Trabajo no incluido
Selección manual de corridas favorables.

#### Componentes o archivos afectados
scripts/run-experiments.* y results/: POR DEFINIR.

#### Consideraciones técnicas
Guardar versión de código, semilla y parámetros por corrida.

#### Riesgos concurrentes
Datos incompletos, contaminación entre corridas o variación del entorno.

#### Pasos de implementación sugeridos
1. Automatizar ejecución.
1. Validar cada salida.
1. Repetir según protocolo.
1. Consolidar métricas.

#### Pruebas requeridas
Chequeo de completitud y checksum/identificador de parámetros.

#### Evidencia de finalización
Logs, CSV/tabla y comando reproducible.

#### Dependencias y bloqueadores
T3.2.3, T4.1.3, T4.2.1

#### Definición de terminado
Todas las combinaciones requeridas tienen corridas válidas. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: PERF · Prioridad: P1 · Tamaño: M · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: perf, priority::p1, draft · Milestone: M4 — Evidencia · Responsable sugerido: Responsable de rendimiento


---


## T4.2.3

### Título

`[DOCS] Interpretar distribución, invariantes y tiempos`

### Descripción

#### Objetivo
Convertir resultados en conclusiones académicas defendibles.

#### Resultado esperado
Sección de informe con tablas/gráficos, explicación causal y limitaciones.

#### Trabajo incluido
Comparación de políticas, conteo por invariante, duración y discrepancia analítico-práctica.

#### Trabajo no incluido
Afirmaciones de corrección basadas solo en promedios.

#### Componentes o archivos afectados
docs/report/results.md o informe final: POR DEFINIR.

#### Consideraciones técnicas
Separar observación, interpretación e inferencia.

#### Riesgos concurrentes
Sobreinterpretación o ocultar variabilidad.

#### Pasos de implementación sugeridos
1. Resumir datos.
1. Comparar contra expectativas.
1. Explicar contención y prioridad.
1. Registrar limitaciones.

#### Pruebas requeridas
Revisión de que cada conclusión tenga evidencia enlazada.

#### Evidencia de finalización
Sección final y tablas/gráficos.

#### Dependencias y bloqueadores
T4.2.2

#### Definición de terminado
Resultados trazables y revisados por un integrante distinto. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: DOCS · Prioridad: P1 · Tamaño: M · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: docs, priority::p1, draft · Milestone: M4 — Evidencia · Responsable sugerido: Responsable de documentación


---


## T5.1.1

### Título

`[TASK] Definir JDK, build y punto de entrada Main`

### Descripción

#### Objetivo
Permitir compilar y ejecutar el proyecto desde consola sin depender del IDE.

#### Resultado esperado
Versión Java fijada, build reproducible, dependencias declaradas y Main funcional.

#### Trabajo incluido
Maven/Gradle POR DEFINIR, wrapper si aplica, configuración y argumentos.

#### Trabajo no incluido
Contenedores salvo necesidad justificada.

#### Componentes o archivos afectados
pom.xml/build.gradle, wrapper, Main.java, README: POR DEFINIR.

#### Consideraciones técnicas
Elegir una versión disponible para el equipo/docente; no asumir Java 9 por la bibliografía.

#### Riesgos concurrentes
Dependencias implícitas o incompatibilidad de versión.

#### Pasos de implementación sugeridos
1. Confirmar JDK permitido.
1. Elegir build.
1. Declarar dependencias.
1. Exponer comando de ejecución.

#### Pruebas requeridas
Build limpio sin caché y ejecución desde consola.

#### Evidencia de finalización
Comandos y salida.

#### Dependencias y bloqueadores
T2.2.2

#### Definición de terminado
Un entorno limpio compila y arranca con una instrucción documentada. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: TASK · Prioridad: P1 · Tamaño: M · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: task, priority::p1, draft · Milestone: M5 — Entrega · Responsable sugerido: Responsable de integración


---


## T5.1.2

### Título

`[TEST] Validar portabilidad y ausencia de configuración adicional`

### Descripción

#### Objetivo
Comprobar que el paquete no depende de rutas, IDE ni sistema operativo del desarrollador.

#### Resultado esperado
Matriz de entornos probados y problemas corregidos.

#### Trabajo incluido
Al menos dos entornos disponibles para el grupo; separadores de ruta, encoding y permisos.

#### Trabajo no incluido
Garantizar todo SO existente.

#### Componentes o archivos afectados
README y scripts portables: POR DEFINIR.

#### Consideraciones técnicas
La interpretación práctica de ‘cualquier máquina’ debe documentarse.

#### Riesgos concurrentes
Rutas absolutas, locale, line endings o shell específico.

#### Pasos de implementación sugeridos
1. Crear copia limpia.
1. Ejecutar en entornos definidos.
1. Registrar fallos.
1. Corregir y repetir.

#### Pruebas requeridas
La matriz de ejecución es la prueba.

#### Evidencia de finalización
Tabla de entornos y logs.

#### Dependencias y bloqueadores
T5.1.1

#### Definición de terminado
Todos los entornos acordados completan una corrida válida. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: TEST · Prioridad: P1 · Tamaño: M · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: test, priority::p1, draft · Milestone: M5 — Entrega · Responsable sugerido: Responsable de QA


---


## T5.1.3

### Título

`[DOCS] Crear diagramas de clases y secuencia coherentes con el código`

### Descripción

#### Objetivo
Mostrar estructura y dinámica de fireTransition con la política.

#### Resultado esperado
Imágenes de alta calidad y fuentes editables.

#### Trabajo incluido
Clases principales, relaciones, participantes del disparo, lock/espera/política/actualización y retorno.

#### Trabajo no incluido
Diagramar cada clase auxiliar.

#### Componentes o archivos afectados
docs/diagrams/class.* y sequence-fire-transition.*.

#### Consideraciones técnicas
El diagrama debe reflejar la versión final, no una arquitectura aspiracional.

#### Riesgos concurrentes
Divergencia con código o notación ilegible.

#### Pasos de implementación sugeridos
1. Generar borrador desde arquitectura.
1. Comparar con código.
1. Revisar secuencia feliz y espera.
1. Exportar PNG/SVG.

#### Pruebas requeridas
Revisión cruzada código–diagrama.

#### Evidencia de finalización
Fuentes e imágenes.

#### Dependencias y bloqueadores
T2.1.3, T3.1.2

#### Definición de terminado
Diagramas legibles e incorporados al informe. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: DOCS · Prioridad: P1 · Tamaño: M · Estado inicial: Backlog · Estado de definición: DEFINIDA · Labels: docs, priority::p1 · Milestone: M5 — Entrega · Responsable sugerido: Responsable de arquitectura


---


## T5.1.4

### Título

`[DOCS] Redactar informe y matriz de trazabilidad`

### Descripción

#### Objetivo
Documentar modelo, código, decisiones, pruebas y resultados con trazabilidad completa.

#### Resultado esperado
Informe obligatorio y matriz requisito→issue→código→prueba→evidencia.

#### Trabajo incluido
Introducción, RdP, hilos, monitor, políticas, tiempo, invariantes, pruebas, resultados, riesgos y conclusiones.

#### Trabajo no incluido
Copiar bibliografía sin relación con decisiones.

#### Componentes o archivos afectados
docs/report/ o informe: POR DEFINIR.

#### Consideraciones técnicas
Distinguir requisitos oficiales, decisiones del equipo y recomendaciones.

#### Riesgos concurrentes
Documento descriptivo sin evidencia o desactualizado.

#### Pasos de implementación sugeridos
1. Crear índice.
1. Asignar secciones.
1. Insertar evidencia enlazada.
1. Revisar consistencia y fuentes.

#### Pruebas requeridas
Auditoría de cobertura de R01–R20.

#### Evidencia de finalización
Informe y matriz.

#### Dependencias y bloqueadores
E1–E4

#### Definición de terminado
Todos los requisitos cubiertos o marcados POR DEFINIR con justificación. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: DOCS · Prioridad: P1 · Tamaño: L · Estado inicial: Backlog · Estado de definición: DEFINIDA · Labels: docs, priority::p1 · Milestone: M5 — Entrega · Responsable sugerido: Responsable de documentación


---


## T5.1.5

### Título

`[TASK] Preparar paquete final y ensayo de defensa`

### Descripción

#### Objetivo
Entregar todos los artefactos y asegurar que cada integrante pueda explicar su parte y la solución global.

#### Resultado esperado
Paquete LEV, checklist, versión etiquetada y guion de defensa.

#### Trabajo incluido
Código, dependencias, imágenes, informe, logs representativos, instrucciones y responsabilidades.

#### Trabajo no incluido
Subir o crear issues reales sin autorización del usuario/equipo.

#### Componentes o archivos afectados
release/: POR DEFINIR; repositorio GitHub: POR DEFINIR.

#### Consideraciones técnicas
La fecha oficial 10/06/2026 está vencida respecto de la fecha actual; confirmar fecha efectiva.

#### Riesgos concurrentes
Archivo faltante, versión equivocada o defensa fragmentada.

#### Pasos de implementación sugeridos
1. Congelar versión.
1. Ejecutar checklist desde paquete.
1. Preparar demostración.
1. Ensayar preguntas por integrante.

#### Pruebas requeridas
Instalación/ejecución desde el paquete final.

#### Evidencia de finalización
Checklist firmado, tag/zip y guion.

#### Dependencias y bloqueadores
T5.1.2–T5.1.4

#### Definición de terminado
Paquete reproducible y ensayo sin bloqueadores críticos. Incluye implementación/artefacto completo, pruebas aprobadas, revisión, documentación actualizada, ausencia de errores relevantes conocidos, evidencia adjunta e integración con la rama principal cuando exista repositorio.

#### Fuentes
Enunciado TP Final Concurrente 2026; Mapa maestro de contenidos; prompt de backlog; programa oficial de la asignatura

#### Metadatos sugeridos
Tipo: TASK · Prioridad: P1 · Tamaño: M · Estado inicial: Backlog · Estado de definición: BORRADOR · Labels: task, priority::p1, draft · Milestone: M5 — Entrega · Responsable sugerido: Responsable de integración


---


# Bloque 4: validación del backlog

## Requisitos sin cobertura
No se detectan requisitos oficiales sin una épica/historia asociada. Las condiciones administrativas de grupo, evaluación individual y carga en LEV se cubren en E5, pero no se convierten en tareas de código.

## Issues con información faltante
Las issues marcadas `BORRADOR` dependen del archivo PIPE, la definición de 200 invariantes, la cantidad de corridas, los tiempos elegidos, fairness, versión Java/build, repositorio y fecha efectiva. Los campos de archivos usan `POR DEFINIR` mientras no exista estructura de repositorio.

## Dependencias críticas
1. T1.1.1 → todo el análisis formal.
2. T1.1.2 → hilos, verificadores e interpretación de 200 invariantes.
3. T1.2.1/T1.2.2 → trabajadores.
4. T2.1.3/T2.2.1 → políticas, tiempo y logging.
5. T4.2.1 → campaña experimental final.

## Riesgos del camino crítico
- No disponer del archivo PIPE oficial.
- Interpretar mal invariantes o cantidad de hilos.
- Diseñar una espera temporal que bloquee la sección crítica.
- No resolver la finalización de hilos bloqueados.
- Obtener resultados no reproducibles o logs no parseables.
- Fecha oficial vencida sin una fecha efectiva confirmada.

## Trabajo que puede hacerse en paralelo
- Una vez validada la red: T1.2.1 y el diseño inicial de T2.1.1.
- Tras definir el modelo Java: políticas, logger y pruebas unitarias pueden avanzar en paralelo.
- UML e informe pueden evolucionar incrementalmente, pero deben cerrarse después del código.

## Posibles divisiones adicionales
- T2.1.3 puede separarse en contrato de API y sincronización si supera tamaño L.
- T3.2.2 puede separarse en reloj temporal e integración con Monitor si aparecen dos semánticas alternativas.
- T5.1.4 puede dividirse por secciones del informe entre roles, manteniendo una revisión editorial única.

## Mejoras opcionales separadas del alcance obligatorio
- CI en GitHub Actions.
- Logs estructurados JSON.
- Cargador de redes desde archivo configurable.
- Métricas de CPU/memoria y profiling.
- Visualización automática de resultados.
Estas mejoras son `P3/OPCIONAL` y no deben desplazar requisitos obligatorios.

## Próxima issue recomendada
**T1.1.1 — `[SPIKE] Recuperar y validar la red oficial en PIPE`.** Es la única issue inicial recomendada porque reduce la mayor incertidumbre y desbloquea invariantes, propiedades, cantidad de hilos, verificadores, criterio de 200 ciclos y pruebas de referencia.

## Control de duplicados
No fue posible verificar issues existentes porque no se proporcionó ni conectó un repositorio GitHub. Antes de crear issues reales, se debe comparar esta lista con el issue tracker y fusionar o descartar duplicados.
