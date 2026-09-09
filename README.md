# TP Final Programación Concurrente 2026

Implementación en Java de una Red de Petri para un sistema de procesamiento de transacciones de pago, ejecutada mediante un monitor de concurrencia.

## Requisitos de desarrollo

- JDK 17
- Maven 3.9+ recomendado
- IntelliJ IDEA o Visual Studio Code (el proyecto no depende de un IDE específico)

## Compilar y ejecutar tests

```bash
mvn clean test
```

## Ejecutar el bootstrap

```bash
mvn -q -DskipTests package
java -cp target/classes ar.edu.unc.concurrente.Main
```

## Convenciones de ramas

- `main`: versión estable.
- `develop`: integración común del equipo.
- ramas cortas por tarea, creadas desde `develop`, por ejemplo:
  - `feature/T2.1.1-petri-net`
  - `test/T2.1.2-petri-net`
  - `feature/T2.1.3-monitor`

Todo cambio funcional entra a `develop` mediante Pull Request. `main` recibe únicamente versiones integradas y estables.

## Estructura prevista

```text
src/main/java/ar/edu/unc/concurrente/
├── Main.java
├── petri/
├── monitor/
├── policy/
├── worker/
├── verification/
├── logging/
└── config/
```

Los paquetes se crearán cuando exista una tarea que realmente los necesite; no se agregan clases vacías solo para completar la estructura.
