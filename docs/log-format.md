# Formato del log de ejecución

## Objetivo

Definir el formato estable y parseable utilizado para registrar los
disparos confirmados de la Red de Petri durante una corrida.

Este contrato es producido por `T4.1.1` y será utilizado posteriormente
por `T4.1.3` para analizar T-invariantes mediante expresiones regulares.

---

## Formato de una entrada

Cada disparo exitoso genera exactamente una línea con el siguiente formato:

```text
SEQUENCE|THREAD|TRANSITION|TIMESTAMP|POLICY|SEED|CONFIG
