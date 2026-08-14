import json
import tempfile
import unittest
from pathlib import Path

from scripts.tune_eval import load_rows


class TuneEvalInputTest(unittest.TestCase):
    def write_rows(self, rows):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        path = Path(directory.name) / "positions.jsonl"
        path.write_text("".join(json.dumps(row) + "\n" for row in rows), encoding="utf-8")
        return path

    def test_loads_enriched_rows(self):
        row = {
            "features": {"lifeDifference": 3, "handSizeDifference": -1},
            "gameId": "1-0-42",
            "setCode": "BLB",
            "agent": "production",
            "result": 1,
        }
        self.assertEqual(load_rows([self.write_rows([row])]), [row])

    def test_rejects_old_rows_without_provenance(self):
        path = self.write_rows([{"features": {"lifeDifference": 3}, "gameId": "old", "result": 1}])
        with self.assertRaisesRegex(ValueError, "recollect this pre-provenance dataset"):
            load_rows([path])

    def test_rejects_mixed_feature_schemas(self):
        common = {"gameId": "game", "setCode": "BLB", "agent": "v0", "result": 1}
        path = self.write_rows([
            {**common, "features": {"lifeDifference": 3}},
            {**common, "features": {"lifeDifference": 2, "turnNumber": 4}},
        ])
        with self.assertRaisesRegex(ValueError, "feature schema differs"):
            load_rows([path])


if __name__ == "__main__":
    unittest.main()
