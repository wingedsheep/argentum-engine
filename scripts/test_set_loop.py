"""Launcher integration checks without model sessions, GitHub calls, or real sleeps.

Run: python3 -m unittest scripts.test_set_loop
"""
import os
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


class SetLoopTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.bin = self.root / "bin"
        self.bin.mkdir()
        (self.root / ".git").mkdir()
        shutil.copy(Path(__file__).with_name("set-loop"), self.bin / "set-loop")
        self.stub("resolve-set", 'echo ecl')
        self.stub("git", 'echo "$TEST_ROOT/.git"')
        self.stub("gh", "exit 0")
        self.stub("sleep", 'echo "$1" >> "$TEST_ROOT/sleeps"; touch "$TEST_ROOT/.claude/loop-runs/ecl.stop"')
        self.stub("codex", """
printf '%s\\n' "$@" > "$TEST_ROOT/args"
while [ "$#" -gt 0 ]; do
  if [ "$1" = --output-last-message ]; then shift; output=$1; fi
  shift
done
if [ "$MODE" = limit ]; then
  echo '{"type":"error","message":"usage limit reached"}'
  exit 1
fi
if [ "$MODE" = failure ]; then echo 'Invalid model'; exit 1; fi
printf '%s\\n' "$VERDICT" > "$output"
echo '{"type":"item.completed","item":{"type":"agent_message","text":"Summary: checked"}}'
echo '{"type":"turn.completed","usage":{}}'
""")
        self.stub("claude", """
printf '%s\\n' "$@" > "$TEST_ROOT/args"
echo '{"type":"result","result":"SET_COMPLETE","num_turns":1,"duration_ms":1000}'
""")

    def stub(self, name, body):
        path = self.bin / name
        path.write_text("#!/usr/bin/env bash\n" + body + "\n")
        path.chmod(0o755)

    def run_loop(self, *args, model="", mode="", verdict="SET_COMPLETE", set_code="ecl"):
        env = dict(os.environ, PATH=f"{self.bin}:{os.environ['PATH']}",
                   TEST_ROOT=str(self.root), MODEL=model, MODE=mode, VERDICT=verdict)
        return subprocess.run(["bash", str(self.bin / "set-loop"), set_code, *args],
                              cwd=self.root, env=env, capture_output=True, text=True, timeout=10)

    def test_help_and_models_do_not_resolve_sets_or_launch_sessions(self):
        self.stub("resolve-set", "exit 99")
        for option in ("", "--help", "-h", "--models"):
            with self.subTest(option=option):
                result = self.run_loop(set_code=option)
                self.assertEqual(result.returncode, 0, result.stderr)
                for model in ("sonnet", "opus", "astra"):
                    self.assertIn(model, result.stdout)
                self.assertFalse((self.root / "args").exists())
                self.assertFalse((self.root / ".claude").exists())

    def test_claude_aliases_and_explicit_id(self):
        for model in ("sonnet", "opus", "claude-opus-5-5"):
            with self.subTest(model=model):
                result = self.run_loop(model)
                self.assertEqual(result.returncode, 0, result.stderr)
                args = (self.root / "args").read_text().splitlines()
                self.assertEqual(args[0], "-p")
                self.assertEqual(args[args.index("--model") + 1], model)
                if model in ("sonnet", "opus"):
                    self.assertIn("resolve the actual driving", "\n".join(args))

    def test_picker_without_terminal_does_not_launch(self):
        result = self.run_loop("pick")
        self.assertEqual(result.returncode, 2)
        self.assertIn("pick needs a terminal", result.stderr)
        self.assertFalse((self.root / "args").exists())

    def test_astra_alias_and_explicit_id(self):
        for model in ("astra", "gpt-6-astra"):
            with self.subTest(model=model):
                result = self.run_loop(model)
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                args = (self.root / "args").read_text().splitlines()
                self.assertEqual(args[:3], ["exec", "--model", "gpt-6-astra"])
                self.assertIn("--approve-for-me", args)
                self.assertIn("[agent-loop: gpt-6-astra]", "\n".join(args))
                self.assertIn("Summary: checked", result.stdout)
                self.assertIn("set ecl complete", result.stdout)

    def test_environment_model_and_positional_override(self):
        result = self.run_loop("", model="astra")
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn("harness codex", result.stdout)
        result = self.run_loop("claude-opus-5-5", model="astra")
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn("harness claude", result.stdout)

    def test_arbitrary_codex_model_ids(self):
        for model in ("gpt-6-sol", "o3", "custom-model"):
            with self.subTest(model=model):
                result = self.run_loop(f"codex:{model}")
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                args = (self.root / "args").read_text().splitlines()
                self.assertEqual(args[:3], ["exec", "--model", model])
                self.assertIn(f"[agent-loop: {model}]", "\n".join(args))

    def test_codex_model_from_environment(self):
        result = self.run_loop(model="codex:custom-model")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("harness codex, model custom-model", result.stdout)

    def test_empty_codex_model_does_not_launch(self):
        result = self.run_loop("codex:")
        self.assertEqual(result.returncode, 2)
        self.assertIn("requires a model ID", result.stderr)
        self.assertFalse((self.root / "args").exists())

    def test_default_still_uses_claude(self):
        result = self.run_loop()
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn("harness claude, model claude-opus-5-5", result.stdout)

    def test_stuck_and_launch_failure_stop(self):
        for options in ({"verdict": "STUCK missing capability"}, {"mode": "failure"}):
            with self.subTest(options=options):
                result = self.run_loop("astra", **options)
                self.assertEqual(result.returncode, 1, result.stdout)
                self.assertFalse((self.root / "sleeps").exists())

    def test_usage_limit_waits_then_honors_stop_file(self):
        result = self.run_loop("astra", mode="limit")
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertEqual((self.root / "sleeps").read_text(), "900\n")
        self.assertIn("stop file found", result.stdout)

    def test_claude_session_limit_waits_then_honors_stop_file(self):
        message = "You've hit your session limit · resets 1:20am (Europe/Amsterdam)"
        event = json.dumps({"type": "result", "result": message,
                            "num_turns": 1, "duration_ms": 3000})
        self.stub("claude", "cat <<'EOF'\n" + event + "\nEOF")
        result = self.run_loop("sonnet")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual((self.root / "sleeps").read_text(), "900\n")
        self.assertIn("limit reached — waiting 15 min", result.stdout)
        self.assertNotIn("iteration 2", result.stdout)
        self.assertIn("stop file found", result.stdout)


if __name__ == "__main__":
    unittest.main()
