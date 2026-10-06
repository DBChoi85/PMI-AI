#!/usr/bin/env bash
set -euo pipefail

CP="build/classes/java/main:build/resources/main:build/benchmark-libs/*"
MAIN="io.github.dbchoi85.pmiai.results.IsolatedBenchmarkMain"
OUT="results/raw/forks"
mkdir -p "$OUT"
rm -f "$OUT"/*.csv

run_forks() {
  local experiment="$1" forks="$2" iterations="$3" warmups="$4"
  for run in $(seq 1 "$forks"); do
    echo "[$experiment] JVM fork $run/$forks (warmup=$warmups, measured=$iterations)"
    java -cp "$CP" "$MAIN" "$experiment" "$run" "$iterations" "$warmups"
  done
}

# E1/E3/E4/E5 use independent JVM forks. E2 deliberately retains the previous
# practical scale because the 10,000-agent population cell dominates runtime.
run_forks E1 10 100 100
run_forks E2 10 10 5
run_forks E3 10 100 50
run_forks E4 10 100 100
run_forks E5 10 100 100

python3 scripts/merge_publication_csv.py "$OUT" results/raw/publication.csv

GIT_COMMIT="$(git rev-parse HEAD)" java -cp "$CP" \
  io.github.dbchoi85.pmiai.results.ResultExportMain results/raw/publication.csv

echo "Publication benchmark complete."
echo "E3 uses all 1,000 EPG observations/depth for statistics; AIP remains 100 observations/depth."
echo "Depth 0 represents established base authority / zero EPG delegation overhead and is not a direct AIP authority-token comparison."
