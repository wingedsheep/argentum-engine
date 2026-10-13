"""When the launchers start a session versus wait on CI (scripts/loop-lib.sh).

Run: python3 -m unittest scripts.test_loop_lib
"""
from pathlib import Path
import subprocess
import tempfile
import unittest

LIB = Path(__file__).with_name("loop-lib.sh")

# A gh stub: `pr list` prints the open PR numbers from $OPEN, `pr checks <n>` prints the bucket from
# $CHECKS_<n> (empty = not registered yet). Every --watch is recorded, so a test can tell a wait.
GH = r'''#!/usr/bin/env bash
if [ "$1 $2" = "pr list" ]; then
  for n in $OPEN; do echo "$n"; done; exit 0
fi
if [ "$1 $2" = "pr checks" ]; then
  [[ " $* " == *" --watch "* ]] && { echo "$3" >> "$ROOT/watched"; exit 0; }
  var="CHECKS_$3"; [ -n "${!var}" ] && echo "${!var}"; exit 0
fi
'''


class AwaitTurnTest(unittest.TestCase):
    def run_turn(self, open_prs, checks, max_open=2, last=""):
        with tempfile.TemporaryDirectory() as root:
            bin_dir = Path(root) / "bin"
            bin_dir.mkdir()
            (bin_dir / "gh").write_text(GH)
            (bin_dir / "gh").chmod(0o755)
            (bin_dir / "sleep").write_text("#!/usr/bin/env bash\n")
            (bin_dir / "sleep").chmod(0o755)
            env = {"PATH": f"{bin_dir}:/usr/bin:/bin", "ROOT": root, "OPEN": " ".join(open_prs)}
            env.update({f"CHECKS_{n}": v for n, v in checks.items()})
            script = (f'log() {{ echo "LOG $*"; }}; source "{LIB}"; '
                      f'loop_await_turn loop-ecl- {max_open} "{last}"; echo "PRS=$LOOP_PRS"')
            out = subprocess.run(["bash", "-c", script], env=env, capture_output=True, text=True,
                                 timeout=10, check=True).stdout
            watched = Path(root, "watched").read_text().split() if Path(root, "watched").exists() else []
            return out, watched

    def test_no_open_pr_starts_at_once(self):
        out, watched = self.run_turn([], {})
        self.assertIn("PRS=\n", out)
        self.assertEqual(watched, [])

    def test_pending_pr_under_the_cap_builds_meanwhile(self):
        out, watched = self.run_turn(["7"], {"7": "pending"})
        self.assertIn("building the next unit meanwhile", out)
        self.assertEqual(watched, [])

    def test_pending_pr_with_cap_one_waits(self):
        _, watched = self.run_turn(["7"], {"7": "pending"}, max_open=1)
        self.assertEqual(watched, ["7"])

    def test_at_the_cap_waits_on_the_oldest(self):
        out, watched = self.run_turn(["9", "7"], {"7": "pending", "9": "pending"})
        self.assertIn("PRS=7 9", out)
        self.assertEqual(watched, ["7"])

    def test_after_waiting_ci_answer_waits_instead_of_another_session(self):
        _, watched = self.run_turn(["7"], {"7": "pending"}, last="WAITING_CI")
        self.assertEqual(watched, ["7"])

    def test_a_finished_pr_needs_a_session_even_at_the_cap(self):
        _, watched = self.run_turn(["7", "9"], {"7": "pass", "9": "pending"})
        self.assertEqual(watched, [])

    def test_unregistered_checks_count_as_pending(self):
        out, watched = self.run_turn(["7"], {})
        self.assertIn("building the next unit meanwhile", out)
        self.assertEqual(watched, [])


class PipelineProtocolTest(unittest.TestCase):
    def protocol(self, max_open):
        return subprocess.run(["bash", "-c", f'source "{LIB}"; loop_pipeline_protocol {max_open}'],
                              capture_output=True, text=True, check=True).stdout

    def test_only_present_when_more_than_one_pr_may_be_open(self):
        self.assertEqual(self.protocol(1), "")
        self.assertIn("up to 2 of our PRs", self.protocol(2))


if __name__ == "__main__":
    unittest.main()
