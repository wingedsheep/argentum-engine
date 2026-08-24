# Fanning the sweep out to subagents

Past ~15 cards, author them in parallel. The whole difficulty is that the agents cannot see each other,
so everything they need must be *in the brief* and everything they produce must be checked *mechanically*.

## Hand agents the brief, not facts from your head

Pre-fetch every card's Scryfall payload once (the set cache `~/.cache/scryfall/<code>.json` already holds
`rarity`, `collector_number`, `artist`, `image_uri`, `flavor_text`, `oracle_text` per card — that is what
`just assay-ready <CODE> --json` is for) and render one brief per card containing:

1. the **authoritative metadata**, pre-filled into a `card(...)` skeleton;
2. Assay's `compile` JSON for that card — the model to author *to*;
3. a `// ABILITIES GO HERE` marker;
4. the copy-from card for each mechanic, from the Stage 1 capability table.

Then put this sentence in the shared `COMMON.md`: **"copy metadata verbatim from the brief."** On MH3
three prompts carried a P/T typed from memory (Nightshade Dryad, Furnace Hellkite, Warren Soultrader) and
all three were wrong; every agent used the brief and *flagged the discrepancy*. That one sentence turned
three would-be bugs into three reports.

## A generated skeleton is code — lint it like code

Read two rendered briefs before fanning out. Every one of these shipped or nearly shipped:

- **Multi-line `oracleText` with a leading `+`** does not parse. A newline ends the expression and `+ "…"`
  is an invalid unary-plus statement. Use the corpus's trailing-`+` form. CLB's generator got this wrong
  and all ten agents silently rewrote it; had they copied literally as instructed, every multi-line card
  would have failed to compile.
- **A `*` power closes a KDoc.** Rendering the header as `*/5` terminates the comment block. The corpus
  writes `* / 4` (Haughty Djinn).
- **A characteristic-defining `power` renders as `power = *`**, which is not Kotlin. Emit no `power` line
  and a comment pointing at the CDA instead (Filigree Attendant).
- **`flavorText` copied from Scryfall carries real newlines**, which end the Kotlin string literal. The
  escaper must cover `\n` and `\t`, not just quotes and `$`.

## Check the output mechanically

Diff every written file's metadata back against the brief — but **scope the field match to inside the
`metadata { }` block**. A naive first-match regex hits the `CreateToken` `imageUri` instead and silently
overwrites token art with the card's own image. That bug shipped 9 corrupted cards before the differential
caught it.

## The two import traps

- `GameObjectFilter` is `com.wingedsheep.sdk.scripting.GameObjectFilter`, but `GroupFilter` and
  `TargetFilter` are `…scripting.filters.unified.*`. The package is real, which is why the wrong guess
  looks right — one agent's wrong import was the single compile error across 120 files.
- A free-form land subtype needs **no** SDK entry: `TypeLine.parse` wraps every subtype word in
  `Subtype(it)` with no closed vocabulary, so `Land — Sphere` just works. Add a `Subtype` constant only
  when something filters on it.

## Wordings worth naming in the brief

These are the shapes the differential caught, so pre-empt them:

- **"Whenever this artifact or another artifact you control enters"** is `TriggerBinding.ANY`, not
  `OTHER`. The two wordings sit one card apart in the same set (Veil of Assimilation vs Mandible
  Justiciar); calling it out is what kept them distinct.
- **A bare tribal noun is permanents.** "Zombies you control" is `Permanent.withSubtype`, not
  `Creature.withSubtype` — four of MH3/J22's six defects were this one rule.
- **`YouControlAtLeast(1, X)` restates `Exists(You, Battlefield, X)`** — use the latter.
- **`controller = EffectTarget.Controller` is already the default** — writing it explicitly is a
  divergence, not a clarification.
