# Shared by the headless loop launchers (set-loop, commander-loop, assay-loop). Sourced, not run.
#
# A loop PR is reviewed *before* it is opened, so it needs exactly one CI run, and the session
# that merges it goes straight on to build the next unit — no session is spent on review alone or
# on merging alone.

# The prompt block every launcher appends: how a PR moves from build to merge.
LOOP_PR_PROTOCOL="PR LIFECYCLE — every PR is reviewed BEFORE it is opened, so it needs exactly one CI run:
- Gate with \`just loop-gate\` from the worktree, plus whatever unit-specific gates the step names (rebless,
  printing checks, assay gates). Never run \`just build\` or \`just test\`: loop-gate runs the tests your diff can
  break and CI runs the full suite on the PR — running the full suite twice is the waste this rule exists to stop.
- Then review the branch (\`origin/main...HEAD\`) with the review-changes skill in SPLIT MODE: you wrote the code, so
  you only dispatch — one TRIAGE subagent, one subagent per area it names, one WEIGH subagent — each a fresh context,
  with the review dir \`<worktree>/.claude/loop-runs/review-<branch>/\`. Tell each that \`just loop-gate\` already
  passed on this head (CI runs the full suite), so nobody re-runs tests. WEIGH returns the verdict and writes the
  findings to the review dir instead of posting them. Fix the findings it calls blocking or important; you may decline
  one only with a stated reason. Re-run \`just loop-gate\` if anything changed. Without subagents (e.g. Codex), run
  the skill's sequential mode and say in the PR body that the review was not fresh-context.
- Then push, open the PR (its body lists the review's fixes and declines), and post a PR comment whose first line
  is exactly \`loop-reviewed <full sha of the pushed HEAD>\`. Record the unit as in review, with its PR number, in the
  ledger. Then stop: never wait on or poll CI — the launcher waits for it between sessions.
- Any later push to an open PR (a CI fix, a rebase) ends with a new \`loop-reviewed <new head sha>\` comment saying
  what changed since the review.
- Each open PR of ours:
  a. CI failed -> read only the failing log tail (\`gh run view <id> --log-failed | tail -200\`), fix it, re-run
     \`just loop-gate\`, push, post the new marker. A known-flaky test: \`gh run rerun --failed\` instead.
  b. Merge conflicts -> rebase on origin/main (re-bless snapshot goldens / re-bake generated files instead of
     hand-merging), \`just loop-gate\`, push, post the new marker.
  c. No loop-reviewed marker naming its current head (an earlier session died before finishing) -> review it in
     split mode as above, fix, gate, push, post the marker.
  d. CI green, merges cleanly, and the newest loop-reviewed comment names its head -> \`gh pr merge --squash
     --delete-branch\` (from the main checkout, after \`git worktree remove\` of its worktree), then do the merge
     bookkeeping below. Then DON'T stop: \`git fetch\` and carry on in this same session with the build step the
     updated state calls for, so the next PR is already open by the time you finish.
  e. CI still pending or running -> nothing to do for that PR. Unless PIPELINING below lets you build the next unit,
     answer WAITING_CI."

# Prompt rules for running more than one PR at a time. $1 is the cap on open PRs.
loop_pipeline_protocol() {
  [ "${1:-1}" -gt 1 ] || return 0
  cat <<EOF
PIPELINING — up to $1 of our PRs may be open at once, so the next unit is built while CI runs on the last one:
- First handle every open PR of ours that needs work (PR LIFECYCLE a-d), oldest first.
- Then, while fewer than $1 of our PRs are open, the build steps' "No PR open" condition counts as met: build the
  next unit from fresh origin/main.
- Never build on unmerged work. Skip a gap whose deps include a gap still in review, cards only an in-review gap
  unlocks, cards an open PR already adds, a second Assay sweep while one is open, and the finishing PR while any PR
  of ours is open. If nothing else is buildable, answer WAITING_CI.
- With $1 PRs open and none needing work, answer WAITING_CI.
- After the older PR merges, the newer one often conflicts on snapshot goldens or backlog checklists. That is
  routine lifecycle b: rebase and regenerate, never hand-merge.
EOF
}

# Our open PRs by branch prefix, oldest first.
loop_open_prs() {
  gh pr list --author @me --state open --json number,headRefName \
    --jq "[.[] | select(.headRefName | startswith(\"$1\")) | .number] | sort | .[]" 2>/dev/null
}

# True while PR $1's CI is still running, or hasn't registered yet.
loop_ci_pending() {
  local buckets
  buckets=$(gh pr checks "$1" --json bucket --jq '[.[].bucket] | join(" ")' 2>/dev/null)
  [ -z "$buckets" ] || [[ " $buckets " == *" pending "* ]]
}

# Waits out CI on PR $1 for free, so no session is spent watching it. Callers define `log`.
loop_wait_ci() {
  local pr=$1 tries=0
  log "waiting for CI on PR #$pr"
  # Checks take a few seconds to register after a push; poll for them instead of a fixed sleep.
  while [ -z "$(gh pr checks "$pr" --json bucket --jq '.[].bucket' 2>/dev/null)" ] && [ $tries -lt 18 ]; do
    tries=$((tries + 1)); sleep 10
  done
  gh pr checks "$pr" --watch >/dev/null 2>&1
  log "CI on PR #$pr: $(gh pr checks "$pr" --json bucket --jq '[.[].bucket] | group_by(.) | map("\(.[0])=\(length)") | join(" ")' 2>/dev/null)"
}

# Decides whether the next session can start now. Sets LOOP_PRS to our open PRs.
#   $1 branch prefix   $2 cap on open PRs   $3 the previous session's last line
# A session starts at once when no PR is open or one needs work (CI finished). When every open PR
# is still in CI, it starts anyway while fewer than the cap are open — it builds the next unit
# meanwhile — unless the last session already said there was nothing to build (WAITING_CI).
# Otherwise this waits out CI on the oldest.
loop_await_turn() {
  local prefix=$1 max=$2 last=${3:-} pr count=0
  LOOP_PRS=$(loop_open_prs "$prefix" | sort -n | xargs)
  [ -n "$LOOP_PRS" ] || return 0
  for pr in $LOOP_PRS; do
    count=$((count + 1))
    loop_ci_pending "$pr" || return 0
  done
  if [ "$count" -lt "$max" ] && [[ "$last" != WAITING_CI* ]]; then
    log "PR #${LOOP_PRS// /, #} in CI — building the next unit meanwhile"
    return 0
  fi
  loop_wait_ci "${LOOP_PRS%% *}"
}

# Re-runs the launcher under the live dashboard (scripts/loop-tui) when it was started from a
# terminal. $1 is the loop's name (its log stem); the rest is the launcher command to run under it.
# LOOP_TUI=0 keeps the plain streamed output.
loop_maybe_tui() {
  local name=$1; shift
  [ -t 0 ] && [ -t 1 ] && [ "${LOOP_TUI:-1}" != 0 ] && [ -z "${LOOP_UNDER_TUI:-}" ] || return 0
  exec python3 "$(dirname "${BASH_SOURCE[0]}")/loop-tui" --name "$name" -- "$@"
}

# The model argument that re-selects the same harness and model.
loop_model_arg() {
  if [ "$HARNESS" = codex ]; then echo "codex:$MODEL"; else echo "$MODEL"; fi
}
