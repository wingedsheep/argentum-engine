# AI card-reading audit

Which cards can't the engine AI read? The audit runs the AI's static card readers over every card in
the corpus and names what each one missed by **SDK effect type**: the effect or cost class the reader
had no entry for. A misread card is almost always a missing classifier entry for an effect type, not a
quirk of that card, and one entry fixes every card that uses the type. So the effect type is the unit
of work, not the card. The rationale is in the
[2026-10-31 AI-vs-humans plan](reviews/2026-10-31-ai-vs-humans-plan.md), "Corpus-wide: AI card-reading audit".

## Running it

```bash
just ai-reading-audit
```

It takes a couple of minutes, mostly the build. It prints the overall fraction and the biggest groups,
and writes three files to `ai/build/reports/ai-reading-audit/`:

| File | What it holds |
|------|---------------|
| `report.md` | Every group, sorted by card count, with up to five example cards each. |
| `summary.tsv` | The comparable numbers: `cards`, `read-fully`, `fraction`, two informational counts, then one `group<TAB>key<TAB>count` line per group, sorted by key. Two runs diff line by line. |
| `cards.tsv` | Each flagged card and its group keys. Use it to find example cards for a group, or to see why one card isn't read. |

The audit itself (`AiReadingAudit`) is in `ai/src/main`; the report writer is a test (`AiReadingAuditBenchmark`) that only runs with `-DaiReadingAudit=true`. The
same walk also runs in the normal `:ai:test` suite as a ratchet (see [The ratchet](#the-ratchet)). The
tool needs no server and no credentials.

## Reading a group

A group key is `<check>: <effect type>`. A card lands in every group it has a flag in, so group counts
add up to more than the number of flagged cards. A card is **read fully** when it has no flag at all.

| Check | Reader asked | What a flag means |
|-------|--------------|-------------------|
| `polarity` | `TargetPolarityAnalyzer`: each target slot of a spell (each mode of a modal one), an activated ability and a triggered ability | The slot reads `UNKNOWN`, so target choice falls back to the removal ranking. The flag names the leaves that name the slot and don't read on their own. `mixed(A+B)` means each leaf reads but they disagree. `Aura>X` means an Aura whose host reading is unknown, named by what it does to the host. |
| `intent` | `CardIntentAnalyzer.analyzeEffect`, per effect slot (spell, trigger, non-mana ability) | The slot earns no `IntentTag` at all, so every intent consumer sees nothing there. Each of its leaves is named. |
| `combat` | `CombatReach`, per leaf of a non-mana activated ability that changes P/T, keywords, P/T counters or blocking | Combat simulation won't try the ability as a response. |
| `timing` | `ExpiringGrantWindow` for instant-speed activations; the combat-trick and group-pump intent that `HoldPolicy` reads, plus `ExpiringGrantWindow`'s shrink and borrow readings, for instant-speed spells | An until-end-of-turn leaf has no "is there a window to spend this in" reading. |
| `modal` | `TargetSelection.payEscalateCost` | A choose-more-than-one modal whose per-extra-mode cost has no picker, so the AI can only pick one mode. |

Two label shapes need explaining:

- `ForEach>X` is the effect `X` inside a "for each …" body.
- `Pipeline(<from>→<to>)` is a lowered library, graveyard or hand pipeline (gather, select, move,
  plus bookkeeping) named as one type by where its cards come from and go to. Its stages mean
  nothing alone.

The `intent` check leaves out one shape the reader is right to leave untagged: a counter an
effect puts on its own source or on its own controller (a creature growing itself, a charge, oil or
quest counter, `{E}` or experience for you), unless it is a keyword counter. Counters on another
player (poison, rad) are not this shape and stay flagged. There is no target to choose and no window to
time, and tagging a self-grown flash creature `PUMP` would make `AmbushWindow` stop holding it.

Not every flag is a bug. Some effects legitimately carry no intent tag, and an until-end-of-turn
effect that is a drawback has nothing to time. A group is a list of places to look, ranked by how many
cards a fix would reach.

## Where a flag is: the per-ability reading

The flags above say *what* a reader missed on a card. `AiReadingAudit.read(card)` says *where*: one
`AbilityReading` per spell, triggered ability, state trigger and activated ability (static
abilities are listed but not checked), each with the findings of every reader asked about it —
which reader, which target slot, which step of the effect (a leaf, numbered in order), the SDK effect
type, a plain-English reason, and the audit flag it produces. Understood parts are findings too
(`target 1: reads as harmful`, `knows what it's for: removal`), so a view can show what is read as
well as what is not. `audit(card)` is `read(card).flags`, so the flag strings and the ratchet
baseline are the same as before.

The audit lives in `ai/src/main` (package `com.wingedsheep.ai.audit`) so it can run in a live
server, not only in the test suite. Two views use it:

- **The Assay explorer's AI reading panel.** `game-server` mounts the Argentum Assay explorer and
  supplies the readings (`AiReadingService`, through `:oracle-assay`'s `AiReadingProvider` hook). A
  card page shows each ability as read or with gaps, with the reasons; a card nobody has implemented
  is read off Assay's whole-card compile, as a preview. See the
  [oracle-assay README](../../oracle-assay/README.md#what-the-engine-ai-reads).
- **The Set Completion view's AI-reading score.** Every set gets the share of its authored cards
  the AI reads fully (and, secondary, the share of their checked abilities), banded ✓ 75%+, ◐ 55–74%,
  ! under 55%, and sortable by "Least AI-read". A set's **AI gaps** tab lists its unread cards
  grouped by flag key, biggest group first: the per-set slice of this report, and the groups the
  loop below works on. `GET /api/sets/coverage/ai-reading` and `GET /api/sets/{code}/ai-reading`.

Neither is baked. The server indexes every registered card on a background thread the first time
either view asks (about a second for the whole corpus), then holds it for the life of the process:
card definitions are fixed at boot, so it never goes stale, and a baked file would churn on every card
PR.

## Informational counts

`info.choose-n-modal` counts cards with a choose-more-than-one modal, and `info.crew-or-saddle` counts
cards with crew or saddle. The plan asked for both. They aren't gaps: `TargetSelection.modeSelections`
enumerates mode combinations, and `Strategist` fills crew and saddle payments. They're counted so a
change to either shows up.

## What it doesn't check

- **Activation and additional-cost shapes in general.** The AI's cost filler (`SelectionCostPayment`,
  `Strategist.withAutomaticPayments`) works on the engine's `AdditionalCostData.costType`. The legal
  action enumerator builds that from live state, and many cost shapes pause for a decision that
  `DecisionResponder` answers instead. No static reader says which SDK cost shape ends up where, and
  mirroring the enumerator in the audit would drift from it silently. Only the escalate case is checked,
  because `payEscalateCost` maps a fixed set of `CostAtom` shapes.
- **Usage weighting.** Groups are ranked by card count only.

## The ratchet

`AiReadingRatchetTest` runs the same full walk inside the normal `:ai:test` suite (CI's `ai` group;
the walk takes about ten seconds once the module is compiled) and holds it against a checked-in
per-card baseline, `ai/src/test/resources/ai-reading-baseline.tsv`. That file has one line per card in
the corpus at the last re-bless: the name alone when the card is read fully, otherwise the name and its
flags. Coverage can only move up. The test fails on two things:

1. **A card the baseline knows gains a flag.** A reader lost a case, or an edit to that card made it use
   an effect type the AI doesn't read. Every per-group unread count and the unread-card total, taken over
   the baselined cards, can only rise this way, so this is the regression check.
2. **A new group**: a flag key that no baselined card has. A new card brought an effect type the AI has
   never been asked to read, or a reader change opened a new gap.

Everything else passes on purpose:

- **A card the baseline doesn't list yet** passes when it is read fully or lands only in groups the
  baseline already has. Set loops add cards all the time. Failing every new card in a known gap would
  make each card PR edit the baseline, and every two card PRs would then conflict on it. Growth into a
  known gap isn't a reader regression, and the group is already on the A3 work list.
- **Coverage gains and removed cards** pass. The test prints a hint to re-bless and lock the gain in.

When it fails, read the message. It lists the cards and groups that moved. If a reader change caused
it, fix the reader. If it's expected (a card whose effect type the AI can't read yet), re-bless and
commit the baseline in the same PR:

```bash
just ai-reading-baseline
```

The baseline diff is the review: it shows exactly which cards gained or lost flags. A PR that teaches a
reader an effect type re-blesses too, so the cards it fixed move to read-fully in the diff. The file holds
no totals line on purpose, because every two re-blesses would conflict on it. The test prints the totals
instead. `just loop-gate` runs the ratchet whenever cards, the SDK or the engine changed.

## Keeping it honest

`AiReadingAuditSnapshotTest` runs in the normal suite too. It pins the exact flags on a dozen cards,
covering both read and unread shapes, so a reader change shows up as a deliberate edit there. When a
reader learns an effect type, remove that card's expected flag in the same change. When a reader loses
one, the test fails.

## The loop

`just ai-reading-loop [model] [focus]` works the audit down unattended, one group per PR, in a fresh
headless session per step. It has the same launcher shape as `just assay-loop` and `just set-loop`: a
model argument, PRs titled `[agent-loop: <model-id>]`, review before the PR opens, merge once CI is
green, a ledger at `.claude/loop-runs/ai-reading.md`, and a stop file at
`.claude/loop-runs/ai-reading.stop`. `focus` is a group key (`"polarity: ModifyStats"`) or a check name
(`timing`). The step prompt lives in the [`ai-reading-loop`](../../.agents/skills/ai-reading-loop/SKILL.md)
skill, which also describes the in-session `/loop` / `/goal` variant. `scripts/ai-reading-loop` reads the
prompt from there.

Each step:

1. Runs the audit and takes the biggest group, or the focus group. It skips groups already in the ledger
   and groups an open PR is already working on.
2. Judges whether the group is a real gap. If the reader is right to have no entry (a self-only effect,
   a drawback with nothing to time), the step fixes the audit instead and says so in the PR. If the right
   reading depends on board context, the group is a judgment call and is skipped. A group whose effect has
   distinct sub-shapes is split, and one sub-shape goes in each PR.
3. Adds one AI decision test, named for the effect, on an example card from the group. The test must fail
   before the reader change, in a position where the better play is clearly better.
4. Teaches the reader the effect type, never a card. It composes existing readings before adding new
   ones and changes no SDK or engine code.
5. Re-runs the audit, re-blesses the baseline with `just ai-reading-baseline`, and checks that cards only
   lose flags.
6. Gates with `just loop-gate` and opens the PR. It adds an `ai/CHANGELOG.md` line only when the new
   reading changes how the AI plays a common card.

The loop stops when the top groups are all in the ledger, in flight, judgment calls, or too small to
matter (under five cards).
