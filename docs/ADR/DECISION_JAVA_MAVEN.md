# Decisión tecnológica: Java 17 + Maven

**Proyecto:** TP Final de Programación Concurrente 2026  
**Fecha:** 09/09/2026

## Decisión

El proyecto utilizará **Java 17 (LTS)** como versión base del lenguaje y **Apache Maven 3.9.x** como herramienta de construcción y gestión de dependencias.

> Esta versión de Java no es una exigencia del enunciado. El TP únicamente exige implementar la solución en Java y que el proyecto pueda ejecutarse independientemente del sistema operativo y del IDE. La elección de Java 17 + Maven es una decisión técnica del equipo.

## ¿Por qué Java 17?

Java 17 es una versión **LTS (Long-Term Support)**, madura y ampliamente soportada por IDEs, herramientas de build y distribuciones OpenJDK.

Para este TP ofrece todas las herramientas de concurrencia necesarias, entre ellas:

- `Thread` y `Runnable`;
- `synchronized`;
- `Lock`, `ReentrantLock` y `Condition`;
- colecciones concurrentes;
- primitivas de sincronización de `java.util.concurrent`.

El objetivo del proyecto es implementar y explicar explícitamente conceptos como monitor, exclusión mutua, bloqueo/reactivación, hilos y políticas. Por ese motivo no necesitamos características más nuevas del lenguaje para resolver el problema.

### ¿Por qué no usar directamente la versión más reciente?

A septiembre de 2026, **Java 26 es la versión general más reciente** y **Java 25 es la LTS más reciente**. Java 17, Java 21 y Java 25 son versiones LTS.

Elegimos Java 17 como baseline conservador porque:

1. **No necesitamos APIs nuevas para cumplir el TP.** El núcleo requerido se implementa completamente con APIs disponibles en Java 17.
2. **Reduce diferencias entre las máquinas del equipo.** Es más probable encontrar soporte ya instalado o fácilmente disponible en distintos entornos académicos y de desarrollo.
3. **Evita adoptar características nuevas que no aportan al objetivo pedagógico.** Por ejemplo, mecanismos modernos como virtual threads pueden ser útiles en otros proyectos, pero no son necesarios para demostrar el diseño explícito del monitor y la sincronización requerida en este trabajo.
4. **Da una base estable para IntelliJ, VS Code, Maven y JUnit.**
5. **Facilita la defensa del proyecto.** El código se apoya en mecanismos clásicos de concurrencia que coinciden directamente con los conceptos evaluados por la materia.

Esto no significa que Java 21 o Java 25 sean malas opciones. Si el equipo decide más adelante que todos los entornos disponen de una versión superior y existe una ventaja concreta para el proyecto, la versión puede revisarse. Por ahora se prioriza estabilidad y simplicidad.

**Recomendación de distribución:** usar una distribución OpenJDK LTS, por ejemplo Eclipse Temurin 17, para que todos trabajen con una implementación equivalente del JDK.

## ¿Por qué Maven?

Maven permite definir la construcción completa del proyecto en un único archivo `pom.xml`, evitando que el proyecto dependa de configuraciones particulares de IntelliJ o VS Code.

Sus principales ventajas para este trabajo son:

- estructura estándar de proyecto Java (`src/main/java` y `src/test/java`);
- gestión automática y versionada de dependencias, como JUnit;
- compilación y testing desde terminal;
- mismo comportamiento en IntelliJ, VS Code y otros IDEs;
- configuración de la versión de Java en el propio proyecto;
- comandos simples y reproducibles para todo el equipo.

Por ejemplo:

```bash
mvn clean test
```

permite compilar el proyecto y ejecutar los tests sin depender del IDE.

## Maven Wrapper

Para mejorar todavía más la portabilidad, el repositorio debería incluir **Maven Wrapper** (`mvnw`, `mvnw.cmd` y `.mvn/wrapper`). De esta forma los integrantes no necesitan instalar manualmente la misma versión de Maven.

Los comandos pasarían a ser:

```bash
./mvnw clean test       # Linux / macOS
mvnw.cmd clean test     # Windows
```

Así, la única dependencia externa relevante para desarrollar y ejecutar el proyecto es disponer de un JDK compatible.

## Resumen

La combinación **Java 17 + Maven** se elige porque proporciona una base estable, portable, suficientemente moderna y simple para implementar el núcleo concurrente del TP sin incorporar complejidad tecnológica que no aporta a los objetivos académicos del trabajo.
