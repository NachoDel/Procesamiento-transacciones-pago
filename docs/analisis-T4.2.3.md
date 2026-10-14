# Análisis de resultados — T4.2.3

**Tarea:** T4.2.3 — Interpretar distribución, invariantes y tiempos  
**Proyecto:** TP Final — Programación Concurrente 2026  
**Fuente principal:** campaña formal definitiva de T4.2.2  
**Estado:** Análisis experimental

---

## 1. Objetivo

El objetivo de esta tarea es interpretar los resultados producidos por la campaña formal ejecutada en T4.2.2.

El análisis se concentra en:

- distribución de los T-invariantes `IT1`, `IT2` e `IT3`;
- comparación entre las políticas `RANDOM` y `PRIORITY`;
- comparación entre las configuraciones temporales `BASELINE` y `VARIANT`;
- tiempos mínimos, máximos, promedio y dispersión;
- relación entre el modelo temporal analítico y los tiempos medidos;
- efecto de los recursos compartidos `P7` y `P8`;
- cumplimiento de P-invariantes y T-invariantes;
- conclusiones reutilizables en el informe final.

Los datos analizados corresponden exclusivamente a la campaña formal definitiva almacenada en:

```text
results/summary/formal-campaign.csv
```

La campaña diagnóstica anterior a la corrección del arbitraje del `Monitor` no se utiliza para las conclusiones finales.

---

## 2. Configuración experimental

La campaña formal combina:

```text
2 políticas
x
2 configuraciones temporales
x
5 seeds
=
20 corridas
```

Las políticas utilizadas fueron:

```text
RANDOM
PRIORITY
```

Las configuraciones temporales fueron:

```text
BASELINE
VARIANT
```

con:

```text
VARIANT_SCALE = 1.05
```

Las seeds fueron:

```text
2026
2027
2028
2029
2030
```

Cada corrida procesa exactamente:

```text
200 T-invariantes completos
```

---

## 3. T-invariantes analizados

Los tres caminos completos de la Red de Petri son:

### IT1 — Pago con tarjeta

```text
T0 -> T1 -> T2 -> T3 -> T9
```

### IT2 — Pago de alto riesgo

```text
T0 -> T4 -> T5 -> T9
```

### IT3 — Transferencia bancaria

```text
T0 -> T6 -> T7 -> T8 -> T9
```

En cada corrida válida debe cumplirse:

```text
IT1 + IT2 + IT3 = 200
```

---

## 4. Resumen de resultados por escenario

Los promedios obtenidos en las cinco corridas de cada combinación fueron:

| Política | Timing | IT1 prom. | IT1 % | IT2 prom. | IT2 % | IT3 prom. | IT3 % | Tiempo prom. |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| RANDOM | BASELINE | 75.60 | 37.80 % | 64.60 | 32.30 % | 59.80 | 29.90 % | 32.142 s |
| RANDOM | VARIANT | 76.40 | 38.20 % | 63.60 | 31.80 % | 60.00 | 30.00 % | 33.519 s |
| PRIORITY | BASELINE | 49.40 | 24.70 % | 99.80 | 49.90 % | 50.80 | 25.40 % | 36.184 s |
| PRIORITY | VARIANT | 49.40 | 24.70 % | 100.00 | 50.00 % | 50.60 | 25.30 % | 38.032 s |

La suma promedio continúa siendo:

```text
IT1 + IT2 + IT3 = 200
```

en todos los escenarios.

---

# 5. Análisis de RANDOM

Agrupando las diez corridas de RANDOM se obtiene:

```text
IT1 = 76.00  -> 38.00 %
IT2 = 64.10  -> 32.05 %
IT3 = 59.90  -> 29.95 %
```

La distribución presenta participación de los tres caminos.

No se espera una distribución final exactamente igual a:

```text
33.33 % / 33.33 % / 33.33 %
```

porque `RandomPolicy` no impone cuotas globales.

La política selecciona pseudoaleatoriamente entre las solicitudes candidatas de cada conflicto particular.

Por lo tanto, la distribución final también depende de:

```text
- estado de la Red de Petri;
- disponibilidad de P7 y P8;
- solicitudes pendientes;
- tiempos de las transiciones temporales;
- scheduling de los threads.
```

El resultado:

```text
38.00 % / 32.05 % / 29.95 %
```

es consistente con una política donde ningún camino es excluido y las tres alternativas participan de forma significativa.

---

## 6. Estabilidad de RANDOM frente al cambio temporal

Los promedios de distribución fueron:

### BASELINE

```text
IT1 = 37.80 %
IT2 = 32.30 %
IT3 = 29.90 %
```

### VARIANT

```text
IT1 = 38.20 %
IT2 = 31.80 %
IT3 = 30.00 %
```

El cambio temporal no modifica sustancialmente la distribución.

Las diferencias entre BASELINE y VARIANT son pequeñas:

```text
IT1: +0.40 puntos porcentuales
IT2: -0.50 puntos porcentuales
IT3: +0.10 puntos porcentuales
```

Por lo tanto, dentro de esta campaña, aumentar los delays un 5 % afecta principalmente la duración y no produce un cambio relevante en la distribución de los caminos bajo RANDOM.

---

# 7. Análisis de PRIORITY

Agrupando las diez corridas de PRIORITY se obtiene:

```text
IT1 = 49.40  -> 24.70 %
IT2 = 99.90  -> 49.95 %
IT3 = 50.70  -> 25.35 %
```

El cambio respecto de RANDOM es claro.

Comparación global:

| Política | IT1 % | IT2 % | IT3 % |
| --- | ---: | ---: | ---: |
| RANDOM | 38.00 % | 32.05 % | 29.95 % |
| PRIORITY | 24.70 % | 49.95 % | 25.35 % |

El camino priorizado `IT2` pasa de:

```text
32.05 %
```

a aproximadamente:

```text
49.95 %
```

La diferencia es:

```text
+17.90 puntos porcentuales
```

respecto de RANDOM.

En términos relativos:

```text
49.95 / 32.05 ≈ 1.56
```

por lo que la participación de `IT2` es aproximadamente un 56 % mayor bajo PRIORITY que bajo RANDOM.

---

## 8. Interpretación del efecto de PRIORITY

La transición prioritaria es:

```text
T4
```

que inicia:

```text
IT2 = T0 -> T4 -> T5 -> T9
```

La política PRIORITY selecciona `T4` cuando su solicitud participa del conflicto.

Con el mecanismo de grant lógico del `Monitor`, la solicitud seleccionada puede conservar el turno mientras espera los recursos necesarios para quedar formalmente sensibilizada.

Esto evita que otras alternativas del mismo conflicto se adelanten continuamente.

Los resultados muestran que esa decisión tiene un efecto observable:

```text
RANDOM
IT2 ≈ 32 %

PRIORITY
IT2 ≈ 50 %
```

Por lo tanto, la política priorizada está influyendo realmente sobre la distribución de caminos.

---

## 9. PRIORITY no implica IT2 = 100 %

El hecho de priorizar `T4` no significa que todas las operaciones deban seguir `IT2`.

La prioridad actúa solamente cuando la solicitud prioritaria participa del conflicto.

Las otras alternativas pueden continuar cuando:

```text
- T4 no participa como solicitud candidata;
- no existe una solicitud prioritaria pendiente;
- el conflicto se resuelve mediante el fallback correspondiente.
```

Por esta razón, una distribución cercana a:

```text
25 % / 50 % / 25 %
```

es compatible con una prioridad estricta sin convertir el sistema en un único camino exclusivo.

---

# 10. Estabilidad de PRIORITY

Los resultados de IT2 fueron especialmente estables.

### PRIORITY + BASELINE

```text
100
100
100
100
99
```

Promedio:

```text
99.8
```

### PRIORITY + VARIANT

```text
100
100
100
100
100
```

Promedio:

```text
100.0
```

Esto muestra que el efecto de PRIORITY se mantiene para las cinco seeds y para ambas configuraciones temporales.

El comportamiento no depende de una única corrida particular.

---

# 11. Análisis temporal experimental

Los tiempos obtenidos por escenario fueron:

| Política | Timing | Promedio | Mínimo | Máximo | Desvío estándar |
| --- | --- | ---: | ---: | ---: | ---: |
| RANDOM | BASELINE | 32.142 s | 31.562 s | 32.773 s | 0.412 s |
| RANDOM | VARIANT | 33.519 s | 32.741 s | 34.329 s | 0.532 s |
| PRIORITY | BASELINE | 36.184 s | 36.088 s | 36.322 s | 0.096 s |
| PRIORITY | VARIANT | 38.032 s | 37.926 s | 38.187 s | 0.099 s |

Todas las corridas permanecieron dentro del rango:

```text
20 s <= duración <= 40 s
```

El rango total observado fue:

```text
mínimo = 31.562 s
máximo = 38.187 s
```

---

# 12. Efecto de VARIANT

La configuración `VARIANT` utiliza:

```text
VARIANT_SCALE = 1.05
```

por lo que los delays temporales aumentan aproximadamente un 5 % respecto de BASELINE.

## RANDOM

```text
BASELINE = 32.142 s
VARIANT  = 33.519 s
```

Incremento absoluto:

```text
1.377 s
```

Incremento relativo:

```text
4.29 %
```

## PRIORITY

```text
BASELINE = 36.184 s
VARIANT  = 38.032 s
```

Incremento absoluto:

```text
1.848 s
```

Incremento relativo:

```text
5.11 %
```

Los incrementos prácticos son del mismo orden que el escalado temporal aplicado.

No se espera una relación perfectamente lineal debido a:

```text
concurrencia
+
solapamiento de caminos
+
contención
+
scheduling
+
sincronización del Monitor
```

Sin embargo, los resultados muestran una respuesta temporal coherente con el incremento del 5 %.

---

# 13. Dispersión temporal

RANDOM presentó:

```text
desvío BASELINE = 0.412 s
desvío VARIANT  = 0.532 s
```

PRIORITY presentó:

```text
desvío BASELINE = 0.096 s
desvío VARIANT  = 0.099 s
```

Las corridas PRIORITY son temporalmente más estables.

Esto coincide con la distribución observada.

Bajo PRIORITY:

```text
IT2 ≈ 100
```

en prácticamente todas las corridas.

Por lo tanto, la composición de trabajo cambia muy poco entre seeds.

En RANDOM existe mayor variación en la proporción de los tres caminos, y cada camino utiliza recursos y tiempos diferentes.

Esa variación en la mezcla de operaciones produce también una mayor variación en el tiempo total.

---

# 14. Modelo temporal de los caminos

La configuración BASELINE define:

| Transición | Delay |
| --- | ---: |
| T2 | 120 ms |
| T3 | 80 ms |
| T5 | 150 ms |
| T7 | 100 ms |
| T8 | 120 ms |

Por lo tanto, considerando únicamente los delays temporales de cada camino:

### IT1

```text
T2 + T3
=
120 ms + 80 ms
=
200 ms
```

### IT2

```text
T5
=
150 ms
```

### IT3

```text
T7 + T8
=
100 ms + 120 ms
=
220 ms
```

En VARIANT los valores utilizados son:

| Transición | Delay VARIANT |
| --- | ---: |
| T2 | 126 ms |
| T3 | 84 ms |
| T5 | 158 ms |
| T7 | 105 ms |
| T8 | 126 ms |

Por lo tanto:

```text
IT1 temporal = 210 ms
IT2 temporal = 158 ms
IT3 temporal = 231 ms
```

---

# 15. Recursos compartidos P7 y P8

Los caminos utilizan los recursos compartidos de manera diferente.

Conceptualmente:

```text
IT1 utiliza P7

IT2 utiliza P7 + P8

IT3 utiliza P8
```

Esto significa que IT1 e IT3 pueden presentar paralelismo entre sí cuando:

```text
IT1 ocupa P7
```

mientras simultáneamente:

```text
IT3 ocupa P8
```

En cambio, IT2 necesita ambos recursos al mismo tiempo.

Por lo tanto:

```text
mayor proporción de IT2
        ↓
mayor ocupación simultánea de P7 y P8
        ↓
menor oportunidad de solapamiento entre IT1 e IT3
        ↓
mayor contención efectiva
```

Este comportamiento ayuda a explicar por qué PRIORITY produce tiempos superiores a RANDOM.

---

# 16. Modelo analítico de carga sobre P7 y P8

Sea:

```text
C = cantidad de IT1
H = cantidad de IT2
B = cantidad de IT3
```

En BASELINE, una estimación idealizada de carga temporal sobre los recursos es:

```text
Carga P7 = 200*C + 150*H  [ms]

Carga P8 = 150*H + 220*B  [ms]
```

Como P7 y P8 pueden trabajar parcialmente en paralelo, un límite inferior idealizado para la ejecución es:

```text
T_ideal >= max(Carga P7, Carga P8)
```

Este valor no pretende predecir exactamente la duración real.

Representa una cota analítica basada únicamente en la ocupación temporal mínima de los recursos compartidos.

---

# 17. Estimación analítica — BASELINE

## RANDOM + BASELINE

Promedios:

```text
C = 75.6
H = 64.6
B = 59.8
```

Carga de P7:

```text
200*75.6 + 150*64.6
=
15120 + 9690
=
24810 ms
=
24.810 s
```

Carga de P8:

```text
150*64.6 + 220*59.8
=
9690 + 13156
=
22846 ms
=
22.846 s
```

Límite inferior idealizado:

```text
max(24.810, 22.846)
=
24.810 s
```

Tiempo práctico:

```text
32.142 s
```

Diferencia:

```text
32.142 - 24.810
=
7.332 s
```

---

## PRIORITY + BASELINE

Promedios:

```text
C = 49.4
H = 99.8
B = 50.8
```

Carga de P7:

```text
200*49.4 + 150*99.8
=
9880 + 14970
=
24850 ms
=
24.850 s
```

Carga de P8:

```text
150*99.8 + 220*50.8
=
14970 + 11176
=
26146 ms
=
26.146 s
```

Límite inferior idealizado:

```text
max(24.850, 26.146)
=
26.146 s
```

Tiempo práctico:

```text
36.184 s
```

Diferencia:

```text
36.184 - 26.146
=
10.038 s
```

---

# 18. Estimación analítica — VARIANT

En VARIANT:

```text
IT1 = 210 ms
IT2 = 158 ms
IT3 = 231 ms
```

Por lo tanto:

```text
Carga P7 = 210*C + 158*H

Carga P8 = 158*H + 231*B
```

---

## RANDOM + VARIANT

Promedios:

```text
C = 76.4
H = 63.6
B = 60.0
```

Carga de P7:

```text
210*76.4 + 158*63.6
=
16044 + 10048.8
=
26092.8 ms
=
26.093 s
```

Carga de P8:

```text
158*63.6 + 231*60
=
10048.8 + 13860
=
23908.8 ms
=
23.909 s
```

Límite inferior idealizado:

```text
26.093 s
```

Tiempo práctico:

```text
33.519 s
```

Diferencia:

```text
7.426 s
```

---

## PRIORITY + VARIANT

Promedios:

```text
C = 49.4
H = 100.0
B = 50.6
```

Carga de P7:

```text
210*49.4 + 158*100
=
10374 + 15800
=
26174 ms
=
26.174 s
```

Carga de P8:

```text
158*100 + 231*50.6
=
15800 + 11688.6
=
27488.6 ms
=
27.489 s
```

Límite inferior idealizado:

```text
27.489 s
```

Tiempo práctico:

```text
38.032 s
```

Diferencia:

```text
10.543 s
```

---

# 19. Comparación analítica vs práctica

Resumen:

| Escenario | Cota ideal por recursos | Tiempo práctico | Diferencia |
| --- | ---: | ---: | ---: |
| RANDOM + BASELINE | 24.810 s | 32.142 s | 7.332 s |
| RANDOM + VARIANT | 26.093 s | 33.519 s | 7.426 s |
| PRIORITY + BASELINE | 26.146 s | 36.184 s | 10.038 s |
| PRIORITY + VARIANT | 27.489 s | 38.032 s | 10.543 s |

El tiempo práctico es superior a la cota idealizada en todos los casos.

Esto es esperable porque el cálculo analítico solamente considera la ocupación temporal mínima de P7/P8.

La ejecución real también contiene:

```text
- adquisición y liberación del lock del Monitor;
- esperas sobre Conditions;
- arbitraje de conflictos;
- grants lógicos;
- scheduling del sistema operativo;
- reactivación de Workers;
- secuencias causales de la Red de Petri;
- períodos en los que un recurso puede quedar momentáneamente libre;
- costo de logging y verificación;
- fase inicial y vaciado final de la ejecución.
```

Por lo tanto, la cota analítica debe interpretarse como un límite inferior idealizado y no como una predicción exacta del tiempo total.

---

# 20. Por qué PRIORITY presenta una diferencia mayor respecto de la cota ideal

En RANDOM, IT1 e IT3 pueden aprovechar P7 y P8 de forma independiente.

Esto permite mayor superposición de trabajo.

En PRIORITY, aproximadamente la mitad de las operaciones son IT2:

```text
IT2 ≈ 50 %
```

y cada IT2 requiere simultáneamente:

```text
P7 + P8
```

Además, el grant lógico mantiene la decisión de prioridad mientras la solicitud seleccionada espera recursos.

Esto introduce una secuencia de utilización más condicionada por el camino priorizado.

Por ello, la ejecución práctica presenta:

```text
menor paralelismo efectivo
+
mayor contención
+
mayores períodos de espera coordinada
```

y la diferencia entre la cota idealizada y el tiempo real es mayor que en RANDOM.

---

# 21. Cambio del recurso crítico

El modelo también permite observar qué recurso posee mayor carga ideal.

## RANDOM

En BASELINE:

```text
P7 = 24.810 s
P8 = 22.846 s
```

Por lo tanto:

```text
P7
```

es el recurso de mayor carga ideal.

## PRIORITY

En BASELINE:

```text
P7 = 24.850 s
P8 = 26.146 s
```

Por lo tanto:

```text
P8
```

pasa a ser el recurso de mayor carga ideal.

El aumento de IT2 modifica la utilización relativa de los recursos y desplaza el cuello de botella idealizado.

Esto constituye otra evidencia de que la política no solamente cambia la distribución de invariantes, sino también la utilización interna de la Red de Petri.

---

# 22. P-invariantes

Durante cada disparo exitoso se verifica el marcado mediante `PlaceInvariantChecker`.

Los invariantes utilizados son:

```text
IP1:
P2 + P3 + P4 + P7 = 1
```

```text
IP2:
P4 + P5 + P6 + P8 = 1
```

```text
IP3:
P0 + P1 + P2 + P3 + P4 + P5 + P6 + P9 = 3
```

Las 20 corridas formales registraron:

```text
pInvariantsValid = true
```

Por lo tanto, no se detectaron violaciones de los P-invariantes durante la campaña formal.

---

# 23. Interpretación de los P-invariantes

## IP1

```text
P2 + P3 + P4 + P7 = 1
```

representa la conservación asociada al recurso lógico compartido por los caminos que utilizan `P7`.

El token correspondiente se encuentra:

```text
- disponible en P7;
- o comprometido en una etapa asociada a P2/P3/P4.
```

La suma permanece constante.

---

## IP2

```text
P4 + P5 + P6 + P8 = 1
```

representa de forma análoga la conservación asociada al recurso compartido `P8`.

El recurso puede estar:

```text
- disponible en P8;
- o comprometido en P4/P5/P6.
```

La suma permanece constante.

---

## IP3

```text
P0 + P1 + P2 + P3 + P4 + P5 + P6 + P9 = 3
```

representa la conservación global de las tres unidades de capacidad que circulan por el sistema.

El valor constante:

```text
3
```

coincide con el marcado inicial definido para esa capacidad.

---

# 24. T-invariantes

El análisis posterior de cada log verifica los caminos completos:

```text
IT1 = {T0,T1,T2,T3,T9}

IT2 = {T0,T4,T5,T9}

IT3 = {T0,T6,T7,T8,T9}
```

En las 20 corridas se obtuvo:

```text
tInvariantsValid = true
```

y además:

```text
IT1 + IT2 + IT3 = 200
```

en cada ejecución.

También se verificó:

```text
Admissions = 200
Completions = 200
```

Por lo tanto, cada operación admitida durante la campaña formal termina siendo contabilizada dentro de uno de los tres T-invariantes completos.

---

# 25. Terminación

Las 20 corridas registraron:

```text
workersTerminated = true
```

y:

```text
successful = true
```

No se registraron:

```text
INVALID_RESULT
ERROR
```

durante la campaña formal definitiva.

Esto confirma que la estrategia de parada permitió:

```text
- cerrar admisiones al alcanzar el target;
- completar las operaciones ya iniciadas;
- detener los Workers;
- finalizar sin threads activos.
```

---

# 26. Cumplimiento temporal

El rango requerido fue:

```text
20 s <= duración <= 40 s
```

La campaña obtuvo:

```text
duración mínima = 31.562 s
duración máxima = 38.187 s
```

Resultado:

```text
20 / 20 corridas dentro del rango
0 / 20 corridas fuera del rango
```

Por lo tanto, la configuración temporal definitiva cumple el requisito en todos los escenarios analizados.

---

# 27. Conclusiones sobre RANDOM

Los resultados permiten concluir que:

```text
RANDOM
```

produce una distribución con participación de los tres caminos.

Promedio:

```text
IT1 = 38.00 %
IT2 = 32.05 %
IT3 = 29.95 %
```

La política no garantiza igualdad exacta, pero ninguna alternativa queda sistemáticamente excluida.

La distribución presenta mayor variabilidad que PRIORITY, lo cual también se refleja en una mayor dispersión temporal.

---

# 28. Conclusiones sobre PRIORITY

La política:

```text
PRIORITY
```

produce un aumento claro y estable del camino de alto riesgo.

Promedio:

```text
IT2 = 49.95 %
```

frente a:

```text
IT2 = 32.05 %
```

en RANDOM.

El resultado se mantiene para:

```text
5 seeds
x
2 configuraciones temporales
```

por lo que el efecto no depende de una única ejecución.

La prioridad configurada sobre `T4` es experimentalmente observable.

---

# 29. Conclusiones sobre BASELINE y VARIANT

El escalado:

```text
VARIANT_SCALE = 1.05
```

produce:

```text
RANDOM   -> +4.29 %
PRIORITY -> +5.11 %
```

en el tiempo promedio.

Esto muestra una correspondencia razonable entre el aumento de los delays temporales y la duración práctica de las corridas.

La distribución de invariantes cambia poco entre BASELINE y VARIANT dentro de una misma política.

Por lo tanto, el principal efecto de VARIANT es temporal y no distributivo.

---

# 30. Conclusiones sobre paralelismo

La comparación RANDOM vs PRIORITY muestra que la política también influye indirectamente en el paralelismo.

Con RANDOM:

```text
IT1 puede utilizar P7
al mismo tiempo que
IT3 utiliza P8
```

Con PRIORITY aumenta significativamente IT2:

```text
IT2 necesita P7 + P8 simultáneamente
```

Esto reduce la posibilidad de solapamiento entre operaciones y ayuda a explicar:

```text
RANDOM   -> 32.831 s promedio global
PRIORITY -> 37.108 s promedio global
```

Por lo tanto, la elección de política afecta no solamente qué caminos se ejecutan, sino también la utilización de recursos y el rendimiento global.

---

# 31. Resultado final de T4.2.3

La campaña formal permite demostrar simultáneamente:

```text
corrección formal
+
terminación
+
cumplimiento temporal
+
efecto observable de RANDOM
+
efecto observable de PRIORITY
+
efecto observable de VARIANT
```

Los datos obtenidos muestran que:

1. los P-invariantes se mantienen durante todas las ejecuciones;
2. cada corrida completa exactamente 200 T-invariantes;
3. RANDOM permite participación significativa de los tres caminos;
4. PRIORITY incrementa de manera clara y estable la participación de IT2;
5. PRIORITY reduce el paralelismo efectivo al favorecer un camino que necesita P7 y P8 simultáneamente;
6. VARIANT incrementa los tiempos en un orden consistente con el escalado del 5 %;
7. las 20 corridas permanecen dentro del rango de 20 a 40 segundos;
8. los resultados prácticos son compatibles con el modelo temporal analítico, considerando que éste representa una cota idealizada y no una predicción exacta.

Estos resultados pueden utilizarse como base para la sección de análisis experimental y conclusiones del informe final.
