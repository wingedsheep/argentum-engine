#!/usr/bin/env python3
"""Fit a deterministic, dependency-free pairwise linear ECL apprentice.

Input is a JSON array of examples with `preferred`, `other`, and optional `weight`; both feature
objects must use the committed RawBoardFeatures names. Output is the finite JVM artifact contract.
The untouched test split must never be passed to this command.
"""
import argparse
import json
import math
import random
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("examples", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--model-id", default="ecl-apprentice")
    parser.add_argument("--epochs", type=int, default=200)
    parser.add_argument("--learning-rate", type=float, default=0.02)
    parser.add_argument("--l2", type=float, default=0.01)
    parser.add_argument("--seed", type=int, default=20260801)
    args = parser.parse_args()
    rows = json.loads(args.examples.read_text())
    if not rows:
        raise SystemExit("training set is empty")
    names = sorted(rows[0]["preferred"])
    if any(sorted(r["preferred"]) != names or sorted(r["other"]) != names for r in rows):
        raise SystemExit("feature schema differs between examples")
    weights = [0.0] * len(names)
    rng = random.Random(args.seed)
    order = list(range(len(rows)))
    for _ in range(args.epochs):
        rng.shuffle(order)
        for index in order:
            row = rows[index]
            delta = [float(row["preferred"][n]) - float(row["other"][n]) for n in names]
            margin = max(-40.0, min(40.0, sum(w * x for w, x in zip(weights, delta))))
            error = 1.0 / (1.0 + math.exp(margin))
            importance = float(row.get("weight", 1.0))
            weights = [w + args.learning_rate * (importance * error * x - args.l2 * w)
                       for w, x in zip(weights, delta)]
    if not all(math.isfinite(w) for w in weights):
        raise SystemExit("fit produced non-finite coefficients")
    artifact = {"schemaVersion": 1, "modelId": args.model_id, "setCode": "ECL",
                "featureNames": names, "sharedCoefficients": weights,
                "setOverlayCoefficients": [], "intercept": 0.0}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    staged = args.output.with_suffix(args.output.suffix + ".tmp")
    staged.write_text(json.dumps(artifact, indent=2, sort_keys=True) + "\n")
    staged.replace(args.output)


if __name__ == "__main__":
    main()
