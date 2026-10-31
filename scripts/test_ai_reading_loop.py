"""Launcher integration checks without model sessions, GitHub calls, or real sleeps.

Run: python3 -m unittest scripts.test_ai_reading_loop
"""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


class AiReadingLoopTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.bin = self.root / "bin"
        self.bin.mkdir()
        (self.root / ".git").mkdir()
        shutil.copy(Path(__file__).with_name("ai-reading-loop"), self.bin / "ai-reading-loop")
        skill = self.root / ".agents" / "skills" / "ai-reading-loop"
        skill.mkdir(parents=True)
        shutil.copy(Path(__file__).parent.parent / ".agents" / "skills" / "ai-reading-loop" / "SKILL.md", skill)
        shutil.copy(Path(__file__).with_name("loop-lib.sh"), self.bin / "loop-lib.sh")
        self.stub("git", 'echo "$TEST_ROOT/.git"')
        self.stub("gh", "exit 0")
        # The session reaper ticks every LOOP_REAP_TICK seconds; those aren't the loop's waits.
        self.stub("sleep", '[ "$1" = "$LOOP_REAP_TICK" ] && exit 0; '
                           'echo "$1" >> "$TEST_ROOT/sleeps"; touch "$TEST_ROOT/.claude/loop-runs/ai-reading.stop"')
        self.stub("claude", """
printf '%s\\n' "$@" > "$TEST_ROOT/args"
echo "{\\"type\\":\\"result\\",\\"result\\":\\"$VERDICT\\",\\"num_turns\\":1,\\"duration_ms\\":1000}"
""")

    def stub(self, name, body):
        path = self.bin / name
        path.write_text("#!/usr/bin/env bash\n" + body + "\n")
        path.chmod(0o755)

    def run_loop(self, *args, verdict="AI_READING_COMPLETE"):
        env = dict(os.environ, PATH=f"{self.bin}:{os.environ['PATH']}",
                   TEST_ROOT=str(self.root), HOME=str(self.root), LOOP_REAP_TICK="1", MODEL="", VERDICT=verdict)
        return subprocess.run(["bash", str(self.bin / "ai-reading-loop"), *args],
                              cwd=self.root, env=env, capture_output=True, text=True, timeout=10)

    def prompt(self):
        args = (self.root / "args").read_text()
        return args

    def test_help_does_not_launch(self):
        for option in ("--help", "-h", "--models"):
            with self.subTest(option=option):
                result = self.run_loop(option)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertIn("astra", result.stdout)
                self.assertFalse((self.root / "args").exists())

    def test_default_launch_completes(self):
        result = self.run_loop()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("harness claude, model claude-opus-5-5", result.stdout)
        self.assertIn("ai-reading loop complete", result.stdout)
        prompt = self.prompt()
        self.assertIn("[agent-loop: claude-opus-5-5] AI reading:", prompt)
        self.assertIn('startswith("loop-ai-reading-")', prompt)
        self.assertIn("just ai-reading-baseline", prompt)
        self.assertIn("AUDIT FALSE POSITIVE", prompt)
        self.assertIn("PR LIFECYCLE", prompt)
        self.assertIn(str(self.root / ".claude" / "loop-runs" / "ai-reading.md"), prompt)
        self.assertIn("Focus: none", prompt)
        for placeholder in ("<MODEL_ID>", "<FOCUS_LINE>", "<RUNS>", "<REPO>", "step-prompt:", "```"):
            self.assertNotIn(placeholder, prompt)

    def test_focus_reaches_the_prompt(self):
        result = self.run_loop("", "polarity: A & B")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("focus: polarity: A & B", result.stdout)
        self.assertIn("Focus: 'polarity: A & B'", self.prompt())

    def test_stuck_stops(self):
        result = self.run_loop("opus", verdict="STUCK reader regression")
        self.assertEqual(result.returncode, 1, result.stdout)
        self.assertIn("ai-reading.md", result.stdout)

    def test_step_done_continues_until_stop_file(self):
        (self.root / ".claude" / "loop-runs").mkdir(parents=True)
        self.stub("claude", """
printf '%s\\n' "$@" > "$TEST_ROOT/args"
touch "$TEST_ROOT/.claude/loop-runs/ai-reading.stop"
echo '{"type":"result","result":"STEP 5 DONE","num_turns":1,"duration_ms":1000}'
""")
        result = self.run_loop()
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn("stop file found", result.stdout)
        self.assertFalse((self.root / ".claude" / "loop-runs" / "ai-reading.stop").exists())


if __name__ == "__main__":
    unittest.main()
