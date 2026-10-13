"""Pure helpers of scripts/loop-tui. Run: python3 -m unittest scripts.test_loop_tui"""
import importlib.machinery
import importlib.util
from pathlib import Path
import unittest

_loader = importlib.machinery.SourceFileLoader("loop_tui", str(Path(__file__).with_name("loop-tui")))
_spec = importlib.util.spec_from_loader("loop_tui", _loader)
loop_tui = importlib.util.module_from_spec(_spec)
_loader.exec_module(loop_tui)


class LoopTuiTest(unittest.TestCase):
    def test_ci_state(self):
        self.assertEqual(loop_tui.ci_state([]), "waiting for checks")
        self.assertEqual(loop_tui.ci_state([{"conclusion": "SUCCESS"}, {"conclusion": "SKIPPED"}]), "green")
        self.assertEqual(loop_tui.ci_state([{"conclusion": "SUCCESS"}, {"conclusion": ""}]), "running 1/2")
        self.assertEqual(loop_tui.ci_state([{"conclusion": "FAILURE"}, {"conclusion": ""}]), "failed")

    def test_wrap_and_clip_respect_display_width(self):
        self.assertEqual(loop_tui.wrap_text("one two three four", 9), ["one two", "three", "four"])
        self.assertEqual(loop_tui.clip("abcdef", 4), "abc…")
        self.assertEqual(loop_tui.width("📋a"), 3)

    def test_watching_a_loop_without_a_log_fails_cleanly(self):
        self.assertEqual(loop_tui.main(["--name", "no-such-loop-zz"]), 1)


if __name__ == "__main__":
    unittest.main()
