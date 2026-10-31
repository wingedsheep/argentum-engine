---
name: ai-reading-loop
description: Launch a self-continuing agent loop that works the AI card-reading audit (`just ai-reading-audit`) one effect-type group per reviewed PR — teaching the engine AI's static readers (target polarity, card intent, combat reach, end-of-turn timing, escalate pickers) to read an SDK effect type, proving it with one AI decision test, and re-blessing the reading ratchet. Holds the per-step prompt that `just ai-reading-loop` runs. Use when asked to "work the AI reading audit", "teach the AI to read more cards", "loop over the unread effect types", or "close the AI card-reading gaps".
argument-hint: "[optional focus: an audit group key, e.g. 'polarity: ModifyStats', or a check name like 'timing']"
---

# AI reading loop

Works the [AI card-reading audit](../../../docs/ai/reading-audit.md) down, one **group** per PR. A group is
`<check>: <effect type>` — an SDK effect type one of the AI's static readers has no entry for. One reader
entry fixes every card that uses the type, so the group, never the card, is the unit of work. Each step
judges whether the group is a real gap, teaches the reader that effect, proves it with one AI decision test
that failed before, and re-blesses the ratchet's baseline (`AiReadingRatchetTest`). You **compose and
launch** the loop; you don't touch the readers in this turn.

**Prefer `just ai-reading-loop [model] [focus]`** when the user can run it in a terminal: `scripts/ai-reading-loop`
runs the step prompt below in one fresh headless session per step (no context growth), waits out CI and
usage limits between sessions, and stops on `touch .claude/loop-runs/ai-reading.stop`. The launcher reads
the prompt from this file, so this file is the single source of what a step does. The in-session `/loop`
variant is at the end.

It is [`set-loop`](../set-loop/SKILL.md)'s and [`assay-loop`](../assay-loop/SKILL.md)'s shape with a
different unit. Read `set-loop` for the harness table, the `<MODEL_ID>` resolution rule and why the
`[agent-loop: <model-id>]` PR label is mandatory; all three apply unchanged. The PR lifecycle (review
before opening, one CI run, merge on green) is `LOOP_PR_PROTOCOL` in `scripts/loop-lib.sh`, shared by every
loop.

## The step prompt

The launcher substitutes `<MODEL_ID>`, `<FOCUS_LINE>`, `<RUNS>` (the absolute `.claude/loop-runs`) and
`<REPO>` (the main checkout), then appends `LOOP_PR_PROTOCOL` and the progress-line rules. Everything
between the markers is sent as-is; keep it free of triple backticks.

<!-- step-prompt:begin -->
```
You are one iteration of an unattended loop that works the AI card-reading audit (docs/ai/reading-audit.md)
down, one effect-type GROUP per PR. A group is '<check>: <effect type>': an SDK effect type one of the engine
AI's static card readers has no entry for. You are a fresh session: your memory is the ledger at
<RUNS>/ai-reading.md (create it if missing; gitignored, never commit it) and GitHub. Do exactly ONE of the
steps below, then stop.
<FOCUS_LINE>

Scope:
- The readers are the ones the audit asks (ai/src/main/kotlin/com/wingedsheep/ai/engine/knowledge/):
  TargetPolarityAnalyzer (polarity), CardIntentAnalyzer (intent), CombatReach (combat), ExpiringGrantWindow
  and the HoldPolicy readings (timing), TargetSelection.payEscalateCost (modal). The audit itself is
  ai/src/test/kotlin/com/wingedsheep/ai/audit/AiReadingAudit.kt.
- Teach the EFFECT TYPE, never a card: no card names, definition ids or set codes in a reader. Read the
  effect's own fields (its target, its amount's sign, its counter type, its duration) and compose the readings
  that already exist (delegate a wrapper to its inner effect, reuse an existing TargetPolarity / IntentTag)
  before adding a tag or enum value. Composition over new types: AGENTS.md 'Card / effect authoring' and
  docs/sdk-design-principles.md ('Extend before you add', 'Four reusability rules') apply to reader
  vocabulary too.
- No mtg-sdk, rules-engine or card changes. A group the reader cannot read without SDK information that
  isn't there is 'needs-sdk' (add-feature work): record it and pick another.

Ledger format: a '## Groups' section, one line per group:
'- [ ] <group key> — <cards> cards — <note>', with [~] building, [r] in review (add PR number),
[x] merged (PR number, group unread before -> after), [a] audit false positive fixed in the audit (PR number),
[j] judgment call (say why), [s] needs-sdk (say what's missing), [!] needs human. A group split into
sub-shapes gets one line per sub-shape, keyed '<group key> / <sub-shape>'.

Other loops may run at the same time, so everything this loop owns is named by the prefix 'loop-ai-reading-':
every branch you create is 'loop-ai-reading-<short-slug>', and its worktree is
<REPO>/.claude/worktrees/loop-ai-reading-<short-slug>. Find our open PR by branch name only:
gh pr list --author @me --state open --json number,headRefName --jq '.[] | select(.headRefName | startswith("loop-ai-reading-"))'
Never touch a PR, branch or worktree without our prefix. Work on our PR in its worktree (recreate it with
'git worktree add' if missing). Never work in the main checkout.

1-4. Our PR is open -> follow the PR LIFECYCLE below (CI failed, conflicts, unreviewed, or ready to merge).
   Merge bookkeeping: mark the group [x] (or [a]) in the ledger with the PR number. On conflicts, never
   hand-merge ai/src/test/resources/ai-reading-baseline.tsv: take main's and regenerate it with
   'just ai-reading-baseline' after the rebase.
5. No PR open -> work the next group. 'git fetch', new worktree + 'loop-ai-reading-' branch from origin/main.
   a. PICK. In the worktree run 'just ai-reading-audit' and read
      ai/build/reports/ai-reading-audit/report.md (groups sorted by card count, with example cards);
      cards.tsv lists every flagged card with its keys. Skip every group already in the ledger (any status)
      and every group already IN FLIGHT: list all open PRs, anyone's —
      gh pr list --state open --limit 100 --json number,title,headRefName,files
      — and skip a group whose key or effect type a title names, or whose reader an open PR is changing for
      that effect (read that PR's diff only when the title is ambiguous). Take the biggest remaining group,
      or the focus group when one is set. Mark it [~] with its card count.
   b. JUDGE — before writing any code, decide whether the group is a real gap. Read the reader its check
      names and three or four example cards (their definitions under mtg-sets/). The question: would a
      correct reading change a decision the AI makes — which target it picks, whether it holds a spell, which
      combat response it tries, which modes it buys? Three verdicts:
      - REAL GAP: yes. Go on to (c).
      - AUDIT FALSE POSITIVE: the effect needs no polarity or intent and the reader is right to have no entry —
        a self-only effect with no target to choose, a drawback with nothing to time, bookkeeping that never
        stands alone. Then fix the AUDIT, not the reader: teach AiReadingAudit.kt not to flag that shape, with
        a one-line comment saying why it needs no reading. No decision test (there is no decision), but update
        AiReadingAuditSnapshotTest if it pins one of those flags. Say so plainly in the PR title and body
        ('AI reading audit: stop flagging <shape>'). Mark [a].
      - JUDGMENT CALL: the right reading depends on board context a static reader cannot see (the same leaf
        helps or hurts by situation), or a reading would be a guess. Mark [j] with the reason and PICK again
        in this same session.
      SUB-SHAPES: a group is too big for one PR when its effect has distinct sub-shapes — different target
      kinds, amount signs, counter types, or wrappers that each need their own reading. Split it in the ledger,
      one line per sub-shape with its card count, and take the biggest real-gap sub-shape only.
   c. TEST FIRST. Add ONE AI decision test, ai/src/test/kotlin/com/wingedsheep/ai/engine/<Effect>ReadingAiTest.kt
      (named for the effect, not a card), on ScenarioTestBase like CrewVehicleAiTest: a real example card from
      the group, in a position where the right play is CLEARLY better — a removal-shaped leaf aimed at the
      AI's own best creature, a pump on the opponent's attacker, a trick spent with no window, a lethal line
      missed — not a close call one heuristic tweak could flip. Assert the decision, with a withClue that
      names the reading. Run it with 'just test-class <Name>' and watch it FAIL for the reason the group
      names; keep the failure line for the PR body. If you cannot build any position where the reading
      changes the decision, that is evidence for AUDIT FALSE POSITIVE or JUDGMENT CALL: go back to (b).
   d. TEACH the reader that effect (Scope above). Re-run the test: it must pass. If the reading needs SDK
      information that isn't there, revert your edits, mark [s], and end with STEP 5 DONE (no PR).
   e. RE-AUDIT and RE-BLESS: 'just ai-reading-audit' again (the group's count must drop; record before -> after
      and the overall read-fully fraction), then 'just ai-reading-baseline'. Read the baseline diff: cards
      may only LOSE flags, and the ones that do should be the group's. A card that gains a flag is a reader
      regression: fix it, never bless it. Update AiReadingAuditSnapshotTest when a pinned card's flags move.
   f. CHANGELOG: add a line at the top of ai/CHANGELOG.md's list ('- #<pr> <what the AI now does
      differently>', following its header) ONLY when the new reading changes how the AI plays a common card —
      a group of commonly played effects (removal, pump, counters, tricks), not a niche one. A pure audit fix
      or a reading no common card reaches gets none; say which in the PR body.
   g. Commit by explicit path, never 'git add -A': reader + test; then the audit/snapshot changes and the
      re-blessed baseline. Gate with 'just loop-gate' and review per the PR LIFECYCLE, then open the PR titled
      '[agent-loop: <MODEL_ID>] AI reading: <group key>' (or '... AI reading audit: stop flagging <shape>').
      The body gives: the group key and sub-shape, the JUDGE verdict and why, the reader change in one
      paragraph, the decision test and the failure it showed before the fix, the group's unread count and the
      read-fully fraction before -> after, a summary of the baseline diff, the changelog line or why there is
      none, and that it came from an agentic loop driven by <MODEL_ID>. Mark the group [r] with the PR number.

If a step fails twice for the same PR, comment on it, mark the group [!] in the ledger, and move on.
STOP when PICK finds nothing worth a PR: every remaining group in the top fifteen is already in the ledger
or in flight, is a judgment call, or touches fewer than 5 cards — or the last three picks all ended [j] or
[s]. Then answer AI_READING_COMPLETE with the ledger's totals.
Follow AGENTS.md; mtgish is irrelevant here and never blocks. Never revert or stash work that isn't yours.
An unrelated failure on origin/main (a test in a module your change doesn't touch) is NOT a reason to answer
STUCK: run the gates for the modules you changed, note the unrelated failure in the ledger and the PR body,
and carry on. Only answer STUCK when the failure is inside code your change touched or depends on.
```
<!-- step-prompt:end -->

## In-session variant (Claude Code `/loop`, Codex `/goal`)

When the user wants it in this session instead of the launcher:

1. Resolve `<MODEL_ID>` exactly as `set-loop` Step 1 says (the literal id of the model driving the loop,
   context suffix dropped; ask once, never guess). `<FOCUS_LINE>` is `Focus: none — take the biggest group.`
   or `Focus: '<group key>' — take that group (or its biggest sub-shape) first.`
2. Send `/loop` (Claude Code) or `/goal` (Codex) with the step prompt above, substituted, followed by the
   PR LIFECYCLE block from `scripts/loop-lib.sh` (`LOOP_PR_PROTOCOL`). In-session there is no launcher to
   wait out CI: a WAITING_CI answer ends the iteration and the next one picks the PR up again. Codex ends
   the goal on AI_READING_COMPLETE.
3. Tell the user the loop is running, its focus, and the `[agent-loop: <MODEL_ID>]` label its PRs carry.

## Why it's shaped this way

- **The group is the unit.** A misread card is almost always a missing reader entry for its effect type;
  per-card fixes would never converge and would rot as cards change.
- **Judge before you teach.** The audit flags every leaf with no reading, and some leaves rightly have
  none. A false positive fixed in the reader would invent a polarity or intent the AI then acts on; it
  belongs in the audit, where it stops costing every later step a look.
- **Test first, and only a clear decision.** The ratchet proves the reader *says* something new; only a
  decision test that failed before proves the AI *plays* differently. A close-call position would pass or
  fail on unrelated heuristics and prove nothing.
- **Sub-shapes, one per PR.** One effect type often hides several readings (a stat change that is a pump
  or a shrink by sign). Splitting keeps each reading reviewable and each test about one thing.
- **The baseline diff is the review.** Re-blessing in the same PR shows exactly which cards moved to
  read-fully, and that none gained a flag.
