# Shared by the headless loop launchers (set-loop, commander-loop, assay-loop, ai-reading-loop). Sourced, not run.
#
# A loop PR is reviewed *before* it is opened, so it needs exactly one CI run, and the session
# that merges it goes straight on to build the next unit — no session is spent on review alone or
# on merging alone.

# The prompt block every launcher appends: how a PR moves from build to merge.
LOOP_PR_PROTOCOL="PR LIFECYCLE — every PR is reviewed BEFORE it is opened, so it needs exactly one CI run:
- Gate with \`just loop-gate\` from the worktree, plus whatever unit-specific gates the step names (printing checks,
  assay gates). A unit that adds or changes cards gates with \`just loop-gate --rebless\`: the snapshot goldens are
  re-blessed in the same build, and you check from the printed diff that only your cards moved — never a separate
  \`just rebless-cards\` run. Use \`just test-class\` only to debug a failure, never to pre-check what the gate runs
  anyway. Never run \`just build\` or \`just test\`: loop-gate runs the tests your diff can
  break and CI runs the full suite on the PR — running the full suite twice is the waste this rule exists to stop.
- NO E2E — never write, edit or run Playwright E2E specs (\`e2e-scenarios/tests/\`, any \`just e2e*\` recipe): they take
  too long. A new or changed player-visible flow (including an engine/server change shown by existing UI) gets a
  MANUAL UI CHECK before review instead; card-only batches that reuse established UI skip it. Start this worktree's
  app with \`just ui-dev <unused-server-port> <unused-client-port>\` in the background, set the board up with the
  generate-scenario skill and POST it to \`http://localhost:<server-port>/api/dev/scenarios\`, then drive the flow in
  headless Chromium with a throwaway script in \`e2e-scenarios/.ui-check/\` (gitignored; import \`chromium\` from
  \`@playwright/test\`, put the response's player token in localStorage \`argentum-token\`, open the client URL). Take a
  screenshot at every prompt and state of the flow and look at each PNG with the image-viewing tool. Judge it as a
  player would: layout, labels, highlighting, nothing overlapping, clipped or stale. Fix what looks off and shoot
  again. Stop the servers when done, and never touch another loop's ports. Put the final screenshots in the PR body.
  If the app or images can't be run or viewed, say exactly that rather than claiming a UX pass.
- Then review the branch (\`origin/main...HEAD\`) with the review-changes skill in SPLIT MODE: you wrote the code, so
  you only dispatch — one TRIAGE subagent, then (unless it reviewed a single-area diff itself in a single pass) one
  subagent per area it names (at most three) and one WEIGH subagent — each a fresh context,
  with the review dir \`<worktree>/.claude/loop-runs/review-<branch>/\`. A diff of only card definitions, their tests, backlog
  and goldens (no SDK, engine, server or client change) is reviewed in a single pass by TRIAGE whatever its
  size — never split cards into several area reviewers. Tell each that \`just loop-gate\` already
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
     --delete-branch\` from the main checkout, after freeing the branch: \`git -C <wt> switch --detach\` in a long-lived
     worktree (keep it, and its build outputs), \`git worktree remove\` for a per-branch one. Then do the merge
     bookkeeping below. Then DON'T stop: \`git fetch\` and carry on in this same session with the build step the
     updated state calls for, so the next PR is already open by the time you finish.
  e. CI still pending or running -> nothing to do; answer WAITING_CI. Never start the next unit while a PR of
     ours is open."

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
  # --watch can return while checks are still pending (an API hiccup, or a new head's checks
  # registering mid-watch); keep watching until nothing is pending, or a session is spent on WAITING_CI.
  tries=0
  while loop_ci_pending "$pr" && [ $tries -lt 60 ]; do
    tries=$((tries + 1))
    gh pr checks "$pr" --watch >/dev/null 2>&1 || sleep 30
  done
  log "CI on PR #$pr: $(gh pr checks "$pr" --json bucket --jq '[.[].bucket] | group_by(.) | map("\(.[0])=\(length)") | join(" ")' 2>/dev/null)"
}

# Decides whether the next session can start now. Sets LOOP_PRS to our open PRs.
#   $1 branch prefix
# A session starts at once when no PR is open or one needs work (CI finished); otherwise this waits
# out CI on the oldest. One PR at a time: the next unit is never built while CI runs on the last.
loop_await_turn() {
  local prefix=$1 pr
  LOOP_PRS=$(loop_open_prs "$prefix" | sort -n | xargs)
  [ -n "$LOOP_PRS" ] || return 0
  for pr in $LOOP_PRS; do
    loop_ci_pending "$pr" || return 0
  done
  loop_wait_ci "${LOOP_PRS%% *}"
}

# How long a Claude session may sit silent after answering its protocol line before it's ended.
LOOP_LINGER_GRACE=${LOOP_LINGER_GRACE:-120}

# Runs a Claude session (the rest of the args) whose stream-json output the caller tees to $1; $2 is
# the loop's completion word (SET_COMPLETE, ...). `claude -p` doesn't exit while a background task it
# started is still running, so one poll loop that never ends — a watcher grepping a log for a line
# nothing writes — would hold the iteration, and with it the whole loop, forever. Once the
# transcript ends on a result whose last line is a protocol answer and nothing more has been written
# for LOOP_LINGER_GRACE seconds, the session and everything it spawned is stopped and "$1.reaped" is
# left for loop_note_reaped. Returns the session's exit status.
loop_run_reaped() {
  local transcript=$1 done_word=$2 pid reaper status
  shift 2
  "$@" &
  pid=$!
  # Off the pipe: a reaper still holding it open would keep the caller's tee from seeing EOF.
  _loop_reap_lingering "$pid" "$transcript" "$done_word" >/dev/null 2>&1 &
  reaper=$!
  # Quiet: a reaped session would otherwise put the shell's "Terminated" notice in the transcript.
  wait "$pid" 2>/dev/null; status=$?
  kill "$reaper" 2>/dev/null; wait "$reaper" 2>/dev/null
  return $status
}

# Logs, and clears, a reap loop_run_reaped made of the session transcribed to $1. Callers define `log`.
loop_note_reaped() {
  [ -e "$1.reaped" ] || return 0
  rm -f "$1.reaped"
  log "session sat idle ${LOOP_LINGER_GRACE}s after answering — stopped it and the background tasks it left running"
}

_loop_reap_lingering() {
  local pid=$1 transcript=$2 done_word=$3 tick=${LOOP_REAP_TICK:-10} size last="" idle=0
  while kill -0 "$pid" 2>/dev/null; do
    sleep "$tick"
    size=$(wc -c < "$transcript" 2>/dev/null) || size=0
    if [ "$size" != "$last" ]; then last=$size; idle=0; continue; fi
    idle=$((idle + tick))
    [ "$idle" -ge "$LOOP_LINGER_GRACE" ] && _loop_answered "$transcript" "$done_word" || continue
    touch "$transcript.reaped"
    _loop_kill_tree "$pid"
    return 0
  done
}

# True when the transcript's last event is a result and the last non-empty result (a /goal session
# closes on an empty one) ends on a protocol line. A session still mid-turn — a long silent build —
# has an assistant or tool event last, so it's never taken for idle.
_loop_answered() {
  local last
  [ "$(jq -Rr 'fromjson? | .type' "$1" 2>/dev/null | tail -1)" = result ] || return 1
  last=$(jq -Rc 'fromjson? | select(.type == "result" and (.result // "") != "") | .result' "$1" 2>/dev/null |
    tail -1 | jq -r '.' 2>/dev/null | sed '/^[[:space:]]*$/d' | tail -1)
  [[ "$last" =~ ^(STEP\ [0-9]+\ DONE|WAITING_CI|$2|STUCK\ .+)[[:space:]]*$ ]]
}

# Stops $1 and all its descendants in one signal. One `kill`, because the session's exit ends this
# reaper (loop_run_reaped stops it) before it could get round to the children.
_loop_kill_tree() {
  kill $(_loop_tree "$1") 2>/dev/null
}

_loop_tree() {
  local kid
  echo "$1"
  for kid in $(pgrep -P "$1"); do _loop_tree "$kid"; done
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

# A stop file is only consumed when the loop gets back to its check, so one touched for a run that
# then died (Ctrl-C, closed terminal, STUCK) outlives it and would stop the next run before its
# first step. Called at startup, before the pid file is rewritten: $1 the stop file, $2 the pid
# file. A stop file is kept while the pid it belongs to is still alive.
loop_clear_stale_stop() {
  local stop=$1 pidfile=$2 pid
  [ -e "$stop" ] || return 0
  pid=$(cat "$pidfile" 2>/dev/null)
  if [ -n "$pid" ] && [ "$pid" != $$ ] && kill -0 "$pid" 2>/dev/null; then return 0; fi
  rm -f "$stop"
  echo "[$(date '+%F %T')] removed a stale $(basename "$stop") left by an earlier run"
}

# Machine-wide cap on concurrent loop sessions, across every loop and set. Each session runs its
# own gate builds and subagents. Two sessions may run at once by default (matching the two Gradle slots); additional loops queue
# for a slot and take turns, one session each. Same shlock scheme as
# scripts/gradle-locked: a slot held by a dead process is reclaimed automatically.
LOOP_SESSION_SLOTS=${LOOP_SESSION_SLOTS:-2}
LOOP_SESSION_LOCK=""
_loop_session_base="$HOME/.cache/argentum/loop-session.lock"

# Live waiters other than us (stale markers from dead loops are cleared).
_loop_session_others_waiting() {
  local f pid
  for f in "$_loop_session_base".wait.*; do
    [ -e "$f" ] || continue
    pid=${f##*.}
    if [ "$pid" = "$$" ]; then continue; fi
    if kill -0 "$pid" 2>/dev/null; then return 0; fi
    rm -f "$f"
  done
  return 1
}

# Blocks until a session slot is free. $1 is the loop's stop file: returns 1 if it appears while
# waiting. Callers define `log`.
loop_session_acquire() {
  local stop=$1 slot lock waited=0
  command -v shlock >/dev/null 2>&1 || return 0
  mkdir -p "$(dirname "$_loop_session_base")"
  touch "$_loop_session_base.wait.$$"
  while :; do
    for ((slot = 1; slot <= LOOP_SESSION_SLOTS; slot++)); do
      lock="$_loop_session_base.$slot"
      if shlock -f "$lock" -p $$; then
        LOOP_SESSION_LOCK=$lock
        rm -f "$_loop_session_base.wait.$$"
        return 0
      fi
    done
    [ "$waited" -eq 0 ] && log "another loop's session is running — queuing for a session slot (LOOP_SESSION_SLOTS=$LOOP_SESSION_SLOTS)"
    if [ -e "$stop" ]; then rm -f "$_loop_session_base.wait.$$"; return 1; fi
    sleep 20; waited=$((waited + 20))
  done
}

# Frees our slot; if another loop is queued, steps aside long enough for it to take the slot.
loop_session_release() {
  [ -n "$LOOP_SESSION_LOCK" ] || return 0
  rm -f "$LOOP_SESSION_LOCK"
  LOOP_SESSION_LOCK=""
  if _loop_session_others_waiting; then sleep 30; fi
}
