# `:oracle-assay` — Argentum Assay

Guidance for agents working in this module. Read [`README.md`](README.md) first — it is the
authoritative reference for the commands, the verdict table, the differential gate's buckets, and
what the gates have found so far. The design is [`../docs/oracle-assay.md`](../docs/oracle-assay.md);
the build order is [`../docs/plans/oracle-assay.md`](../docs/plans/oracle-assay.md).

This file is the *architecture*: the rules that decide whether a change belongs here and what shape
it has to take.

## What this module is

A bidirectional Oracle-text grammar: Scryfall JSON in, `mtg-sdk` models out, every rule written in
both directions so the corpus proves it without a human reading the output.

It is an **auditor before it is a generator**. Its output today is two documents — parser coverage,
and a continuously-updated `mtg-sdk` gap report ranked by cards blocked. It is **not a runtime card
loader** and never will be; ground truth stays a human-authored `cardDef` with a passing scenario
test.

**The one carve-out: the Scenario Builder's custom-card sandbox.** `compile/CardCompiler.kt` turns a
Scryfall(-style) card object into a whole `CardDefinition`, and `game-server` depends on this module
to offer that in the builder — paste a card, see what Assay reads, play it. It is the design's own
"Custom cards" note made executable, and it stays inside the rule because of four constraints that
are enforced in code rather than by convention:

- **Dev-gated** — `AssayCardService` reads `game.dev-endpoints.enabled`, and the *player-facing*
  `/api/scenarios` goes through the same gate rather than a second one that could drift.
- **Session-scoped** — a compiled card is registered into a `CardRegistry` overlay for one scenario.
  It cannot be drafted, deck-built, persisted, or seen by another game, and the corpus is untouched.
- **Whole cards only** — a card any of whose lines Assay cannot read is refused, with the line that
  stopped it. There is no best-effort mode and no flag to add one; a card that silently dropped an
  ability would test *green* and mean nothing.
- **Still not a loader** — nothing loads `mtg-sets` through this. The corpus is hand-written cards
  with scenario tests, exactly as before, and this module's own dependency stays `:mtg-sdk` only.

`:mtg-sdk` is the **only** production dependency, and that is load-bearing rather than tidy. Not
`:rules-engine`, not `:mtg-sets`, not `:mtgish-tooling` — a dependency on the engine invites a
runtime loader, and one on the incumbent pipeline re-imports the vocabulary Assay exists to replace.
The differential's goldens are read as *files* (`mtg-sets/src/test/resources/snapshots/cards/`,
decoded by the SDK's own `CardLoader`), which is why the rule still holds.

## The four invariants

Everything below is a consequence of these. A change that trades one of them away is not a change to
review, it is a change to decline.

1. **Bidirectional or it doesn't ship.** A canonical rule registers `build` *and* `match`;
   `PhraseBuilder.finish` refuses to construct one without both. The gate is
   `print(parse(normalize(t))) == normalize(t)`, and a one-directional rule has nothing to prove.
2. **One printed form per model.** Where English spells one meaning two ways, exactly one rule is
   canonical and the other is `alternate(...)`. Printing must be determined by the model, never by
   `oneOf` ordering.
3. **Declining is success.** Unparseable text is counted, ranked, and named — never approximated.
   A decline is the module's *product*: it names a missing capability in Argentum's own vocabulary.
4. **A phrase never throws.** A leaf that reads a malformed symbol returns no parse. A grammar that
   crashes on the corpus cannot report fineness, and "declining is success" only holds if declining
   is always reachable.

## Mapping to the SDK

- **There is no Assay IR, and none may be introduced.** Rules parse straight into `mtg-sdk` types.
  `CardFragment` is the one near-miss and it is not a counterexample: it holds SDK values and says
  *which of a card's two behavioural slots* a line fills. Nothing in it is ever translated, only
  destructured. If you find yourself defining a type to hold "what the text means", stop — that type
  belongs in `mtg-sdk` and it is `add-feature` work, not Assay work.
- **`build` goes through the SDK's companion facades** — `Effects.Destroy(...)`,
  `KeywordAbility.flashback(...)`, `Triggers.EntersBattlefield` — for the reason cards do: the
  facades are the curated surface, and this is the half that would otherwise drift from how cards are
  actually written. `match` necessarily destructures concrete classes; that asymmetry is inherent to
  a bidirectional rule, which is exactly why the `build` half must not compound it.
- **Where a keyword is *lowered* rather than stored, call the lowering — don't restate it.** Equip is
  the worked example: a card writes `equipAbility("{1}")` and the DSL produces `equipCost` plus a
  whole activated ability. `Grammar.equipLine` calls `ActivatedAbility.equip`, the factory that body
  moved into, so both sides of the differential are one definition. Restating a lowering here would
  agree with the cards exactly until someone edited one of them, and the gate would then report every
  card with the mechanic over a change nobody made to a card. Note what this is *not* a licence for:
  extracting an existing lowering into a factory is fine, and it is the one thing this module may
  push into `mtg-sdk` on its own; **adding a type or a capability there is still `add-feature` work.**
- **An SDK gap is reported, never routed around.** If the SDK cannot express a card, the rule
  declines and the report ranks it. Do not model it in Assay, do not approximate it with the nearest
  effect, and do not add a type to `mtg-sdk` from inside this module — that goes through
  `add-feature`, with the SDK's own bar and its own reference-doc update.
- **Two SDK spellings of one thing get one rule, and a finding.** Registering both is genuine
  ambiguity. `Primitives.protectionScope` deliberately never emits
  `Simple(PROTECTION_FROM_EACH_OPPONENT)`; `ProtectionScope.Colors` is a scope the grammar never
  produces; `Mana` never emits `ManaColorSet.Specific`, which a handful of cards use for a dual
  land's line where 165 use two abilities. Each such omission carries a KDoc paragraph naming it as
  an SDK finding. Declining the minority spelling also polices the minority's membership: what earns
  `Specific` is a rider, a rider is what makes the line decline, so a card in that group whose mana
  line *reads* is in the wrong group. That is how Spider Manifestation's card bug surfaced.
- **A value the SDK carries twice is derived, not spelled.** `ActivatedAbility` says a mana ability
  is one in `isManaAbility` *and* in `timing`; no printed word says either, and CR 605.1a defines it
  as a property of the effect and the target list. `Activated.abilityFor` therefore computes both,
  which is the only reading that stays true when a rule later produces an ability the corpus has not
  seen. A rule that copied a majority value it had not derived would be reading a habit.

## Generalizing: fewer mappings, not more

The failure mode this module was built to avoid is mtgish's — 382 bespoke variants reached one
locally-reasonable two-line change at a time. The same curve is available here in the form of one
rule per printed phrase. Five habits keep off it.

**Write the rule *shape*, not the rule.** A family is a private function returning a `Phrase`, and
the members are rows in a list: `Keywords.costKeyword`, `numericKeyword`, `simple`,
`Steps.quantifiedPermanentSteps`, `Filters.controlledBy`, `Keywords.qualityRun`. Seventeen numeric
keywords and twenty-odd cost keywords are two shapes, not thirty-seven rules. Don't pre-abstract —
write it inline the first time, and factor when the *second* member of the shape appears.

**Lift, don't re-spell.** `Triggers` slots `Steps.step` whole and lifts its `CardScript` onto the
ability, so every step rule enriches every trigger rule for free. Any new sentence context —
activated abilities, modal spells, "you may", delayed triggers — must slot the existing effect
grammar rather than restating the verbs. The win is multiplicative; restating is additive and rots.

`Modal` is the rule read in both directions at once, and the cheapest band so far because of it. A
mode slots the cascade's whole `sentence`, so every effect the grammar can read is a mode; and the
family is offered *at* `Steps.step` rather than as a line rule, so every context that already slots a
step — spells, triggers, activated abilities — got modal abilities without being told. Ask of a new
construct which of the two directions it wants before writing it: a line rule reaches one position, a
clause reaches all of them.

The **cost band** is the same rule applied to a *vocabulary* rather than to a clause, and it is the
one to copy when the SDK has already done the factoring. `CostAtom`'s KDoc calls itself "the one cost
language" — one payable thing, carried into each context by that context's own `Atom` wrapper — and
the grammar had it the other way round: `Costs` read a list of cost sentences and
`Restrictions.additionalCostLine` read one of them again, separately. The fix was to make the
vocabulary a `Phrase<CostAtom>` and the two contexts two lifts of it. Read the SDK's own type before
writing a second vocabulary; if it already unifies the thing, the grammar's factoring should be the
same shape, and what stays outside the shared part (the self-costs, which a spell cannot pay) is then
a stated rule rather than a gap.

The band's other transferable finding is about **case**. A construct that lives in one sentence
position can spell its second capitalization as an `alternate` — parseable, never printed. The moment
it reaches a second position that spelling may become *canonical* there, and an `alternate` would
print the wrong one. Make the capitalization a parameter of the family and instantiate it per
position, exactly as `SelfSteps.retargetable` is instantiated per anaphor position.

**Layer over a predicate bag; never compose.** A `GameObjectFilter` is a bag with no canonical
spelling, so two rules that can each print *part* of one value leave printing underdetermined. The
answer in `Filters` is layering: one alternation spells the whole type phrase, exactly one optional
suffix owns `controllerPredicate` and strips precisely that field before delegating. Every new
dimension (power/toughness, colour, subtype, tapped-ness) adds one layer that owns one field — not a
combinator that can also print the others.

**An omissible modifier is a row, not template text.** The `.` decline family — the tail ranking's
number one at 179 cards — was one defect shape repeated: a rule whose template spelled a clause
English can leave off, so the *bare* sentence died on its own full stop. "sacrifice ~ unless you pay
{2}" could not read "Sacrifice ~."; "~ gets +1/+1 for each {counted} on the battlefield." could not
read "for each artifact you control." Two tests before you freeze a word into a template: does Oracle
print the sentence without it, and does the SDK have a distinct value for the version that has it? If
the answer is *yes, no*, the clause is a row of a shared layer and the absent spelling is a row too —
`Amounts.scopes` is the worked example, published once because five families had each frozen a
different subset of the same three rows. If it is *yes, yes*, the bare form is a separate rule over a
separate value, which is what "Sacrifice ~." needed. Watch the ranking for it: a decline whose tail is
a bare `.` is always this, never a construct.

**Generalize the axis when the rules define one.** `qualityRun` started as a colour-join fix and
generalized in the same change to any quality, to the Oxford-comma three-way, and to hexproof under
CR 702.11f, because the Comprehensive Rules define the join over *qualities*. Reach for the rule the
CR states, not the instance the card in front of you shows.

**Enumerate only where English carries a distinction the model doesn't — and say so in the KDoc.**
There are honest cases: "artifact or enchantment" is an ordered `Or` while "artifact creature" is two
predicates, and nothing in the shape says which; `{type}cycling` would need a lookahead that breaks
`token`'s ability to verify its own output. Both are enumerated with the reason recorded. An
enumeration with no such paragraph is a rule that has not been thought through.

**A new set is not a code event.** The corpus is all of Scryfall; `--set` is a report filter, nothing
more. Never write a set-scoped rule. If a new set forces a new *file*, that is the signal to ask
which existing family the mechanic is a member of — the expected cost of a set is rows in existing
lists, and the exceptions should be nameable.

A set is, however, a good *target*: reading all of one proves the grammar has no systematic hole in
that era rather than no hole in one family, which is a stronger statement than any percentage. The
Portal band is the first worked example — 200 of 200 cards, and the work was four pieces of machinery
(clause sequences, the layered noun-phrase cascade in two numbers, the two anaphors, the three
restriction vocabularies) plus rows. Nine cards in it needed a rule that unlocks only them; each says
in its KDoc why the alternative was not a smaller rule but a wrong one, which is the bar the "a rule
that unlocks one card needs a stated reason" line sets.

The **Legions band** is the second, and it is worth reading for the shape of a *hard* set: 145 of 145
cards, every one a creature, and nine families rather than nine rules — the subtype layer, the cost
atom run, the lord statics, grants in both their printed shapes, the counted-variable vocabulary, the
morph payoffs, amplify, the multi-ability trigger line, and the sequence fix that lets a target be
introduced by a clause other than the first. Two of those changed something outside `grammar/`, and
both are worth knowing before touching a leaf: a subtype is a *proper noun* standing where
`SentenceCase` has already lowercased it, and the fix belongs to the leaf and is gated on the SDK's
own type list — an ungated one reads "**Other** creatures you control get +0/+1." as a tribe called
*Other*, byte-perfect and wrong, which the differential caught and the README records.

A set is also worth reading **twice**. Bloomburrow's second pass cost rows in six existing families
and no machinery at all, and it moved the set 69 → 83 cards while moving the corpus 8,364 → 8,516 —
eleven times as far, because two of its rows ("each opponent loses N life", "~ deals N damage to each
opponent") are printed on five Bloomburrow cards and six hundred others. That ratio is the argument
for a second pass: the first pass takes a set's machinery and the second takes its *vocabulary*, and
vocabulary is what the rest of the corpus shares. The pass also produced the module's clearest case
of a construct that is not a line rule: gift's printed line means `KeywordAbility.Gift` plus a
derived enters trigger on a permanent and a modal fold inside the spell's own resolution on an
instant, and nothing but the **type line** separates them — which the line grammar does not have and
should not get. Both halves were written, the differential reported two instants being read whole as
cards the validator would reject, and both were reverted. A construct whose meaning depends on the
face rather than the line is a finding to report, not a rule to widen.

The **Bloomburrow band** is the third, and it is the shape of a set picked *because it is already
implemented*: every card in it has a golden, so every declined line is a grammar gap whose answer is
written and whose fix the differential confirms on the same run. That is where it paid — four pieces
of machinery moved 594 cards corpus-wide and surfaced sixteen bugs in the *corpus*, one of them in an
`mtg-sdk` trigger facade that 29 cards share. Two of the four went outside `grammar/` again, and the
reason is the one this file keeps stating: an ability word has no rules meaning (CR 207.2c), so it is
printed shape and belongs to `normalize/`, exactly where the attachment noun went. The other lesson
is subtractive — the keyword *run* deleted two rules while covering more, because the count belonged
in the slot and not in the rule, and a family with a `pairForm` boolean in it is a family with an
axis it has not named yet.

The **spell-cost band** is the one to read for a family whose axes are the SDK type's own fields.
`ModifySpellCost(target, modification, gating)` has three, the printed sentence has three variable
parts, and the grammar is their product — one subject slot shared by every clause, so the second
subject ("Creature spells you cast cost {1} less to cast.", 148 cards) was one row rather than a
parallel set. Three of its lessons generalize. **Read the SDK's duplicates before choosing a
canonical, and choose by reach**: `CostReductionSource`'s five `FixedIf…` cases restate
`CostGating.OnlyIf`, the corpus writes both 21 times each, and the gate wins because its slot is the
whole of `Conditions` while the `FixedIf…` cases can never become six. **A position can need its own
instantiation of `Filters`**: a spell is not a permanent, so a bare subtype in spell position is
`Any.withSubtype` and a `StatePredicate` is refused outright — the guard that made Dream Chisel
decline instead of round-tripping a different value. And **widening a condition vocabulary exposes
the sentences above it**: the new `Conditions` rows made Leonin Vanguard readable, and the
differential immediately showed `conditionalClause` scoping its condition over only the first clause
of a run — which inside a trigger silently dropped the CR 603.4 intervening-if, because `Triggers`
lifts only a *top-level* gate. The fix was the pay-gates' own shape: a clause run for the
consequence, and the rule made sentence-terminal — plus a named position, `runEndingInScopedClause`,
for the twelve lines that *end* in such a clause ("Counter target spell. If you control a blue
creature, draw a card, then discard a card."). Widening the full-stop join instead was tried and was
wrong, and the ambiguity gate said so in one run: the scope simply leaked one join further along.

The **counting band** is the second worked example of that same lesson, and it adds two of its own.
`DynamicAmount` is the SDK's one language for "a number the game works out", and `Amounts` was
holding a three-row `count` plus seventeen bespoke clauses each restating one verb over one count —
so the fix was the cost band's fix: a layered vocabulary, and every sentence a lift of it. Its first
new lesson is about **where a line's meaning is allowed to land**. A characteristic-defining ability
(CR 604.3) is not an ability the engine executes; it is the value behind the printed `*`, and the SDK
puts it in `CardDefinition.creatureStats`. So `CardFragment` grew a slot outside the script, one
field per characteristic — two fields rather than one `CreatureStats` because Yavimaya Kavu prints
the halves on separate lines — and `CardCompiler` is where the header's star and the text's
definition meet, fail-closed in both directions and with the star's *offset* compared as a number
rather than as a string (`1+*` in Oracle is `*+1` in the model). If a line's meaning has nowhere to
go in `CardScript`, the answer is a fragment slot and a note here, never an approximation into the
nearest ability list.

Its second lesson is about **printing conventions the model does decide**. A damage sentence puts its
"equal to …" clause before or after the recipient, 152 printed lines to 195, and neither is a
minority to decline — but two rules that can each print one model is printing left to alternation
order. The corpus draws the split on the *shape of the amount* (a property read off an object leads,
a tally trails), which is a fact about the model, so the two orders take disjoint halves of
`DynamicAmount` and the minority order for each half is an `alternate`. The mirror case is why life
gain is **not** in the band: "for each" already spells that model 131 times to 23, so adding the
clause there would have been the second printer, and an `alternate` would have left graveyard counts
parseable and unprintable. When two spellings collide, check which rule can print the *whole* domain
before deciding which is canonical.

The **top-of-library band** is the third instance of that same "read the SDK's factoring back"
lesson, and it is the one to copy when the recipe you need already exists and the grammar is calling
it wrong. `Patterns.Library` publishes these sentences as functions whose **parameters are exactly
the words that vary** — `keepDestination`, `restDestination`, `restOrder`, `count`, `filter` — and
[`Library`](src/main/kotlin/com/wingedsheep/assay/grammar/Library.kt) was calling them with every
parameter frozen, one whole-sentence rule per printed card. So "…and the rest on the bottom of your
library in any order" was a rule nobody had written rather than a *word* nobody had slotted. Before
adding a rule beside an existing one, check whether the two differ only in an argument the facade
already takes; if they do, the rule is a slot, not a sibling.

Three of its lessons transfer. **Put the noun inside the count**: "the top card" and "the top four
cards" are one slot because the phrase carries its own grammatical number, so no sentence above it
can get the agreement wrong — the alternative is the singular/plural split repeated in every
sentence, and one of them eventually getting it backwards. **Check agreement, don't spell it**: the
impulse anaphor ("that card" / "those cards") is decided by a count the sentence already carries, so
the rule's `build` returns null on a mismatch rather than taking two rows per duration; a second
place that decides the number is a second place to get it wrong. And **the canonical word order can
be decided by a field other than the one the counting band found**: every impulse duration is printed
both fronted and trailing, and which is the majority *flips with the duration* — "this turn" trails
115:40, `UntilEndOfNextTurn` fronts 59:16. A duration slot in one template would print the minority
spelling for two of three, so each duration takes its own pair of rows.

The **conditional-tapped-entry band** is the same lesson one step further out, and the step is worth
naming: *a rule this file wrote off as impossible can become possible without anyone editing it.*
`Replacements`' own KDoc said the check lands decline because "an `unlessCondition` is an arbitrary
`Condition`, and the grammar has no condition vocabulary yet" — true when written, and false from
the moment the spell-cost band built `Conditions`. So the whole band was one template with a slot in
it. **When a family's KDoc names a missing dependency as its reason for declining, that reason has an
expiry date; re-read the write-offs after every band that builds a vocabulary.**

Two of its findings transfer. **An elided verb is a clause boundary, and the article is what marks
it**: "a Mount or Vehicle" is one noun phrase and one filter, "a Plains or an Island" is two noun
phrases and a disjunction of *conditions* — and the hand-written cards drew the line in the same
place before the grammar did, which is what let both rules be canonical instead of one being an
`alternate`. And **a word that qualifies a count belongs to the count**: "two or fewer **other**
lands" is `AggregateBattlefield.excludeSelf`, not a filter predicate and emphatically not "total ≤
three" — twenty goldens had written the arithmetic, which is equal to the sentence only while the
source itself matches the filter, and all twenty moved in the same change. The facades that
migration earned (`Conditions.YouControlOtherAtLeast` / `…AtMost`) are the one kind of `mtg-sdk`
change this module may make on its own: naming an existing composition so the cards and the rule are
one definition, never adding a capability.

Its third finding is about the meta-tests. A row reading "it's your turn" was written into
`Conditions` and rejected by `SpellCostsTest`'s `every rule prints what it parses` — because
`SpellCosts.leadingGate` prints that same `IsYourTurn` as the fronted "During your turn, …" clause,
and says so in its KDoc. **A family meta-test catches cross-family collisions, not only dead `match`
halves**, and it catches them where the corpus gates cannot: two texts collapsing to one model is a
*printing* failure the touchstone only sees if some card happens to print the losing form. Adding a
row to a shared vocabulary is a change to every position that slots it — grep the value before you
write it.

The **prevention band** is the top-of-library lesson on an `Effect` rather than on a `Patterns`
recipe, and it adds three things worth carrying forward. First, **a family whose axes are one type's
fields still has to decide where each axis lives, and `null` is what forces the decision**:
`PreventDamageEffect.amount` is nullable and "prevent **all** damage" is that null, so it cannot be a
slot — a `build` returning null means the surface denotes nothing — and the quantifier became three
instantiations of the shape over disjoint halves of `DynamicAmount` instead. Ask of every axis
whether its value space contains an absence before reaching for a slot.

Second, **read the redundancy number after generalizing a family, not just the coverage number.**
`Combat` had held "prevent all damage that would be dealt to you this turn by attacking creatures" as
a whole sentence; the new source layer read the same text into the same model, and the report's
redundant-readings count went 0 → 2 in the run that added +75 cards. Nothing else would have said so
— the touchstone is happy, the differential is happy, and a second rule for one model is precisely
the configuration that becomes a hard `AMBIGUOUS` the moment either rule's model shifts. A band that
makes a sentence composable should assume the sentence was already spelled somewhere.

Third, **a whole-line probe is exact only when the construct behind the line is uniform.** The
spell-cost band's rule was that the probe is honest when the family owns the line, and this family
does — and it still overstated 101 → 75, because a family that is one SDK type's *product* has
members the type cannot reach. Forty of the corpus's prevention lines carry no duration and are a
different type entirely (`ReplacementEffect.PreventDamage` over an `EventPattern`, a vocabulary
nobody has built); the rest name a noncombat scope, a recipient set of players, or divided damage.
That last group is also this band's write-off with an expiry date, in the sense the tapped-entry band
records: the day some band builds `EventPattern`, `Prevention`'s KDoc is what says the prevention
statics are sitting there waiting for it.

The **batch-trigger band** — CR 603.2c's "one or more" — adds three, and the first is about *where a
rider lives*. "This ability triggers only once each turn." is `TriggeredAbility.oncePerTurn`: a field
on the ability, part of no event, so **one wrapper rule reaches every trigger sentence the grammar
can read** where a row per family would have been thirty rows. It also had to be written before the
family it was written for could land, because the fail-closed reconstruction each trigger family
performs compares the whole model — a capped ability *refused to print*, so every card carrying the
rider declined however ordinary its trigger. Ask of any rider whether it belongs to the sentence or
to the model the sentence builds; the second kind is one rule, and it is usually overdue.

Its corollary is about the meta-tests. `TriggersTest` asserted that an ability carrying `oncePerTurn`
refuses to print, and that assertion became *false* — correctly — the moment the rider was spelled.
**A fail-closed test names a field no rule spells, so it is expected to move every time a band spells
one**; the witness went to `triggersOnce`. A fail-closed test that never moves is one whose property
has quietly stopped being checked.

Second, **a position can change what an absent field means.** `Filters.pluralSubject` is a third
instantiation of the noun cascade beside `filter` and `spellQuality`, and its reason is sharper than
the spell-cost band's: every batched `EventPattern` folds a null `controllerPredicate` to "you
control", so the bare plural does not mean "any controller" there. Slotting `Filters.plural` would
have printed "creatures you control" out of `ControlledByYou` — a second spelling of what the event
already says — so the controller clause is a word in each trigger's own surface, one row per scope,
and the vocabulary stops one layer below the field. Crossed with each family's "other" flag that is a
six-row product per family rather than six rules. Before slotting an existing vocabulary, ask what
its *default* denotes in the new position.

Third, the probe's sharpest result so far, and it names the *next* band rather than this one.
`one or more …` led the tail ranking by every column — 355 cards, 140 sole, 359 lines — and the probe
put it at 89 lines and **35 whole cards**. Sub-family by sub-family the split was the whole story:
26 of 68 combat-damage payoffs read, against **6 of 48** for attacks and **2 of 24** for enters,
because a batch names a *set* and Oracle addresses it as "that many", "them", "those creatures". The
grammar has no vocabulary for a captured collection, and 261 of the family's lines are still waiting
on one. **When a probe's sub-families disagree by an order of magnitude, the number is telling you
where the real band is** — here it is the collection, not any of the trigger prefixes. The **fronted
duration** band below found the same shape one row down the same ranking, which is why the two are
worth reading together: a family that is a *position* measures its payload, not itself.

The **fronted duration** is the first band whose product is a *measurement*, and it is the one to
read before picking anything off the top of the tail ranking. "Until end of turn, …" was the second
row — 265 cards blocked, 189 sole-blocked — and moving the duration to the back of its own 266
declined lines finishes **five**. The rest decline again on the payload: 54 animate a permanent, 42
grant a quoted ability, 32 set base power and toughness, 14 pump by a count. So the honest reading is
that **a family which is a clause position measures its payload rather than itself** — the ranking
counts the cards whose *first* unreadable clause is this one, and for an opening clause that is every
card whose sentence the grammar cannot read at all. The band still ships, because the position is
what every one of those four families lands in the day it is written; what it must not do is claim
the 265.

Three things transfer. **The probe was exact for once, and the reason generalizes**: every previous
band substituted a stand-in prefix and overstated (234 → 183, 101 → 75), while this one performed the
family's own transformation — moving a word — so there was no gap between the measurement and the
rule to be wrong in. When a family's construct can be *applied* rather than approximated, the probe
stops being a prediction. Second, **a second spelling of one rule is not a second rule**: the fronted
form needs the identical `build` and `match`, so the kernel grew `PhraseBuilder.alsoSpelled` — an
extra surface template on the same rule, sharing its closures, alternate by construction — and
`Durations` owns the derivation. Copying a rule to move one word is how a grammar acquires two halves
that agree until someone edits one. Third, **the derivation belongs to the family and the capability
to the kernel**: `alsoSpelled` knows nothing about durations, which is what keeps it usable by the
next family that finds one model with two word orders, and what keeps Oracle vocabulary out of
`syntax/`.

The **entry band** — "When ~ enters …" and "As ~ enters, …" — is the one to read when the SDK offers
*two* spellings of one sentence. Its trigger half found `pairedTriggerRule` already written for
"attacks or blocks" and needed only the list, so the list was counted in the corpus rather than
designed: five joins are printed, and a cross product of the ten self-events would have been
forty-five rules for them. Its replacement half is `EntersWithChoice`, a type whose own KDoc calls it
"a single parameterized type" and which the grammar was calling with every parameter frozen — the
cost band's lesson a fourth time. Two things transfer. **Split the axes by how Oracle spells them**:
the kind of choice is a *noun phrase*, so it is a rule parameter and each is a row, while who chooses
is *one word position*, so it is a slot the whole noun list shares; getting that backwards is either
eight copies of a rule or a slot with nothing to put in it. And **two SDK spellings of one sentence
is a divergence to classify, never a fold and never a second reading**: `EventPattern.AnyOf` is one
ability watching both events, and three cards use it where sixty write two abilities, so the grammar
prints the majority and the gate reports the rest. A `match` that accepted both would be `AMBIGUOUS`
by construction; a fold entry would be the gate agreeing with itself.

The **step-trigger band** — "At the beginning of each opponent's end step, …" — is the frozen-facade
lesson a fifth time, and it is the one to read for *where a derivation lives*. The SDK's
`Triggers.phase(step, player, binding)` says in its own KDoc to "reach for this factory for any other
combination", and the grammar was calling the thirteen constants that call it with all three
arguments fixed. Three things transfer.

**A derivation the SDK publishes is the spelling; the family only chooses the membership.** Every
word the whose-turn layer needs — "your", "each player's", "each opponent's", "the chosen player's",
"enchanted player's" — is `Player.possessive`, which exists precisely so zone and step descriptions
do not each restate it, so the rule takes the `Player` and asks. A table copied into `grammar/` would
have agreed exactly until someone added a `Player`. But `possessive` is *total*, and "target player's
upkeep" is a sentence no card writes — so the members are an explicit list and only the spelling is
borrowed. Borrowing a total derivation without bounding its domain is how a rule acquires the ability
to print English that does not exist.

**Which spelling is canonical can be decided by the argument, not by the family.** `Player.Each` is
printed both bare and possessive, and the majority flips with the *step*: "each upkeep" 100:83, "each
end step" 98:23, and 0:13 for the draw step, where only "each player's draw step" is ever printed. One
template with the step slotted would print the minority form for a third of the family, so it is one
rule per step with its own `alsoSpelled` list — the same shape `TopOfLibrary`'s impulse durations
take, and the second time this exact question has come up. Check the majority *per value of the other
axis* before making an axis a slot.

**And the third band in a row to confirm it: a family at the front of its line measures its payload.**
This one was the tail ranking's third row (197 cards, 106 sole-blocked) and finished **3**. The probe
said 17 lines before it was written, which is the number to have believed. What the band is actually
worth is that its 200 lines are now keyed on what blocks them rather than on their opening clause, and
the ranking names three successors — "sacrifice ~." as a bare step (171 cards, now the table's top
row), the triggering *player* as a subject, and the delayed trigger. **When the fronted-duration
lesson applies, say the small number and publish the ranking it uncovered; that is the product.**

The top-of-library band's differential result is the argument for the whole discipline: it put
`SelectFromCollectionEffect` under comparison for the first time, and **every newly-compared card
that disagreed was wrong** — five had silently dropped `restOrder` so "in any order" resolved as "in
the printed order", two had dropped `showAllCards` so a player told to look at five cards saw only
the takeable ones, and Prophetic Bolt kept all four cards its text says to keep one of. A field the
grammar cannot yet produce is a field nothing is checking.

## Fail-closed matching — the rule that catches the dangerous bug class

**A `match` half reconstructs what `build` would have produced and compares the whole model.** Not a
walk over the fields it cares about. See `Steps.quantifiedPermanentSteps`, `Triggers.triggerRule`,
`Targets.permanentFilter`: each rebuilds and tests equality, so a script carrying an
intervening-if, an `elseEffect`, an `excludeSelf`, a non-battlefield zone or a once-per-turn cap
*refuses to print* rather than printing a sentence that quietly drops it.

A matcher that inspects only part of a value round-trips byte-perfectly while meaning something
else. That is the **reversible-but-wrong** class, the touchstone structurally cannot see it, and it
is why the differential gate exists. Equality-against-reconstruction makes the check exhaustive by
construction instead of by a list of fields someone has to remember.

The same discipline covers values the text does not determine — target slot names, `AbilityId`s.
Mint one fixed constant (`Targets.SLOT`, `Triggers.ID`); the differential normalizes both sides by
position. A rule that tried to reproduce a generated id would be reading a counter, not a card.

The **chosen count** band is the one to read before assuming a *word* can be a slot. "any number of"
stands where a number word stands, reads like one, and is a different SDK **value** in every position
it appears in: `TargetRequirement.unlimited` on a target, `CostAtom.VariablePermanents` in a cost,
`SacrificeEffect.any` in an effect, `SelectionMode.ChooseAnyNumber` in a pipeline. Slotting the
phrase into the counted rules would have made one model printable by two rules. Ask of any word that
looks slottable whether the SDK gives it the same *type* everywhere it is printed; if it does not, it
is a family per position and the word is a row inside each.

Three of its lessons transfer. **A band can be worth writing when its probe returns zero.** The
substitution finished three of 123 lines and no whole cards, and that is a measurement of the
*payload*, not a verdict on the count — the ranking fell from 123 cards to 77 and off the top of the
table because 46 lines started declining on what actually blocks them. A position band's product is
the ranking it leaves behind, which the fronted-duration band said about a clause and this says about a
word. **Two printed forms of one value is an `alsoSpelled`, and the reach can be larger than the
band.** "Remove any number of charge counters from ~" is the same `RemoveCounters(XValue, self)` as
"Remove X charge counters from ~"; one line moved seventeen lands onto a single remaining sentence,
where the whole `VariablePermanents` product delivered one card. And **a spelling that is ambiguous
by position must not be registered until the position can be seen.** CR 601.2b makes a variable
cost's count the ability's X, so "for each storage counter removed this way" *is* `XValue` — after a
cost. After an effect (Coalition Relic) the identical clause is a collection count. The grammar has
no way to scope a leaf inside `Steps.step` to the cost above it, so the spelling stays unwritten and
the write-off names its own fix: a fail-closed guard where an ability is assembled, refusing a script
that reads `XValue` under a cost that declares none. Registering it unscoped would have round-tripped
and meant something else — the reversible-but-wrong class, in one clause.

The **combat restriction** band is the one to read before believing a ranked family is a *missing*
construct. "Can't be blocked" led the tail ranking by every column (122 cards, 79 sole) and the
grammar had read it since Phase 1 — three times, each frozen into a whole sentence, covering three of
the twenty-odd combinations Oracle prints. `mtg-sdk` had the factoring right all along: every
`CantBeBlocked*` static carries the affected set as one `GroupFilter` and differs only in what it
forbids, so the grammar's job was to become the same **product** — a subject crossed with a
restriction — rather than to acquire a construct. **When a top-ranked family names something you are
sure is implemented, check how many of its combinations are, not whether any is.**

Four of its lessons transfer.

**A deliberate hole in a product is where the card bugs live.** The source's bare "~ can't be
blocked." stays an `AbilityFlag` (19 hand-written cards to 6), so the grammar must *not* offer a
source-scoped bare static — two rules for one text. But a card-level flag lands on the permanent the
card **is**, so on an Aura or an Equipment that same shortcut grants the evasion to the enchantment:
silently inert, and the board looks right. Three shipped cards were doing it. Ask of every hole in a
product whether the spelling that fills it elsewhere is *reachable* in the positions the hole covers.

**Neighbouring SDK families with opposite defaults for one field are a card bug generator.** Every
ability in `BlockingStaticAbilities.kt` defaults `filter` to `GroupFilter.source()`; `GrantKeyword`
and `ModifyStats` default to `attachedCreature()`. Air Bladder's two lines took the two defaults and
one of them was wrong. The grammar's answer is to spell the subject in every rule and never rely on a
default — and the finding goes in the *SDK's* KDoc, because that is where the next card author will be
standing.

**One printed sentence can be a row of a family it does not look like.** "Target creature can't be
blocked this turn." is `GrantKeywordEffect` — the same effect "gains flying until end of turn" builds
— over an `AbilityFlag` rather than a `Keyword`. A CR 702.x keyword is a *noun* a creature can gain;
an `AbilityFlag` names a sentence and has no noun, so Oracle prints it as its own predicate with the
duration spelled "this turn". That is an irregular *surface* on an existing model, not a second
vocabulary — and once it was a row, three more restrictions ("can't block this turn", "can't attack
this turn", "can't attack or block this turn") were rows of the same table for free. Before writing a
family, check whether the model you are about to build is one an existing family already builds under
a different word.

**A sub-band's card count is a claim about the payload, and it can be off by 19×.** The conditioned
form ("~ can't be blocked as long as defending player controls an artifact") is 31 of the family's
lines and an upper bound of 19 cards; substituting a readable conditional finishes **2 of 29**,
because the payload is `Conditions` and not this family. Writing the wrapper would have bought one
card. The band's product there is the *name of the next band*, which is the fronted-duration lesson a
fourth time. Note also that the exact probe corrected the first probe from 18 cards to 7 for the same
band's cheapest row — the fifth overstatement in the same direction, and it happened because the
first run substituted a stand-in for the printed filter as well as for the construct.

Two mechanical notes. `effectOver`/`memberOf` moved from `Steps` onto `Targets.Quantifier`, because
they are the same knowledge `requirement` is — what a quantifier *denotes* — and a second family
taking the whole table must not carry a second copy of the iteration-space decision. And a family's
findings are **not bounded by what the differential can compare**: Whispersilk Cloak and My Precious
have the same bug as Cloak of Mists and are not comparable (their line is a static clause run the
grammar does not read), so they were found by grepping the flag the band had just classified.

**Three anaphors, three positions.** Oracle's "it" means the source in a first clause ("Whenever this
creature attacks, **it** gets +2/+0"), the target in a later one ("Untap target creature. **It**
gets +2/+4"), and — inside a trigger whose event names a **filter** — the object that matched
("Whenever a Rat you control becomes blocked, **it** gets +2/+0" pumps the *Rat*); "that creature"
always means the target. `SelfSteps.anaphoric`, `Continuations` and `SelfSteps.triggering` are
reachable from disjoint positions for exactly that reason — registering any surface form in two of
them is two readings of one text. The differential found the second and third by *running*: both
wrong readings round-tripped byte-perfectly and meant a different creature.

The third one is also the worked example of **how** to add an anaphor position. The distinction
exists only at parse time — after parsing, "~ gets +1/+1" and "it gets +1/+1" are the same model, so
no remap on the built ability can recover it. The vocabulary is therefore written once
(`SelfSteps.retargetable`, a function of the target and the subject's spelling) and instantiated per
position, and `Steps.Cascade` makes the clause cascade above it a shape with two instances rather
than a second copy of `Steps.step`. Every leaf and every atom stays shared; only the dozen
combinators that join clauses are built twice. If a fourth position appears, it is another
instantiation — not another `oneOf` branch, which would be ambiguity by construction.

## Printed-shape information belongs to normalization

Line grouping, the `;` separator, reminder text, which noun a card uses for itself, and which
adjective it uses for the permanent it is attached to ("equipped creature" vs "enchanted creature",
one model and two words chosen by the type line) are properties of the *printed line*. The model has
nowhere to put them and must not grow somewhere.
`normalize/Normalizer.kt` owns them, every pass is invertible by construction (it records what it
removed and `restore` replays the inverses), and `NormalizedFace.restore(lines) == raw` is itself
gated — a normalization that cannot round-trip its own output would let any grammar look correct.

Corollary: **never "fix" a `VARIANT` by adding a field to the model.** A variant already says the
right thing — the reading survived, only the spelling moved. Encoding the spelling would be a lie
about where the information lives.

**Case is the same kind of information, and lives one step further out.** `syntax/SentenceCase.kt`
sits at the text boundary rather than in a normalization pass, because it moves nothing — it only
lowercases a letter Oracle templating guarantees is uppercase. It does that at *every* sentence start
in a line: the first word, the clause after each ability cost's `": "`, and the clause after each
full stop. Templates are therefore written mid-sentence throughout, and that is what lets `Activated`
slot `Steps.step` unchanged, and what lets one line hold several clauses, rather than either needing
a capitalized second copy of the effect vocabulary. If you find yourself wanting a capital inside a
template, the answer is almost certainly another sentence start this file should know about — the
full stop was added exactly that way, and the shock lands' `"If you don't, …"` had to be rewritten
mid-sentence in the same change.

## Ambiguity is a factoring signal

`AMBIGUOUS` is never resolved by picking a reading, reordering an alternation, or narrowing a rule
until the collision hides. Two rules that read one text into two models are a bad factoring, and the
fixes are structural:

- **Disjoint domains** — `drawOne` builds 1, `Cardinals.word` starts at 2. One printed form per
  model, nothing for the printer to choose.
- **`min` on a run** — a one-element list has no separator in it, so `separated(..., min = 2)` is
  what stops every single keyword reporting as grammar redundancy.
- **`alternate(...)`** — when both forms are real English and one is canonical.

Parsing returns *every* reading on purpose, so `oneOf` order is irrelevant and ambiguity has a
definition rather than a feeling. Keep it that way: an alternation whose order matters is a bug that
has not surfaced yet.

## Kernel mechanics worth knowing before you touch `syntax/`

| Thing | Why it is like that |
|---|---|
| `parseAt` memoizes, `parseHere` must not | Memo is keyed on (rule id, offset); that is what makes an all-readings parser affordable. |
| `ParseContext.parseCap` (64) | A span with more readings is a left-factoring bug, not genuine ambiguity, and is treated as a decline. |
| Left recursion is *reported* | It becomes a named decline, never a stack overflow — a grammar bug must not crash a corpus run. |
| `token` re-reads what it writes | The kernel cannot cross-check a leaf's two halves the way a template can, so `unparse` is verified against `read` on every call. |
| `furthest`/`expected` | The entire source of `assay explain`'s caret. Any new combinator must call `ctx.fail(pos, name)` where it gives up. |
| Declaration order inside an `object` | Initializers run in order; a rule referencing a later one reads a null out of a half-initialized object. Declare leaves first. |
| `alsoSpelled` shares the rule's closures | Two spellings of one model must not be two rules — a copied `build`/`match` pair agrees until someone edits one. The extra template is alternate by construction, and the *derivation* between the two spellings belongs to a grammar family (`Durations`), never here. |

## Adding a rule

1. Write it in `grammar/`, bidirectionally, through an SDK companion factory. Prefer a row in an
   existing family; if there is no family and this is the second of its shape, make one.
2. `just assay parse "<a card that uses it>"` — read the *verdict*, not just that it parsed.
3. `just assay-gate` — `MISMATCH` and `AMBIGUOUS` must stay **0**. Declines may go up or down freely.
4. `just assay-differential` — the only gate that catches reversible-but-wrong. A rise in
   `DIVERGENT` is the gate earning its keep, not a regression.
5. Add the surface form to the round-trip list in the matching test under `src/test/`.
6. If the rule reaches a `CardScript` slot the grammar could not previously produce, widen
   `CardFragment.merge` **and** the differential's completeness check — `MODELLED_SLOTS_NOTE` is the
   pointer between them, and the compiler will not remind you.

Three traps the kernel cannot catch for you:

- **A `match` half that quietly matches nothing** compiles, parses, and surfaces as a print mismatch
  far from its cause. The `every keyword rule can print what it parses` test exists for this; keep an
  equivalent meta-test for every new rule family.
- **Reversible but wrong.** "Elves" de-pluralizes to `Elve` and round-trips forever. Run the
  differential; it has already caught three of these.
- **A rule that prints a value it did not fully inspect.** See fail-closed matching above.

## Triaging the gates

- `MISMATCH`, `AMBIGUOUS`, and a non-invertible normalization pass fail the build. There is no
  acceptable non-zero value and no allowlist.
- A **divergence is classified**, as parser bug / card bug / fold — never left unexplained and never
  silenced to make a number look better. A divergence that turns out to be a bug in a hand-written
  card is the outcome worth the most.
- **The fold list (`Folds` in `gate/Differential.kt`) is reviewed, not grown.** Every entry stops the
  gate reporting something, so every entry has to state why it is not a difference — ideally in the
  SDK's own words. "They happen to agree today" is not a reason; two parallel implementations
  agreeing by accident is precisely what the gate should keep watching.
- **Scoping stays fail-closed.** A card is compared only where Assay reads *every* line, the text is
  the same text, the definition uses only modelled slots, and the lines fold into one card. Anything
  else lands in a named population bucket so the denominator stays visible. Widening a guard to raise
  the compared count is the gate lying to itself — it has happened three times, and each time it was
  found by running, not by reading.

## Scaling to the whole corpus

Phase 1 covers 5.5% of 34,882 cards with ~150 rules. The rules below are what keep the last 90%
from arriving as 3,000 one-offs. Everything here is cheap to watch now and expensive to discover
late.

**Watch three curves, as numbers rather than as principles.**

- **Cards covered per rule.** mtgish reached 382 bespoke variants one locally-reasonable two-line
  change at a time, and no single change looked wrong. The defense is a tracked number, not good
  intentions: a family whose marginal leverage collapses is N one-offs wearing a factory's clothes.
- **Redundant readings.** Two rules producing the *same* model for one text are reported and not
  gated — and they are exactly the configuration that becomes a hard `AMBIGUOUS` the moment either
  rule's model shifts. It is ambiguity's leading indicator, and the one thing here that grows
  quadratically with rule count. Noise at 150 rules; the number to watch at 1,500, ranked by which
  rules overlap.
- **The unchecked surface.** The fold list, the differential's population buckets, `alternate` rules,
  every `canonical = false`. Each is justified individually and none is free. A growing unchecked
  surface is how a green gate stops meaning anything.

**100% is the destination; coverage is the lagging indicator of it.** Fineness at 1000‰ means
`mtg-sdk` can express every card in Magic — the engine's actual goal, with Assay as the instrument
that measures it — so take the number literally. What it must never become is the quantity optimized
*directly*: coverage bought one rule per card is coverage that arrives as 3,000 rules nobody can
change, and it is reached by a sequence of individually reasonable commits. Work the ranked decline
list top-down by cards blocked and let the percentage be the consequence. 2,250 decline families is a
statement about sequencing, not a ceiling; a rule that unlocks one card and joins no family needs a
stated reason.

**Split the decline list by what is already implemented** — `just assay-report --implemented`. The
~8,900 hand-written cards are each proof that the SDK can express that card. So a declined line on a
card that already has a golden is a **grammar** gap whose known-good answer is sitting in the goldens
— the cheapest work in the module, and confirmable by the differential the moment it parses. A
declined line on a card nobody has implemented may be an **SDK** gap, which is `add-feature` work with
a much longer lead time. Ranking those two populations separately turns one long list into two
backlogs that different people can work in parallel, and it is the fastest route to the 100%.

**Rank sentences, not dead tokens, when you are choosing what to write next.** The report's table is
keyed on the token a line *died on*, which is the right key for "what is the grammar missing" and the
wrong one for "what should I write". A line dies on its first unknown token, so a trigger whose prefix
is already known dies somewhere after the comma and a trigger whose prefix is unknown dies on "At" —
the same missing verb therefore lands in several buckets, and a missing prefix looks larger than it
is. `assay-report --implemented --declines` prints every declined line; collapsing numbers and mana
symbols to a skeleton and counting those gives the shape ranking, and it disagrees with the token
ranking in ways that change the work. The step triggers are the worked example: 410 cards decline on
"At the beginning of…", and adding every step-trigger prefix moved whole-card coverage by 23, because
the other 387 were blocked on their effect clause all along.

**The ranking that has actually held up is over the parse's *tail*, and it is two measurements.**
Both are now in the tool — `just assay-report --rank tail`, and the probe box on the explorer's
decline-family page — because the spell-cast band had to be picked with a throwaway probe outside it.
That band is the worked example and the method is reusable verbatim:

1. Key each declined line on **the text from the decline's `position` on**, skeletonized and cut to
   its first three words (`DeclineKey.TAIL`). That is neither the token ranking (which over-weights a
   missing prefix) nor the shape ranking (which counts cards a family *mentions*): it names the piece
   of grammar that would have to exist for the line to get further. Then count, per family, the cards
   **all** of whose declined lines fall in it — the honest "sole-blocked" number, which the report
   prints as `sole` and the explorer as a column beside `Blocks`.
2. Before writing anything, **substitute a known-good prefix for the family** into those cards'
   declined lines and re-parse. That says how many payoffs the rest of the grammar can already read,
   which is the number the band will actually deliver. The spell-cast family predicted 234 whole
   cards and delivered 183; modal spells, which both other rankings put first, measured 126 and
   delivered **137**. Landfall is the standing example on the live grammar: 189 cards blocked, 104
   sole-blocked, and the probe says **48** whole cards would be finished.

Every ranking that skipped step 2 has overstated its band, four times in the same direction. Step 2
is family-specific, so it cannot be precomputed and does not belong in a report — but it is one
`PrefixProbe.run` over the live grammar, and it is what turns "which cards does this family reach"
into "which lines does it finish".

The modal band is the one case where the probe **under**-stated, and the reason is worth knowing
before trusting a number: it substitutes a prefix into the lines that *declined*, and the modal
family's payoff was partly in lines that had never declined as a family at all — the 204 cards whose
header sits inside a trigger, which the probe measured as trigger declines. A family that is a
*clause position* rather than a line shape will do this again. Read the probe as a floor there.

The fronted duration is the same shape and it came out the other way, which is what makes the pair
useful. It is a clause position too, and its probe was **exact** — 5 lines and 4 whole cards
predicted, 5 and 4 delivered — because the substitution *was* the rule: moving "until end of turn" to
the back of a line is precisely what the band taught the grammar to read. **A probe that performs the
family's own transformation has no gap to be wrong in; one that stands in for it does.** What the
ranking got wrong there was not the probe but the *card count*: an opening clause is where every
unreadable sentence dies, so 265 cards "blocked" and 189 "sole-blocked" were mostly cards whose
payload the grammar cannot read either. When the family sits at the *front* of its line, read the
sole-blocked number as an upper bound with no lower bound in it, and let the probe say the rest.

**A family that dies at offset 0 may be an artifact of your own templates, not of the corpus.** A
`TemplatePhrase` fails at the start of the *literal* it could not match, so a template that swallows a
whole clause into one literal — `"when ~ enters, {effect}"`, where the prefix and its comma are one
run — reports every near-miss at offset 0. `TAIL` then keys all of them on the opening words, and the
result is a family named after a construct the grammar has read since Phase 1. The trigger join found
the top row of the whole table that way: 177 cards and 88 sole-blocked on "When ~ enters …", which
dissolved into a hundred-odd per-payload rows of 15 the moment the prefix became a slot. So when a
ranked family names something you are sure is implemented, **check the template before believing the
number** — and note that the fix is the same edit reuse wants anyway: a clause that is a slot is a
clause another sentence can borrow. See [the trigger join](README.md#the-trigger-join).

**Three keyings, three biases, and knowing which to read.** `DeclineKey` holds all of them and the
gate computes all three in the one sweep, so the CLI and the explorer cannot disagree about a family.
Read the tail by default. Read `SHAPE` when the family's sentence *is* the whole line. Read `TOKEN`
when the parse dies at offset 0 — a line that read nothing has no tail short of itself, so `TAIL`
degenerates to `SHAPE` there and only the dead token holds the family together. The modal bullets
*were* the case — 2,015 declined `•` lines were one token family and hundreds of tail rows — and how
that resolved is the more useful half: a bullet is not a line, normalization now keeps a modal card's
rows together, and the family stopped being a ranking problem by stopping being 2,015 lines. A
keying that scatters one construct across hundreds of rows is worth reading as a question about where
the line boundaries are, not only as a question about which key to use.

The split re-weights the list rather than reordering it wholesale, which is itself the finding: the
top families are the same in both populations, so the grammar backlog and the SDK backlog are being
blocked by the same missing sentence shapes rather than by different ones. Where it does diverge is
worth reading — Phase 1's own target class is 97.1% over implemented cards against 84.1% corpus-wide,
because the keywords with no `Keyword` constant are exactly the keywords no card here uses yet.

**Keep the gate fast, because a gate nobody runs before pushing catches nothing.** A whole-corpus run
is ~5s at Phase 1 size. Three levers when it stops being, in order, none of which changes any rule's
semantics: index `oneOf` alternatives by their leading literal rather than trying all of them (and
the print side by the model's concrete type — `unparse` walks alternatives linearly today); cache
parses by normalized line across cards (66,793 lines, 39,746 unique, and the ratio improves as the
corpus grows); run cards in parallel, which is free because the parser is pure and `ParseContext` is
per-parse. Track the rate in the report so a regression is visible.

**Regressions need a diff, not a total.** At this size a change can move thousands of verdicts, and
"round-trips went up" hides the twelve cards that went *down*. Not built yet; when the totals stop
being reviewable, the shape that fits this repo is a committed ledger — one sorted line per card,
verdict plus decline token, pinned to the Scryfall bulk's version — re-blessed deliberately like the
card goldens, so every PR shows exactly which cards changed reading and a corpus refresh is its own
commit.

**Structure that survives 40 grammar files:** one file per semantic family, never per set or card;
dependencies a DAG through each family's public entry `val` (`Steps` → `Filters`/`Targets`/
`Cardinals`/`Mana`, `Triggers` → `Steps`, `Activated` → `Costs`/`Steps`/`Mana`); `Grammar.kt` the
only place families are combined. Rule names
stay unique and name their family, so an ambiguity diagnostic can identify both sides. Every family
gets the `every rule can print what it parses` meta-test through a shared helper, and a family that
is written but never wired into `Grammar` should fail a test rather than sit as dead code.

## Custom cards

Custom cards have no Scryfall text, so the touchstone's direction does not apply to them — but the
**inverse gate does**, and it is the more useful one: print the `CardDefinition`, reparse, compare
models. `model → text → model`. That is the Phase 4 renderer turned into a linter, and it runs on
custom cards with no new infrastructure because they are already hand-written definitions in the
goldens.

Three consequences worth getting right before the first custom set:

- **The grammar is the templating standard, and the custom card yields to it.** This inverts the rule
  for real cards. A real card that declines is an SDK gap to fix; a custom card that declines is
  usually a card to reword — its text is *authorable*, and text outside canonical Magic templating is
  text that will confuse players. Extending the grammar for a custom card is the exception and needs
  the same family test every other rule gets.
- **One grammar, never a fork.** A custom keyword is a rule like any other, in the same files, checked
  by the same global ambiguity gate. A custom surface form colliding with real Oracle text is
  precisely the finding you want; a separate custom grammar would hide it.
- **Separate the populations in the report.** A decline on a real card means "extend the SDK"; a
  decline on a custom card means "retemplate the card". Mixing them corrupts the ranked SDK backlog,
  which is this module's primary product.

The payoff is that Assay becomes a design-time tool as well as an audit: *is this card expressible in
canonical Magic templating, and what exactly does it say?* — answered mechanically, with generated
Oracle text as the by-product.

## The compiler (`compile/`) and the Scenario Builder sandbox

`CardCompiler` is that design-time tool with the last step taken: the answer to "what exactly does it
say?" is a `CardDefinition`, and the Scenario Builder plays it. `just assay compile --file card.json`
is the same path from the command line, and a custom card takes it without a corpus at all.

Rules for working on it:

- **Fail-closed is the whole design.** A card compiles only if normalization holds, *every* line is
  `ROUND_TRIP` or `VARIANT`, the fragments fold into one card, the printed header parses, and
  `CardValidator` passes. Each failure is a named `CompileDecline` carrying the line that caused it.
  Loosening any of these to make more cards playable inverts what the module is for — the decline is
  the product, and a card that compiled with an ability missing would look right on the board.
- **The reader is shared, not copied.** Pasted JSON goes through `corpus/ScryfallJson.kt`, the same
  function the bulk file streams through. A second reader would mean the gates no longer say
  anything about what the builder plays.
- **The header is the compiler's business; the text is the grammar's.** Power/toughness, loyalty and
  defense live on `OracleFace` and only this package reads them. A `*` power declines — mapping a
  characteristic-defining ability into the stat slot is grammar work nobody has done, and reading it
  as 0 is the reversible-but-wrong class in one line.
- **Ability ids are re-minted here and nowhere else.** The grammar mints a fixed constant per family
  because no printed word determines an id and the differential normalizes by position. A *played*
  card cannot share ids — activation dispatches on them — so the compiler assigns fresh ones. That
  asymmetry is deliberate; do not "fix" either half to match the other.

## The explorer (`explore/`)

`just assay-explore` is the browser UI. Three rules keep it from becoming a second, drifting
implementation of the thing it displays.

**It is a view, never a second source of truth.** Every number it shows comes out of
`FinenessReport`, `Touchstone` or `Differential` — the same objects the CLI renders as text. If the
explorer needs a number the gates do not produce, the number goes in the *gate* and both read it.
An explorer that computed its own fineness would make two reports that can disagree, and the one
people look at would be the one nobody gates on. The decline rankings are the worked example of the
rule being applied rather than argued about: the keying (`gate/DeclineKey.kt`), the family counts and
the sole-blocked number all live in `FinenessReport`, so `assay report --rank tail` and the page are
one ranking. What stays in `AssayIndex` is the *link table* — which card names, a dozen example
lines, the hand-written split — which is not a number and would make a corpus run's memory grow with
the corpus for a question no gate asks.

**It calls the live grammar; it never precomputes a payload.** The mtgish model explorer this is
modelled on had to embed its data and ship the parser as WebAssembly, because the parser lived in
another repository. Ours is on the classpath. Do not "optimize" a view by baking a snapshot into a
resource — the whole value is that a rule you just edited is one restart away from being re-measured.

**No new production dependency, and no path back to being a loader.** `com.sun.net.httpserver` is in
the JDK; that is why the SDK-only rule still holds, and it is the constraint to check before reaching
for a framework. `ExploreServer` binds loopback only and holds no state a request can mutate.

**Two transports, one explorer — and the split is by transport, not by feature.** `game-server`
mounts the same page and the same routes under `/api/assay/explorer` so the web client's Set
Completion view can frame the live tool as a tab. That is only safe while neither server *decides*
anything: `explore/ExploreApi.kt` owns the sweep, the caches and every route's behaviour, and both
`ExploreServer` and the Spring controller are reduced to moving bytes. If you find yourself writing a
`when` on a route name in a transport, it belongs in `ExploreApi`. The page's request prefix is a
serve-time substitution (`ExploreApi.page(apiBase)` replaces `%%ASSAY_API_BASE%%`) for the same
reason — a hard-coded `/api/` would have forced the second server to proxy or the page to fork.

The embedded tab is **not** gated, and the reason is worth stating so nobody "fixes" it: the explorer
is a read over public card text with no state a request can mutate, so the cost of mounting it is
resources rather than exposure. Those are handled where they arise — the sweep is lazy, so a server
nobody opens the tool on pays nothing; a sweep that cannot run leaves the page usable and says so;
the differential degrades to its "no goldens" message off a bootJar. What this does *not* license is
treating the explorer as production infrastructure: it still wants a 24 MB corpus and a warm index,
which is why the coverage badges read the baked ledger below instead of asking it.

Two supporting pieces have their own reasons:

- **`Phrase.shape`** is read-only structural introspection on the kernel, and it is walked from
  `Grammar.abilityLine` rather than assembled from each family's published rule list. Walking from
  the root can only show rules that are *wired*; a hand-maintained index would also show a family
  written but reached from nothing. It is never consulted by `parseHere` or `unparse`, which is what
  makes it safe to have there at all — a new combinator should override it, and nothing should ever
  branch on it inside `grammar/`.
- **Rule usage numbers come from the print side.** The kernel records no parse provenance, but
  `oneOf` prints through the first canonical alternative that can express a value, so "which rule
  printed this" is exact. Do not replace it with a re-match to attribute a number: that would be a
  second, unverified implementation of the half the round trip depends on.

## The verdict ledger (`bake/`)

`just assay-bake` writes one sorted line per card — read-whole, or the decline that stopped it — to
`game-server/src/main/resources/coverage/assay-verdicts.json`. It is the one place this module
*does* bake a snapshot, and the exception is principled rather than convenient: it answers a
question for a machine that cannot run the grammar. The production `game-server` is a bare JRE with
no `~/.cache/scryfall`, so the Set Completion view's "Assay already reads this card" badge has no
live path to an answer, exactly as the coverage denominator has none and is baked by
`scripts/gen-set-totals`. **This does not weaken the explorer's rule.** The explorer must stay live
because its value is re-measuring an edited rule; the ledger is read by a server that has no grammar
on hand at all.

Three things to preserve when touching it:

- **It is also the regression ledger this file has been asking for.** "Regressions need a diff, not a
  total" describes exactly this artifact — one sorted line per card, re-blessed deliberately like the
  card goldens — so a re-bake's `git diff` *is* the list of cards whose reading changed. That is why
  `write` frames the JSON by hand instead of using `prettyPrint` (which would spread each card over
  five lines and destroy the property), and why the bake is **not** wired into the build: an
  auto-regenerated ledger erases the only signal that made it worth committing. A stale one degrades
  into an out-of-date badge, which is the cheaper failure.
- **It answers with `CardCompiler`, not with line verdicts.** "Could this be implemented using Assay"
  is that object's exact question, and it is stricter than the line verdicts: a card whose every line
  round-trips can still fail on a `*` power, a second face, or `CardValidator`. The explorer's
  "cards fully covered" is therefore legitimately a little higher than the ledger's `whole`, and the
  two are not meant to match.
- **A bulk run is the first thing that hands the compiler every card, so it finds what nothing else
  does.** The first bake crashed on a negative printed power — `CreatureStats` enforces a
  non-negative base with `require`, and `CardCompiler` handed it `-1`. Both halves of the fix are the
  module's own rules applied: negative P/T is now a `HEADER` decline naming the value (an SDK finding
  reported like every other one), and constructing the definition is guarded so any *other* model
  invariant becomes an `INVALID_CARD` decline. A compiler that throws breaks "declining is success"
  for every caller — including the Scenario Builder's paste box, which would have answered a 500.

## Style

The KDoc in this module is unusually dense on purpose: **every non-obvious rule states why it is not
simpler.** That is the convention, not decoration — the alternative is a future agent "cleaning up" a
deliberate asymmetry (the singular/plural split, the enumerated type list, the omitted
`EachOpponent` spelling) and reintroducing an ambiguity the corpus took a full run to surface. If you
cannot write that paragraph, the rule is probably wrong.

Otherwise: pure functions, immutable data, no reflection, no mutable global state, `object` per
topic, rules as `val`s. Nothing in `grammar/` should need a comment explaining *what* it does.

## Commands

Run through `just`, never raw `./gradlew` — the recipes hold the machine-global build semaphore.

```bash
just assay parse "Serra Angel"      # normalized lines + the SDK model each parses to
just assay explain "Wall of Omens"  # the same, with a caret on the token a decline died on
just assay-gate                     # touchstone over the corpus; exit 1 on a bug
just assay-gate --limit 2000        # fast smoke run while iterating
just assay-report --top 40          # the same numbers, always exit 0 — the SDK gap report
just assay-report --rank tail       # …keyed on the parse's tail, with the sole-blocked count
just assay-report --rank tail --tail-words 4   # re-measure the tail's one design parameter
just assay-differential             # Assay's readings vs. the hand-written cards
just assay-explore                  # all of the above in a browser, on the live grammar
just assay-bake                     # re-bless the whole-card verdict ledger (own commit; read the diff)
```

Unit tests are Kotest string-spec under `src/test/kotlin/com/wingedsheep/assay/`, named for the
property they assert rather than the method they call.
