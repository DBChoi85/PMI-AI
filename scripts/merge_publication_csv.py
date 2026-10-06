#!/usr/bin/env python3
import csv
import sys
from pathlib import Path

if len(sys.argv) != 3:
    raise SystemExit("usage: merge_publication_csv.py <fork-dir> <output.csv>")

src = Path(sys.argv[1])
dst = Path(sys.argv[2])
files = sorted(src.glob("*.csv"))
if not files:
    raise SystemExit(f"no benchmark CSV files found in {src}")

header = None
rows = []
for path in files:
    with path.open(newline="") as f:
        reader = csv.reader(f)
        current = next(reader)
        if header is None:
            header = current
        elif current != header:
            raise SystemExit(f"header mismatch: {path}")
        rows.extend(reader)

dst.parent.mkdir(parents=True, exist_ok=True)
with dst.open("w", newline="") as f:
    writer = csv.writer(f, lineterminator="\n")
    writer.writerow(header)
    writer.writerows(rows)

print(f"Merged {len(files)} fork files / {len(rows)} samples -> {dst}")
