---
name: add-card
description: Implements new Magic: The Gathering cards for the Argentum Engine. Use when adding a new card, the user provides one or more card names to implement, or asks to implement a specific MTG card. Several cards that reuse existing primitives can be implemented together into one PR.
argument-hint: <card-name>[, <card-name>…] [--set <set-code>]
---

# Add MTG Card

Implement the card — or cards — named in the request. `--set <set-code>` is optional and defaults to `por`
(Portal).

Deeper references, load when the step calls for them:

- [`examples.md`](examples.md) — card definition, effect, and test templates for every common shape
- [`new-sdk-types.md`](new-sdk-types.md) — wiring a new effect/trigger/keyword/counter type (Step 4)
- [`reprints.md`](reprints.md) — adding a `Printing` row for an already-implemented card (Step 1)

## Batching: several simple cards, one PR

`CONTRIBUTING.md` sets the policy. Cards built entirely from existing `Effects.*` / `Patterns.*` **may
share one branch and one PR** — that's the house shape (`Add five Aetherdrift cards`), and it amortizes one
30-minute gate slot over all of them. A card that needs *new* engine vocabulary does **not** batch: it gets
its own PR, with tests for the primitive itself, so a regression in it can be reverted without touching
unrelated cards.

Given several card names:

- Run **Steps 0–9 once per card**, committing each card separately (Step 10's commit rule) so a bad card
  can be dropped without unpicking the others.
- Run **Step 10's gate once**, at the end, for the whole batch. Don't gate per card.
- **A card that turns out to need new SDK vocabulary leaves the batch** the moment Step 4 says so. Reset
  its commit, report it as dropped, and ship the rest. Do not grow the PR to cover it, and do not
  fake it with an existing primitive that only works in the common case.

Three things stay strictly per-card no matter the batch size: its own `{CardName}.kt`, its own
`{CardName}ScenarioTest.kt` where Step 5 calls for a test (**never** a shared `{Something}BatchScenarioTest`
— AGENTS.md → Hard rules), and its own Step 9 re-verify against Scryfall. Batching is a PR-shape decision;
it never merges two cards' work into one artifact.

Keep a batch to roughly five cards, and prefer cards that share a colour, mechanic, or cycle — a reviewer
reading five cards built on one idea evaluates that idea five times, instead of five ideas once.

## Guiding principle: rules-faithful, no shortcuts

Model exactly what the card does under the Comprehensive Rules, not a convenient approximation.

- **Don't fake an unsupported mechanic** with a lookalike that only works in the common case. If the card
  needs new vocabulary, build it properly (Step 4).
- **Don't drop edge cases** — "may" declines, fizzles, the source leaving the battlefield, zero/empty
  inputs, simultaneous triggers, replacement interactions, layer/timestamp ordering. If you can't cover
  an interaction, say so; don't silently skip it.
- **Don't approximate timing** — cast-time vs resolution-time choices, intervening "if" clauses,
  last-known information, turn/step boundaries. Follow the rules and Scryfall rulings, not what's easiest
  to wire up.
- **Honor oracle text literally** — "up to N target" isn't "N target", "you may" isn't "you do", "each"
  isn't "target".

When the faithful implementation is more work than the shortcut, do the work — or stop and tell the user
what's missing. **A card that looks right but resolves wrong is worse than an unimplemented one.**

## Step 0: mtgish draftability probe — fail fast

```bash
just coverage-fidelity --emit "<Card Name>"     # prints a generated cardDef DSL + a tier banner
```

The cheapest possible signal: it tells you in one command whether the tooling can already draft this
card, and hands you a starting `cardDef` with metadata pre-filled. Read the trailing tier line:

- **`fidelity tier: AUTO`** — whole card rendered. Use it as your Step 3 draft, treating every line as a
  claim to verify.
- **`fidelity tier: SCAFFOLD`** (leads with `// TODO:` / `// STRUCTURE needs human wiring:`) — keep the
  structure as a skeleton and hand-fill every marker in Step 3. Don't ship the stubs.
- **Blocked / nothing emitted** — implement from scratch.

Two things the emit deliberately cannot do, so **Step 1 always runs anyway**: its Scryfall lookup is keyed
to wherever the card is *already* implemented (else POR), so collector number, artist, flavor, and image
may be for the wrong printing; and it can't make the canonical-placement decision, which needs the
printings list.

**Distrust the draft on the engine's known-sloppy shapes** — additional costs, cast/activation-time value
choices (X, chosen creature type, chosen color), and inheriting a cast-time choice into later effects.
Model those by hand and prove them with a scenario test even on an AUTO render. A confidently-wrong
generated card is worse than none.

## Step 1: Scryfall lookup and canonical placement

**Always fetch real card data before implementing — never guess text or stats.**

1. `https://api.scryfall.com/cards/named?exact=<card-name-url-encoded>&set=<set-code>`

   **Always include `&set=`** — rarity, collector_number, artist, flavor_text, and image_uris differ per
   printing. Take: name, mana_cost, type_line, oracle_text, power, toughness, colors, rarity,
   collector_number, artist, flavor_text, image_uris.normal.

2. **Oracle errata and rulings.** `oracle_text` is the current wording; note significant errata in a
   comment above the definition ("Bury" → destroy + can't regenerate, "Remove from game" → exile). Follow
   `rulings_uri` and add mechanically significant rulings to `metadata { }` via `ruling(date, text)` —
   especially rules updates that changed behavior (Kamigawa Wall/Defender errata, creature type updates,
   functional errata). Skip reminder-text rulings that just restate the rules.

3. **Parse the oracle text** into card type, abilities (spell/triggered/activated/static/replacement),
   targeting, keywords, and conditions.

4. **Decide the canonical set before writing any code.** The canonical `CardDefinition` must live in the
   card's **earliest real-expansion printing** per Scryfall, skipping `promo` / `token` / `art_series` /
   `memorabilia`. Many cards in newer sets are reprints.

   - **Already implemented elsewhere?** `grep -rn 'name = "<Card Name>"' mtg-sets/src/main/kotlin/` — if a
     `CardDefinition` exists in another set, go to [`reprints.md`](reprints.md). Don't duplicate the script.
   - **Otherwise list all printings** via `prints_search_uri` (or
     `https://api.scryfall.com/cards/search?q=%21%22<Card+Name>%22&unique=prints&order=released&dir=asc`)
     and take the first real-expansion printing. Then:
     - *earliest == asked-for set* → continue normally here.
     - *earliest is already scaffolded* → implement the canonical **there** (re-fetch with
       `&set=<earliest-code>` for authoritative metadata) and add a `Printing` row in the asked-for set.
     - *earliest is not scaffolded* → scaffold it: a minimal `MtgSet` object under
       `mtg-sets/.../definitions/<earliest-code>/<EarliestSet>Set.kt` (mirror an existing set such as
       `definitions/tmp/TempestSet.kt`). `MtgSetCatalog` discovers it on the classpath — there's no list
       to append to and no service file. Then the canonical goes there and the asked-for set gets a
       `Printing` row.

     If scaffolding the earliest set would balloon the change set beyond this PR's scope, **ask the user**
     before falling back to a later set as canonical, and document the deviation in the commit message.

   - **Backlog bookkeeping** — when canonical and asked-for sets differ, check the card off in **both**
     backlogs that list it, so neither tracker shows it as missing.
   - **Verify when done** — `just check-card-printing "<Card Name>"` exits non-zero on any drift.

## Step 2: Find what already exists

**Most of what a card needs is already built.** Before modelling anything:

1. [`docs/card-sdk-language-reference.md`](../../../docs/card-sdk-language-reference.md) — the complete
   inventory of `Effects.*`, `Targets.*`, `Triggers.*`, `Filters.*`, `Costs.*`, `Conditions.*`,
   `DynamicAmount.*`, `Patterns.*`, keywords, static abilities, and replacement effects.
2. [`docs/architecture-principles.md`](../../../docs/architecture-principles.md) — how effects, executors,
   and continuations fit together.
3. `grep -r "<keyword-or-effect>" mtg-sets/src/main/kotlin/` — a card with a similar mechanic is the best
   template you'll find.

## Step 3: Model the card

**File:** `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/cards/{CardName}.kt`

There is no registration step — `CardDiscovery` scans the `{set}/cards/` package for top-level `val`s, so
a card in the right package with `val {CardName} = card("{Card Name}") { … }` is picked up automatically.

1. Write it with DSL facades over raw constructors. If Step 0 produced a draft, start from it rather than
   a blank page. [`examples.md`](examples.md) has a template for every common shape.

2. **Image URI — the single most common silent error.** Use the exact `image_uris.normal` from the API
   response, query parameter (`?1562911270`) included. These are hash-based paths; a generated or
   remembered one will be wrong in a way nothing catches. Verify: `curl -sI "<url>" | head -1` must be
   HTTP 200.

3. **Auras** need `typeLine = "Enchantment — Aura"` (modern Oracle errata) and an `auraTarget` in the script.

4. **Token images** — look the token up on Scryfall and pass `imageUri` to `Effects.CreateToken`:
   `…/search?q=t%3Atoken+name%3A%22<TokenName>%22+set%3At<set-code>` (token sets prefix with `t`, e.g.
   `tblb`). Not in the set? Drop the set filter and use the most recent printing. No token on Scryfall at
   all? Omit `imageUri`.

## Step 4: New SDK vocabulary, only if needed

Try composition first — `Effects.Composite` of existing effects, a `Patterns.*` recipe, or the
Gather → Select → Move pipeline for anything touching zones or the library. Most "new" effects aren't.

If the card genuinely needs new vocabulary, the bar and the wiring:
[`docs/sdk-design-principles.md`](../../../docs/sdk-design-principles.md) →
[`new-sdk-types.md`](new-sdk-types.md). If it's a whole mechanic rather than one primitive, switch to the
**`add-feature`** skill.

**This is the step that ejects a card from a batch.** The moment a card needs a new effect, executor,
condition, or keyword, it stops being batchable: reset its commit, note it as dropped, and finish the other
cards. It becomes its own PR — with tests for the primitive, not just for the card that uses it.

## Step 5: Tests

**Only if Step 4 added new vocabulary.** A card built purely from existing primitives is covered by the
snapshot and lint nets.

**File:** `rules-engine/src/test/kotlin/com/wingedsheep/engine/scenarios/{CardName}ScenarioTest.kt`

**One card per file** — a five-card batch unit gets five test files, never one shared
`{Something}BatchScenarioTest`. See AGENTS.md → Hard rules.

Set up a minimal board, exercise the new effect in isolation, verify the state changes, and cover the
edge cases. Templates in [`examples.md`](examples.md); harness details in the **`verify`** skill.

Also run **`/generate-scenario`** for the card — it emits a `DevScenarioController` JSON so the new
mechanic can be exercised by hand in the real client. Describe the board state you want (the card in
hand/play, the relevant opposing permanents, the decision you want to trigger).

## Step 6: Player experience review

**Always.** Walk the card from the player's side: what do they see, and what do they click?

For each ability or mode: how is it activated (action-menu button, auto-trigger — and does the button
text read clearly)? What decisions follow (targeting, selecting, choosing, ordering)? Which component in
`web-client/src/components/decisions/` handles each one, and does `DecisionUI.tsx` route the engine's
decision type there?

The components: `BattlefieldSelectionUI` / `BattlefieldTargetingUI` (permanents in play),
`GraveyardTargetingUI`, hand-click flow, `LibrarySearchUI`, `MultiZoneSelectionUI`,
`BudgetModalDecisionUI`, `YesNoDecisionUI`, and the choose-color/number/option UIs.

UX failures to catch:

- **An overlay where battlefield selection belongs.** Choosing among permanents already in play? Select
  on the battlefield. Overlays hide counters, effects, and which duplicate is which.
- **A flat card list across different zones.** "Choose from your hand or graveyard" needs zone labels —
  `MultiZoneSelectionUI` or separate decision steps, not one undifferentiated row.
- **Action menus enumerating every mode combination.** Budget modes → `BudgetModalDecisionUI`; modal
  spells → the mode-selection overlay.
- **Vague descriptions.** The `description` on a `BudgetMode`, `Mode`, or activated ability *becomes* the
  button text. Write it from the player's perspective.

If the existing components don't serve the card well, prefer extending one over creating a new one — but
build what's genuinely needed.

## Step 7: E2E test

**Only if Step 6 introduced a new visual or UX mechanic.**

`e2e-scenarios/tests/{set}/{card-name}.spec.ts`, using the `createGame` fixture and `GamePage` helpers.
Give both players at least one library card so nobody loses to an empty draw.

## Step 8: Trace new engine mechanics

**Only if Step 4 added new vocabulary.** Walk at least two scenarios — the happy path plus one
alternative (fizzled target, declined "may", trigger firing mid-resolution, replacement interaction,
simultaneous triggers, source leaves before resolution, X=0 / empty library / no legal targets).

For each, check every layer that could silently drop the mechanic:

| Layer | What to check |
|---|---|
| **SDK** | Modelled as pure data, every parameter present (target, duration, filter, amount), serializable |
| **Engine handlers** | Right executor picks it up, emits the right `GameEvent`s, returns the right `GameState` |
| **TriggerDetector** | New trigger detected from the emitted events, registered in `TriggerIndex`, matching the right event type |
| **StateProjector** | New continuous effect applied in the correct Rule 613 layer and reflected in projected state |
| **Continuations** | Player input pauses with a `PendingDecision` and resumes carrying targets/collections |
| **Cleanup** | Duration-bounded state removed at the right time |
| **Server DTO** | New `GameEvent` → branch in `ClientEvent.kt`; private info masked by `StateMasker` |
| **Frontend** | New decision → component in `decisions/`; new keyword/icon → `enums.ts`, display names, icon index |

Note each layer as handled or not-applicable, and **fix every gap before moving on**.

## Step 9: Re-verify against Scryfall

Re-fetch the card and compare field by field against what you wrote. This catches more real mistakes than
any other step.

Mechanical fields: `name`, `mana_cost`, `power`/`toughness`, `rarity`, `collector_number` must match
exactly. `type_line` uses modern Oracle errata. `colors` follow from the mana cost unless colorless or
special.

Then read the oracle text **line by line** against the script — each keyword to a `keyword(Keyword.X)` or
static ability, each activated ability to an `activatedAbility { }` with the right costs, each triggered
ability to a `triggeredAbility { }` with the right trigger, each spell effect to a `spell { }` block, "if
~"/"as long as" clauses to `Conditions.*`, and every "target" in the text to a matching `target =`.

The mistakes this actually catches: wrong P/T, a missing keyword, a missing "target" requirement, "when"
modelled as "whenever", "enters the battlefield" as "dies", one ability of a multi-ability card dropped
entirely, and "until end of turn" wired as a permanent effect.

## Step 10: Verify and commit

Run the gates via the **`verify`** skill — `just build` for cards on existing effects, `just test` when
Step 4 added engine behavior. **Once for the whole batch, after every card is written** — not per card;
`just` holds the machine to two concurrent builds and queues the rest for up to 30 minutes. Expect a
`CardDefinitionSnapshotTest` diff; re-bless with `just rebless-cards` and confirm **only your cards** moved
in the golden. An unrelated card moving means you changed shared SDK behavior — stop and report it rather
than re-blessing past it.

Run `just check-card-printing "<Card Name>"` for each card.

If `backlog/sets/{set-name}/cards.md` lists the cards, tick them (`- [ ]` → `- [x]`) and run
`just fix-backlog` to resync the header count.

Then commit **each card separately**: `Add {Card Name} to {Set Name}` (or `Add {Card Name} with {new
mechanic} support`), so a card can be dropped later without unpicking the others. **Commit to the current
branch — do not create a new one**, even on `main`. Don't push.

A batch PR title follows the house style — terse and imperative, `Add five Aetherdrift cards`. The body
gets one line per card (name, what it does, which existing primitives it composes), plus any card dropped
from the batch and why.
