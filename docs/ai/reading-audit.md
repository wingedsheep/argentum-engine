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

The report writer is a test (`AiReadingAuditBenchmark`) that only runs with `-DaiReadingAudit=true`. The
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

Not every flag is a bug. Some effects legitimately carry no intent tag, and an until-end-of-turn
effect that is a drawback has nothing to time. A group is a list of places to look, ranked by how many
cards a fix would reach.

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
