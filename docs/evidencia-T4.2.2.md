# Evidencia experimental — T4.2.2

**Tarea:** T4.2.2 — Ejecutar campañas por política/timing  
**Proyecto:** TP Final — Programación Concurrente 2026  
**Estado:** Campaña formal definitiva completada

---

## 1. Objetivo

La tarea T4.2.2 tiene como objetivo ejecutar la campaña experimental definida previamente en el protocolo, utilizando las políticas:

```text
RANDOM
PRIORITY
```

y las configuraciones temporales:

```text
BASELINE
VARIANT
```

Cada corrida debe completar:

```text
200 T-invariantes
```

y conservar evidencia verificable de:

```text
IT1
IT2
IT3
duración
P-invariantes
T-invariantes
terminación de Workers
log de ejecución
```

---

## 2. Configuración definitiva

La campaña formal definitiva utiliza:

```text
TARGET = 200
```

Seeds:

```text
2026
2027
2028
2029
2030
```

Políticas:

```text
RANDOM
PRIORITY
```

Configuraciones temporales:

```text
BASELINE
VARIANT
```

La configuración temporal VARIANT fue recalibrada después de corregir el arbitraje del Monitor.

El valor definitivo utilizado fue:

```text
VARIANT_SCALE = 1.05
```

La matriz experimental queda:

```text
2 políticas
x
2 configuraciones temporales
x
5 seeds
=
20 corridas
```

---

## 3. Validación previa

Antes de ejecutar la campaña formal definitiva se ejecutó la suite completa del proyecto.

Resultado:

```text
Tests run: 171
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

Esto permitió iniciar la campaña con el arbitraje del Monitor ya estabilizado.

---

## 4. Resultado global de la campaña

La ejecución produjo:

```text
Recorded runs: 20
SUCCESS: 20
INVALID_RESULT: 0
ERROR: 0
```

Resultado global:

```text
FORMAL CAMPAIGN STATUS: SUCCESS
```

Duración total aproximada de la campaña:

```text
699.728 s
```

equivalente a aproximadamente:

```text
11 min 40 s
```

Este valor corresponde a la campaña completa y no al tiempo de una única corrida.

---

## 5. Resultados individuales

| Run | IT1 | IT2 | IT3 | Duración |
| --- | ---: | ---: | ---: | ---: |
| RANDOM_BASELINE_2026 | 79 | 62 | 59 | 32.359 s |
| RANDOM_BASELINE_2027 | 70 | 67 | 63 | 32.773 s |
| RANDOM_BASELINE_2028 | 76 | 62 | 62 | 31.885 s |
| RANDOM_BASELINE_2029 | 74 | 71 | 55 | 32.131 s |
| RANDOM_BASELINE_2030 | 79 | 61 | 60 | 31.562 s |
| RANDOM_VARIANT_2026 | 83 | 57 | 60 | 32.741 s |
| RANDOM_VARIANT_2027 | 69 | 70 | 61 | 34.329 s |
| RANDOM_VARIANT_2028 | 77 | 60 | 63 | 33.388 s |
| RANDOM_VARIANT_2029 | 74 | 71 | 55 | 33.825 s |
| RANDOM_VARIANT_2030 | 79 | 60 | 61 | 33.314 s |
| PRIORITY_BASELINE_2026 | 54 | 100 | 46 | 36.096 s |
| PRIORITY_BASELINE_2027 | 42 | 100 | 58 | 36.322 s |
| PRIORITY_BASELINE_2028 | 46 | 100 | 54 | 36.276 s |
| PRIORITY_BASELINE_2029 | 55 | 100 | 45 | 36.088 s |
| PRIORITY_BASELINE_2030 | 50 | 99 | 51 | 36.140 s |
| PRIORITY_VARIANT_2026 | 54 | 100 | 46 | 37.926 s |
| PRIORITY_VARIANT_2027 | 42 | 100 | 58 | 38.187 s |
| PRIORITY_VARIANT_2028 | 46 | 100 | 54 | 38.083 s |
| PRIORITY_VARIANT_2029 | 55 | 100 | 45 | 37.928 s |
| PRIORITY_VARIANT_2030 | 50 | 100 | 50 | 38.038 s |

En todas las corridas:

```text
IT1 + IT2 + IT3 = 200
```

---

## 6. Validación temporal

El rango requerido para cada ejecución es:

```text
20 s <= duración <= 40 s
```

Resultado observado:

```text
Duración mínima: 31.562 s
Duración máxima: 38.187 s
Duración promedio: 34.970 s
```

Cantidad de corridas dentro del rango:

```text
20
```

Cantidad de corridas fuera del rango:

```text
0
```

Por lo tanto, todas las ejecuciones utilizadas en la campaña formal cumplen el requisito temporal.

---

## 7. Resumen por política

Agrupando las diez corridas de cada política se obtiene:

| Política | IT1 promedio | IT2 promedio | IT3 promedio |
| --- | ---: | ---: | ---: |
| RANDOM | 76.0 | 64.1 | 59.9 |
| PRIORITY | 49.4 | 99.9 | 50.7 |

En RANDOM participan los tres caminos.

En PRIORITY se observa un aumento claro de la cantidad de invariantes correspondientes al camino de alto riesgo:

```text
IT2
```

Este resultado confirma que el cambio de política produce un efecto observable en la distribución.

La interpretación detallada de esta diferencia corresponde a:

```text
T4.2.3
```

---

## 8. Resumen por política y timing

Los tiempos promedio obtenidos fueron:

| Política | Timing | Duración promedio |
| --- | --- | ---: |
| RANDOM | BASELINE | 32.142 s |
| RANDOM | VARIANT | 33.519 s |
| PRIORITY | BASELINE | 36.184 s |
| PRIORITY | VARIANT | 38.032 s |

La configuración VARIANT incrementa la duración respecto de BASELINE sin superar el límite de 40 segundos.

La interpretación detallada del efecto temporal se realizará en T4.2.3.

---

## 9. Evidencia generada

Los logs de la campaña formal definitiva se encuentran en:

```text
results/logs/
```

La campaña contiene:

```text
20 logs
```

con la convención:

```text
<POLICY>_<TIMING>_<SEED>.log
```

El resultado consolidado se encuentra en:

```text
results/summary/formal-campaign.csv
```

La salida de consola de la campaña se conserva en:

```text
results/summary/formal-campaign-console.txt
```

---

## 10. Verificaciones formales

Cada corrida fue ejecutada utilizando el mecanismo integrado de observabilidad y verificación.

Se verificaron:

```text
P-invariantes
T-invariantes
cantidad de admissions
cantidad de completions
cantidad total de invariantes
terminación de Workers
```

Las 20 corridas finalizaron con estado:

```text
SUCCESS
```

y ninguna fue registrada como:

```text
INVALID_RESULT
ERROR
```

---

## 11. Campaña diagnóstica previa

Antes de la corrección del arbitraje del Monitor se había ejecutado una primera campaña de 20 corridas.

Aunque esa campaña finalizaba formalmente, la distribución de invariantes mostró un comportamiento incorrecto respecto de las políticas.

En particular, PRIORITY produjo:

```text
IT2 = 0
```

de manera sistemática.

Esa evidencia permitió detectar el problema de arbitraje y desarrollar las pruebas de regresión correspondientes.

La campaña anterior fue archivada en:

```text
results/diagnostic-pre-fix/
```

y se conserva únicamente como:

```text
evidencia diagnóstica pre-corrección
```

No debe utilizarse como fuente de resultados para el análisis final.

---

## 12. Campaña formal definitiva

La campaña válida para las conclusiones posteriores es la ejecutada después de:

```text
corrección del arbitraje del Monitor
+
grant lógico por ConflictGroup
+
recalibración temporal
+
VARIANT_SCALE = 1.05
```

Resultado:

```text
20 / 20 corridas SUCCESS

20 / 20 corridas dentro de 20-40 s

0 INVALID_RESULT

0 ERROR
```

---

## 13. Conclusión de T4.2.2

La campaña experimental formal definida para T4.2.2 fue ejecutada completamente.

Se obtuvieron:

```text
20 corridas
4000 T-invariantes completos en total
20 logs formales
1 CSV consolidado
```

Todas las ejecuciones cumplieron:

```text
200 invariantes por corrida
P-invariantes válidos
T-invariantes válidos
Workers terminados
duración entre 20 y 40 segundos
```

Además, se observó una diferencia clara entre las distribuciones obtenidas por RANDOM y PRIORITY.

La tarea T4.2.2 queda por lo tanto respaldada por evidencia experimental reproducible.

La interpretación comparativa de distribución, invariantes y tiempos continúa en:

```text
T4.2.3 — Interpretar distribución, invariantes y tiempos
```
