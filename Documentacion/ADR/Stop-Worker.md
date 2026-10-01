## stopRequested

Este flag se consulta entre ciclos completos y no entre cada transicion 

Por ejemplo si H1 empieza

`T1 → T2 → T3`

y durante T2 alguien solicita una finalizacion **normal**, H1 intenta terminar `T3` antes de dejar iniciar otro ciclo.

Esto evita situaciones como

`T1 -> T2 -> STOP`

Dejando una operacion logica a medio camino.

En cambio, una **interrupción** sí significa cierre inmediato.

