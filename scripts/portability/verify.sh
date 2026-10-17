#!/usr/bin/env bash
# T5.1.2 — validacion Linux de build y ejecucion desde un clon limpio.
set -euo pipefail
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$script_dir/../.."
for tool in java javac mvn git; do
  command -v "$tool" >/dev/null || { echo "Falta $tool en PATH" >&2; exit 1; }
done
stamp="$(date +%Y%m%d_%H%M%S)"
report="results/portability/linux/$stamp"
mkdir -p "$report"
java -version > "$report/java-version.txt" 2>&1
javac -version > "$report/javac-version.txt" 2>&1
mvn -version > "$report/maven-version.txt" 2>&1
uname -a > "$report/system.txt"
git rev-parse HEAD > "$report/commit.txt"
echo '[1/4] Maven: compilacion limpia y tests'
mvn clean test 2>&1 | tee "$report/maven-test.txt"
echo '[2/4] Maven: package'
mvn '-DskipTests' package 2>&1 | tee "$report/maven-package.txt"
echo '[3/4] Main: --help'
java -cp target/classes app.Main --help > "$report/main-help.txt"
grep -q -- '--policy=RANDOM' "$report/main-help.txt"
echo '[4/4] Main: RANDOM y PRIORITY, 12 transacciones'
for policy in RANDOM PRIORITY; do
  output="$report/main-$policy.txt"
  logfile="$report/transactions-$policy.log"
  java -cp target/classes app.Main "--policy=$policy" --seed=2026 --target=12 \
    --scale=0.01 --timeout-seconds=20 "--log=$logfile" | tee "$output"
  for required in 'Admisiones T0: 12' 'Finalizaciones T9: 12' \
    'Total T-invariantes: 12' 'P-invariantes validos: true' \
    'T-invariantes validos: true' 'Workers terminados: true' \
    'Corrida exitosa: true'; do
    grep -Fxq "$required" "$output" || { echo "Falta $required ($policy)" >&2; exit 1; }
  done
  test -s "$logfile" || { echo "Log vacio ($policy)" >&2; exit 1; }
done
printf 'SUCCESS\n' > "$report/status.txt"
echo "VALIDACION EXITOSA: $report"
