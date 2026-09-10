Podríamos hacer:

`public synchronized boolean fireTransition(...)`

y para esta versión mínima funcionaría.

Pero sabemos que la tarea siguiente, **T2.2.1**, necesita colas/condiciones de espera y reactivación. 
`ReentrantLock` nos permite evolucionar naturalmente hacia:

`Condition condition = lock.newCondition();`

sin cambiar completamente la estrategia de sincronización.

El material de monitores que tienen en el proyecto justamente distingue exclusión mutua y condiciones de sincronización: primero se protege el recurso compartido y luego se incorporan mecanismos para suspender/reactivar procesos.