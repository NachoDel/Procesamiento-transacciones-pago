# Configuración inicial del repositorio

## 1. Crear el bootstrap en `main`

Partiendo del repositorio vacío:

```bash
git switch main
git pull origin main
```

Copiar a la raíz del repositorio los archivos de este bootstrap y luego:

```bash
git add .
git commit -m "chore: bootstrap Maven project"
git push origin main
```

## 2. Crear `develop` desde el bootstrap estable

```bash
git switch -c develop
git push -u origin develop
```

## 3. Proteger ramas en GitHub

Recomendado:

- `main`: no push directo; merge solo por Pull Request.
- `develop`: no push directo salvo excepciones acordadas; merge por Pull Request.
- exigir que los tests pasen antes del merge cuando se configure CI.
- al menos 1 aprobación para PRs que cambien contratos compartidos.

## 4. Dejar de usar ramas permanentes por developer

Si `dev1`, `dev2`, `dev3`, `dev4` están vacías, pueden eliminarse después de confirmar que no contienen trabajo:

```bash
git push origin --delete dev1 dev2 dev3 dev4
```

Cada developer crea ramas cortas desde `develop`:

```bash
git switch develop
git pull origin develop
git switch -c feature/T2.1.1-petri-net
```

## 5. Convención sugerida de nombres

- `feature/<ID>-descripcion`
- `test/<ID>-descripcion`
- `docs/<ID>-descripcion`
- `fix/<descripcion>`
- `chore/<descripcion>`

Ejemplos:

```text
feature/T2.1.1-petri-net
test/T2.1.2-petri-net
feature/T2.1.3-monitor
feature/T3.1.1-random-policy
docs/T5.1.3-diagrams
```

## 6. Contratos compartidos

Los cambios en contratos como `MonitorInterface`, `Policy`, configuración y formato de log deberían revisarse por al menos otro developer antes del merge.
