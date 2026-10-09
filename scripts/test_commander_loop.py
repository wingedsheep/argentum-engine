"""Launcher integration checks without model sessions, GitHub calls, or real sleeps.

Run: python3 -m unittest scripts.test_commander_loop
"""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


class CommanderLoopTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.bin = self.root / "bin"
        self.bin.mkdir()
        (self.root / ".git").mkdir()
        shutil.copy(Path(__file__).with_name("commander-loop"), self.bin / "commander-loop")
        self.stub("git", 'echo "$TEST_ROOT/.git"')
        self.stub("gh", "exit 0")
        self.stub("sleep", 'echo "$1" >> "$TEST_ROOT/sleeps"; touch "$TEST_ROOT/.claude/loop-runs/commander.stop"')
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
printf '%s\\n' "$VERDICT" > "$output"
echo '{"type":"item.completed","item":{"type":"agent_message","text":"Summary: checked"}}'
""")
        self.stub("claude", """
printf '%s\\n' "$@" > "$TEST_ROOT/args"
echo "{\\"type\\":\\"result\\",\\"result\\":\\"$VERDICT\\",\\"num_turns\\":1,\\"duration_ms\\":1000}"
""")

    def stub(self, name, body):
        path = self.bin / name
        path.write_text("#!/usr/bin/env bash\n" + body + "\n")
        path.chmod(0o755)

    def run_loop(self, *args, model="", mode="", verdict="COMMANDER_COMPLETE"):
        env = dict(os.environ, PATH=f"{self.bin}:{os.environ['PATH']}",
                   TEST_ROOT=str(self.root), MODEL=model, MODE=mode, VERDICT=verdict)
        return subprocess.run(["bash", str(self.bin / "commander-loop"), *args],
                              cwd=self.root, env=env, capture_output=True, text=True, timeout=10)

    def prompt(self):
        return "\n".join((self.root / "args").read_text().splitlines())

    def test_help_and_models_do_not_launch_sessions(self):
        for option in ("--help", "-h", "--models"):
            with self.subTest(option=option):
                result = self.run_loop(option)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertIn("astra", result.stdout)
                self.assertFalse((self.root / "args").exists())
                self.assertFalse((self.root / ".claude").exists())

    def test_default_uses_claude_and_top_100(self):
        result = self.run_loop()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("top 100, harness claude, model claude-opus-5-5", result.stdout)
        self.assertIn("top 100 commander decks playable", result.stdout)
        self.assertIn("commander-staples --top 100", self.prompt())

    def test_empty_arguments_from_just_use_defaults(self):
        result = self.run_loop("", "")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("top 100, harness claude", result.stdout)

    def test_astra_and_custom_top(self):
        result = self.run_loop("astra", "50")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        args = (self.root / "args").read_text().splitlines()
        self.assertEqual(args[:3], ["exec", "--model", "gpt-6-astra"])
        prompt = self.prompt()
        self.assertIn("[agent-loop: gpt-6-astra]", prompt)
        self.assertIn("commander-staples --top 50", prompt)
        self.assertIn("startswith(\"loop-cmdr-\")", prompt)
        self.assertNotIn("$CODE", prompt)

    def test_claude_alias_resolves_driving_model(self):
        result = self.run_loop("sonnet")
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn("resolve the actual driving", self.prompt())

    def test_bad_top_does_not_launch(self):
        for top in ("0", "ten", "-5"):
            with self.subTest(top=top):
                result = self.run_loop("opus", top)
                self.assertEqual(result.returncode, 2)
                self.assertIn("positive number", result.stderr)
                self.assertFalse((self.root / "args").exists())

    def test_stuck_stops(self):
        result = self.run_loop("astra", verdict="STUCK G3 commander tax")
        self.assertEqual(result.returncode, 1, result.stdout)
        self.assertIn("commander-cards.md", result.stdout)

    def test_usage_limit_waits_then_honors_stop_file(self):
        result = self.run_loop("astra", mode="limit")
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertEqual((self.root / "sleeps").read_text(), "900\n")
        self.assertIn("stop file found", result.stdout)


if __name__ == "__main__":
    unittest.main()
