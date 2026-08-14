#!/usr/bin/env python3
"""Fit Phase 9 raw board-evaluation weights from arena JSONL.

Requires numpy, scikit-learn, and matplotlib. Splits are always grouped by game; an optional
held-out set is never used for model selection. Drawn games are excluded because sklearn's binary
logistic regression does not accept the collector's half-win label.
"""

from __future__ import annotations

import argparse
import json
import math
import sys
from collections import Counter
from pathlib import Path


REQUIRED_ROW_FIELDS = {"features", "gameId", "setCode", "agent", "result"}


def load_rows(paths: list[Path]) -> list[dict]:
    rows: list[dict] = []
    for path in paths:
        with path.open(encoding="utf-8") as source:
            for line_number, line in enumerate(source, 1):
                if not line.strip():
                    continue
                try:
                    row = json.loads(line)
                except json.JSONDecodeError as error:
                    raise ValueError(f"{path}:{line_number}: invalid JSON: {error.msg}") from error
                missing = REQUIRED_ROW_FIELDS - row.keys()
                if missing:
                    names = ", ".join(sorted(missing))
                    raise ValueError(
                        f"{path}:{line_number}: missing {names}; recollect this pre-provenance dataset"
                    )
                if row["result"] not in (-1, 0, 1):
                    raise ValueError(f"{path}:{line_number}: result must be -1, 0, or 1")
                if not isinstance(row["features"], dict) or not row["features"]:
                    raise ValueError(f"{path}:{line_number}: features must be a non-empty object")
                rows.append(row)
    if not rows:
        raise ValueError("no rows found")
    feature_names = tuple(sorted(rows[0]["features"]))
    for index, row in enumerate(rows, 1):
        if tuple(sorted(row["features"])) != feature_names:
            raise ValueError(f"row {index}: feature schema differs from the first row")
        if not all(isinstance(row["features"][name], (int, float)) for name in feature_names):
            raise ValueError(f"row {index}: every feature must be numeric")
    return rows


def fit(rows: list[dict], holdout_set: str | None, profile_id: str, output_dir: Path) -> dict:
    try:
        import numpy as np
        from sklearn.calibration import calibration_curve
        from sklearn.linear_model import LogisticRegression
        from sklearn.metrics import accuracy_score, brier_score_loss, log_loss
        from sklearn.model_selection import GroupKFold, GridSearchCV
        from sklearn.pipeline import Pipeline
        from sklearn.preprocessing import StandardScaler
    except ImportError as error:
        raise RuntimeError(
            "tuning dependencies are missing; install scripts/requirements-ai-tuning.txt"
        ) from error

    decisive = [row for row in rows if row["result"] != 0]
    if not decisive:
        raise ValueError("dataset contains no decisive games")
    training = [row for row in decisive if holdout_set is None or row["setCode"] != holdout_set]
    held_out = [row for row in decisive if holdout_set is not None and row["setCode"] == holdout_set]
    if holdout_set and not held_out:
        raise ValueError(f"held-out set {holdout_set!r} has no decisive rows")
    if len({row["gameId"] for row in training}) < 3:
        raise ValueError("training data needs at least three decisive games")
    if len({row["result"] for row in training}) < 2:
        raise ValueError("training data needs wins and losses")

    feature_names = sorted(training[0]["features"])
    matrix = lambda sample: np.asarray(
        [[row["features"][name] for name in feature_names] for row in sample], dtype=float
    )
    labels = lambda sample: np.asarray([1 if row["result"] == 1 else 0 for row in sample])
    x_train, y_train = matrix(training), labels(training)
    groups = np.asarray([row["gameId"] for row in training])
    folds = min(5, len(set(groups)))
    pipeline = Pipeline([
        ("scale", StandardScaler()),
        ("model", LogisticRegression(max_iter=5_000, solver="lbfgs")),
    ])
    search = GridSearchCV(
        pipeline,
        {"model__C": [0.01, 0.03, 0.1, 0.3, 1.0, 3.0, 10.0]},
        scoring="neg_log_loss",
        cv=GroupKFold(n_splits=folds),
        n_jobs=-1,
    )
    search.fit(x_train, y_train, groups=groups)
    model = search.best_estimator_
    scaler, logistic = model.named_steps["scale"], model.named_steps["model"]
    raw_weights = logistic.coef_[0] / scaler.scale_
    intercept = float(logistic.intercept_[0] - np.dot(raw_weights, scaler.mean_))

    def metrics(sample: list[dict]) -> dict | None:
        if not sample:
            return None
        truth = labels(sample)
        probability = model.predict_proba(matrix(sample))[:, 1]
        return {
            "rows": len(sample),
            "games": len({row["gameId"] for row in sample}),
            "logLoss": float(log_loss(truth, probability, labels=[0, 1])),
            "brier": float(brier_score_loss(truth, probability)),
            "accuracy": float(accuracy_score(truth, probability >= 0.5)),
        }

    model_artifact = {
        "intercept": intercept,
        "weights": {name: float(weight) for name, weight in zip(feature_names, raw_weights)},
        # The fitted score is already a logit, so one evaluator point is one logit.
        "winProbabilityScale": 1.0,
    }
    report = {
        "profileId": profile_id,
        "schema": "raw-board-features-v1",
        "model": model_artifact,
        "selectedC": float(search.best_params_["model__C"]),
        "training": metrics(training),
        "heldOutSet": holdout_set,
        "heldOut": metrics(held_out),
        "agents": dict(sorted(Counter(row["agent"] for row in decisive).items())),
        "sets": dict(sorted(Counter(row["setCode"] for row in decisive).items())),
        "drawRowsExcluded": len(rows) - len(decisive),
    }
    if not all(math.isfinite(value) for value in model_artifact["weights"].values()):
        raise ValueError("fit produced a non-finite coefficient")

    output_dir.mkdir(parents=True, exist_ok=True)
    (output_dir / "raw-eval-weights.json").write_text(
        json.dumps({profile_id: model_artifact}, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    (output_dir / "fit-report.json").write_text(
        json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )

    # Calibration is diagnostic only; arena win rate remains the promotion gate.
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    plot_rows = held_out or training
    truth, probability = labels(plot_rows), model.predict_proba(matrix(plot_rows))[:, 1]
    observed, predicted = calibration_curve(truth, probability, n_bins=10, strategy="quantile")
    figure, axis = plt.subplots(figsize=(6, 6))
    axis.plot([0, 1], [0, 1], "--", color="0.6", label="ideal")
    axis.plot(predicted, observed, marker="o", label=holdout_set or "training")
    axis.set(xlabel="Predicted win probability", ylabel="Observed win rate", xlim=(0, 1), ylim=(0, 1))
    axis.legend()
    figure.tight_layout()
    figure.savefig(output_dir / "calibration.png", dpi=160)
    plt.close(figure)
    return report


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("inputs", nargs="+", type=Path, help="arena feature JSONL files")
    parser.add_argument("--holdout-set", help="set code reserved entirely for final validation")
    parser.add_argument("--profile-id", default="texel-candidate", help="stable candidate profile id")
    parser.add_argument("--output-dir", type=Path, default=Path("benchmarks/eval-tuning"))
    parser.add_argument("--validate-only", action="store_true", help="validate and summarize without fitting")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    try:
        rows = load_rows(args.inputs)
        if args.validate_only:
            print(json.dumps({
                "rows": len(rows),
                "games": len({row["gameId"] for row in rows}),
                "sets": dict(sorted(Counter(row["setCode"] for row in rows).items())),
                "agents": dict(sorted(Counter(row["agent"] for row in rows).items())),
            }, indent=2))
            return 0
        artifact = fit(rows, args.holdout_set, args.profile_id, args.output_dir)
        print(json.dumps(artifact, indent=2, sort_keys=True))
        return 0
    except (OSError, ValueError, RuntimeError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
