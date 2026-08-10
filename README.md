# Procesamiento-transacciones-pago

El sistema debe ejecutar en Java una red de Petri que representa un procesador simplificado de transacciones de pago. Las transacciones recorren uno de tres flujos y compiten por los recursos compartidos P7 y P8. El núcleo debe implementarse mediante un monitor concurrente genérico, políticas intercambiables, hilos derivados de los invariantes y una semántica temporal para T2, T3, T5, T7 y T8.

Además de funcionar, el proyecto debe demostrar:

preservación del marcado y de los invariantes;
ausencia de problemas relevantes de sincronización;
finalización ordenada;
comportamiento de ambas políticas;
duración total entre 20 y 40 segundos;
cumplimiento de 200 invariantes por ejecución;
reproducibilidad mediante logs, pruebas y documentación.
