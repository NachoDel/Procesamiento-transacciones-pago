# Formato del log de ejecución

## Objetivo y alcance

Este documento describe el contrato **implementado** por `logging.ExecutionLogger` y consumido al final de cada corrida por `verification.TInvariantAnalyzer`.

El archivo registra **disparos de transiciones confirmados**, no solicitudes pendientes ni transacciones completas. Los disparos de distintos Workers pueden aparecer intercalados, porque la simulación es concurrente.

## Formato

Cada disparo confirmado genera una línea UTF-8 con siete campos separados por `|`, sin encabezado:

```text
SEQUENCE|THREAD|TRANSITION|TIMESTAMP|POLICY|SEED|CONFIG
```

Ejemplo de una línea válida:

```text
1|H0|T0|1792072420247|PRIORITY|2026|BASELINE
```

| Campo | Tipo / ejemplo | Significado |
|---|---|---|
| `SEQUENCE` | Entero positivo (`1`) | Orden global de escritura del disparo; comienza en 1 y crece de uno en uno. |
| `THREAD` | `H0` a `H4` | Nombre del Worker que efectuó el disparo. |
| `TRANSITION` | `T0` a `T9` | Transición que se disparó exitosamente. |
| `TIMESTAMP` | `1792072420247` | Instante de registro en milisegundos desde Unix epoch (`System.currentTimeMillis()`). |
| `POLICY` | `RANDOM` o `PRIORITY` | Política utilizada para la corrida. |
| `SEED` | `2026` | Semilla configurada para la política pseudoaleatoria o el fallback de prioridad. |
| `CONFIG` | `BASELINE`, `VARIANT`, etc. | Identificador de la configuración temporal. |

No deben aparecer caracteres separadores `|` ni saltos de línea dentro de los campos de texto. Un mismo timestamp puede repetirse: su resolución es de milisegundos y el **orden lógico** lo determina `SEQUENCE`.

## Relación con los Workers

La configuración oficial asigna las siguientes transiciones:

| Worker | Transiciones que dispara |
|---|---|
| H0 | T0 |
| H1 | T1, T2, T3 |
| H2 | T4, T5 |
| H3 | T6, T7, T8 |
| H4 | T9 |

Las rutas completas del modelo son:

```text
IT1: T0 -> T1 -> T2 -> T3 -> T9
IT2: T0 -> T4 -> T5 -> T9
IT3: T0 -> T6 -> T7 -> T8 -> T9
```

Como varios Workers operan en paralelo, **no corresponde buscar esas rutas como subcadenas contiguas en el log global**.

## Concurrencia y orden del registro

`ExecutionLogger` es thread-safe: protege el contador de secuencia, la escritura y el cierre con un `ReentrantLock`. Cada evento se escribe y se hace `flush` bajo ese lock. En la corrida productiva, el registro se llama desde `ExecutionAudit` como parte del `PostFireObserver`, luego del disparo confirmado y mientras el Monitor conserva su exclusión mutua.

Al iniciarse una corrida, el logger crea sus directorios padre si no existen y crea o trunca el archivo indicado. Por lo tanto, **una segunda ejecución con la misma ruta reemplaza el log anterior**.

## Verificación posterior de T-invariantes

Una vez detenidos todos los Workers y cerrado el logger, `TInvariantAnalyzer`:

1. Lee las líneas y valida que cada evento tenga siete campos con tipos y formatos compatibles.
2. Comprueba que `SEQUENCE` sea consecutivo a partir de `1`.
3. Separa las transiciones por Worker lógico y valida sus secuencias mediante expresiones regulares.
4. Reconoce los ciclos completos de H1 (`T1 T2 T3`), H2 (`T4 T5`) y H3 (`T6 T7 T8`), y cuenta IT1, IT2 e IT3.
5. Comprueba que las cantidades de `T0` y `T9` coincidan con la suma de los T-invariantes reconocidos.
6. Informa incidencias si hay secuencias incompletas, inválidas o conteos inconsistentes.

No hay un identificador individual de transacción en el formato: la verificación reconstruye **conteos y secuencias de los Workers**, no la identidad extremo a extremo de cada transacción.

Para una corrida válida con objetivo 200 se espera:

```text
count(T0) = 200
count(T9) = 200
IT1 + IT2 + IT3 = 200
```

La verificación de **P-invariantes** es distinta: se realiza online, después de cada disparo, mediante `ExecutionAudit` y `PlaceInvariantChecker`. No hay que confundirla con el análisis post-run del log.

## Ubicación y análisis

El programa principal permite elegir el archivo mediante `--log=ruta.log`. Por defecto lo almacena en `results/logs/` con un nombre que identifica la política, la configuración temporal, la seed y el objetivo.

Comando de ejemplo (después de compilar):

```bash
java -cp target/classes app.Main --policy=PRIORITY --target=200
```

Consultar además:

- [`runner-experimental.md`](runner-experimental.md): ciclo de vida completo de una corrida.
- [`protocolo-experimentos.md`](protocolo-experimentos.md): condiciones de validez y campañas.
- [`evidencia-T4.2.2.md`](evidencia-T4.2.2.md): resultados de las campañas realizadas.
