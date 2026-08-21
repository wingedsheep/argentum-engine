# `:oracle-assay` — Argentum Assay

A first-party Oracle-text parser: Scryfall JSON in, `mtg-sdk` models out, with a grammar where every
rule is written in both directions so it proves itself against the whole corpus without a human
reading the output.

Design: [`docs/oracle-assay.md`](../docs/oracle-assay.md) · Build order:
[`docs/plans/oracle-assay.md`](../docs/plans/oracle-assay.md)

**Phase 1 is implemented: the kernel, invertible normalization, the touchstone gate, and a grammar
covering vanilla cards and keyword-only abilities. The differential gate is live** over the class the
grammar reads whole, and the first band of the pipeline family is in — cardinals, a filter and target
vocabulary, the one-verb spell effects (draw, destroy, exile, tap, untap, return to hand), the counted
verbs (life, scry, surveil, damage, pump) and the **trigger prefix** — the event triggers and the step
triggers ("When ~ enters, …", "At the beginning of your upkeep, …"), which is where the differential
started comparing whole *abilities* rather than keyword lists, and where it found its first bug in a
hand-written card. The **land band** followed — mana abilities and "~ enters tapped." — the first
family where whole-card coverage moved with line coverage, because a land is a one-to-three-line card
and those are the two sentences on it. The **aura band** followed: `Enchant <filter>` and the
attached-permanent statics, which opened `staticAbilities` — the largest `CardScript` slot the
differential could not see into, and the one every later static family lands in.

The most recent work is the **target quantifier** — "Destroy **up to one** target creature.", "Exile
**up to three** target creatures.", "Destroy **up to X** target artifacts." (**+75 whole cards**), the
family the ranking had been naming from five directions at once. Everything English prints in front
of the word "target" is now a **six-row table** rather than a word inside each verb's template, and
the reason it is a table and not a slot is the noun behind it: "up to one target **creature**" and "up
to two target **creatures**" disagree in number, so a quantifier that could be slotted would leave the
noun's number undetermined. What the table buys is the thing the old hand-copied shapes had already
lost — "tap up to three target creatures" was written and "destroy up to three target creatures" was
not, on a grammar that read both halves of that sentence. Six families slot it — four of them
sentences that never had a quantifier before, two of those taking only the rows whose plural is a
plural *noun* rather than a different sentence — and the ranking's "up to one …" row fell from
**174 cards to 17**.
See [the target quantifier](#the-target-quantifier).

Before it came the **trigger join** — "When ~ enters **and** whenever you cast a spell with
mana value 5 or greater, draw a card." (**+6 whole cards**), the top row of the tail ranking and the
band whose *measurement* was the finding. Two `when` clauses, one payoff, two abilities: the same
model [the entry band](#the-entry-band)'s five-row table produces, reached the opposite way, because
here each half repeats its own trigger word and so is a complete clause drawn from the same
vocabulary. So the prefix became a **value** — one `Prefix` per printed `when` clause, one
`sentence(prefix)` per trigger rule, and one alternation the join slots twice — and every trigger
family the grammar learns is a legal half of it without being told. What that split also moved is
where a *decline* lands: the ranking's number-one family, 177 cards on a prefix the grammar could
already read, was an artifact of the whole clause being one template literal, and it dissolved.
See [the trigger join](#the-trigger-join).

Before it came the **step-trigger band** — "At the beginning of **each opponent's end
step**, …" (**+3 whole cards**, and the "At the beginning …" decline family from 197 cards to 20).
`dsl.Triggers.phase(step, player, binding)` is the SDK's one language for a step trigger and the
grammar was calling its frozen constants — thirteen whole-prefix rules, one per printed sentence — so
[`Phases`](src/main/kotlin/com/wingedsheep/assay/grammar/Phases.kt) makes it the product it already
was: a step noun, a whose-turn layer that is `Player.possessive` rather than a table copied here, and
the three frames English prints. Its number is the small one on purpose. A family at the *front* of
its line measures its payload, and the ranking now names that payload — "sacrifice ~." as a bare step
leads the table it inherited, with the triggering player as a subject and the delayed trigger behind
it. See [the step-trigger band](#the-step-trigger-band).

Before it came the **entry band** — "When ~ enters …" and "As ~ enters, …", the tail
ranking's top two families, taken together because they are the two halves of one moment
(**+26 whole cards**). Its two pieces are the same lesson read off two different SDK types. The
trigger half is a *join*: Oracle writes "When this creature enters **or** dies, …" and the SDK writes
two abilities, so the existing `pairedTriggerRule` — one rule, for "attacks or blocks" — became a
measured five-row table plus two older spellings, and "~ enters or …" fell from 183 blocked cards to
**21**. The replacement half is a *product*: `EntersWithChoice` calls itself "a single parameterized
type" and the grammar was calling it with every parameter frozen at two of them, so eight noun
phrases, a chooser slot and a bounded number replaced two constants. The band's own finding is the
one it deliberately did not fold — `EventPattern.AnyOf` is a second SDK spelling of the join that
three cards use and sixty do not. See [the entry band](#the-entry-band).

Before it came the **batch-trigger band** — CR 603.2c's "one or more" (**+30 whole cards**),
which was the decline table's number-one family by every column: 355 cards blocked, 140 of them
solely, 359 lines. Its lesson is about what a band is *worth*: the probe cut the headline to 89 lines
and 35 cards, because what follows a batch trigger is overwhelmingly "that many", "them" and "those
creatures" — the batch **collection**, which is the band this one identified and did not write. Its
own machinery is two things: a third instantiation of the `Filters` cascade, because these events
fold an absent controller predicate to "you control" and so the controller clause belongs to the
sentence; and the "This ability triggers only once each turn." rider, one rule on the *ability* that
reaches every trigger family and immediately exposed five card bugs. See
[the batch-trigger band](#the-batch-trigger-band).

Alongside it, one row further down the same ranking, came the **fronted duration** — "Until end of turn, target creature gets +3/+3."
(**+4 whole cards**), and it is the band whose *measurement* is the product. It is the second family
on the tail ranking, 265 cards blocked and 189 sole-blocked, and moving the duration to the back of
its own 266 declined lines finishes **five** of them: everything behind that opening clause is a
construct nobody has written yet. So what shipped is the position — one kernel capability
(`PhraseBuilder.alsoSpelled`), one derivation ([`Durations`](src/main/kotlin/com/wingedsheep/assay/grammar/Durations.kt)),
and one line on each of thirteen durational rules — and the four bands the measurement named are the
work. See [the fronted duration](#the-fronted-duration). The two bands are worth reading together:
each is a top-of-the-ranking family whose probe said the payload behind it, not the family itself,
is where the work is.

Before them came the **prevention band** — "Prevent all combat damage that would be dealt this
turn." and everything the same SDK type can say (**+75 whole cards**). It is the top-of-library
band's lesson on an `Effect` rather than on a `Patterns` recipe: `PreventDamageEffect` calls itself
"a single parametrized type that can express any combination" of six fields, and the grammar was
holding two whole-sentence rules whose own KDoc said they "share nothing but the verb". They share
every word except the ones the fields name, so the family is their product — a quantifier, a kind of
damage, three clause frames, a recipient vocabulary, and a source *layer* that owns one field and
strips exactly it. See [the prevention band](#the-prevention-band); it also found the whole-line
probe overstating by a third for a new reason, and its own redundancy report found the rule it had
just generalized.

And before that the **conditional-tapped-entry band** — the lands that enter tapped *unless*
something is true (**+57 whole cards**), which is the same lesson again with a twist worth
remembering: the rule had been written off in `Replacements`' own KDoc for want of a condition
vocabulary, and the spell-cost band had built one without anyone re-reading the write-off. See
[the conditional-tapped-entry band](#the-conditional-tapped-entry-band).

Before it came the **top-of-library band** — how many cards you see, what you keep, and
where the rest go (**+87 whole cards**). It is the cost band's lesson applied a third time, and the
first one where the grammar got *smaller*: `Patterns.Library` had published these sentences as
recipes whose parameters are exactly the words that vary, and the grammar was calling them with every
parameter frozen — so four whole-sentence rules became one three-layer vocabulary, and
"…and the rest on the bottom of your library in any order" stopped being a rule nobody had written.
It is also the band that put `SelectFromCollectionEffect` under the differential for the first time,
and every card it newly compared that disagreed was wrong: see [the top-of-library
band](#the-top-of-library-band) for the eleven, one of which had been drawing all four cards it was
told to look at.

Before it came the **counting band** — *how many*, as one vocabulary and the three places a
card puts it (**+109 whole cards**). It is the cost band's lesson applied to a second SDK type:
`DynamicAmount` is the one language for a number the game works out, and the grammar was holding a
three-row count plus seventeen bespoke clauses restating one verb each. It is also the first band
whose reading lands **outside `CardScript`** — a characteristic-defining ability is the value behind
the printed `*`, so the fragment grew a stat slot and the compiler is where the header's star and the
text's definition finally meet. See [the counting band](#the-counting-band); it found six card bugs,
one of them a Revenant counting the battlefield where its text says graveyard.

Before it came the **modal band** — "Choose one —" and the rows under it (**+137 whole
cards**). It is the first band whose main change is *outside* `grammar/`: a bullet is a continuation
of the line above it, so a modal card's rows are one ability rather than four, and that is
normalization's job — the one join whose inverse is free, because the joined line carries its own
newlines. The grammar half is small and multiplicative: a modal block is a **clause position** rather
than a line, so the 204 cards that print the header inside a trigger or after an activation cost cost
nothing beyond the 446 that print it alone, and a *mode* is one whole sentence from the enclosing
cascade. See [the modal band](#the-modal-band) below; it found eight card bugs — two faking "one or
both" as a third mode, one board wipe destroying one permanent at a time — and all eight are fixed in
the same change.

Beside it came the **spell-cost band** — what a spell costs, and what changes it. It is
the first band picked by the probe *against* both other rankings agreeing, and it delivered
**+126 whole cards** from one sentence read as three vocabularies: whose spells,
by how much, and under what clause. See [the spell-cost band](#the-spell-cost-band); it also closed
a reversible-but-wrong reading of every intervening-if whose consequence is more than one clause,
and it leaves the largest single SDK finding this module has produced — `CostReductionSource`'s five
`FixedIf…` cases restate `CostGating.OnlyIf`, and the corpus is split down the middle between them.

The two were written in parallel off the same 7,451 and compose without overlapping: **7,714 cards**
read whole between them.

Before it came the **cost band** — what you pay, everywhere you pay it. It is the largest
single delivery a family has made here (**+274 whole cards**, 7,177 → 7,451) and the first one that
needed no new grammar *machinery* at all, only a refactoring: `CostAtom`'s own KDoc calls itself
"the one cost language", and the grammar now reads it that way round — one `Phrase<CostAtom>`
vocabulary lifted into an activated ability's cost and into a spell's additional cost, instead of
two vocabularies over the same English. See [the cost band](#the-cost-band) below; it also found
three hand-written cards whose noun phrases were wrong inside a cost, where nothing had ever looked.

Before that came the **spell-cast band** — "Whenever you cast a noncreature spell, …" — which
gives the grammar its first noun phrase for a *spell* rather than a permanent, and was the largest
single family left in the corpus by the honest ranking: 504 cards declined on nothing but a
spell-cast trigger, against 263 for the next one. See [the spell-cast band](#the-spell-cast-band)
below; it is also where the ranking method itself changed, from the token a line died on to the
**tail** the parse could not read. Then the **Bloomburrow band** — the first set picked *because*
its cards are already implemented, so every decline was a grammar gap with a written answer.

Before it came the **counters band**, the first one picked by *ranking the backlog* rather than by
picking a set: `just assay-report --implemented` said 656 cards with a hand-written golden decline
on nothing but a counter sentence, the largest sole-blocked family in that population. See
[the counters band](#the-counters-band) below.

Before that came the **Legions band**, the second set read end to end and the first *hard* one: `just assay-gate --set LGN` reads **145 of Legions' 145 cards**. Legions is every-card-a-
creature, so it is a set made almost entirely of the things Portal had none of — morph payoffs,
tribal lords, granted abilities, amplify, counted variables — and reading all of it took nine new
families rather than nine new rules. See [the Legions band](#the-legions-band) below.

Before it came the **Portal band**, the first one measured against a whole *set* rather than against
a rule family: `just assay-gate --set POR` reads **200 of Portal's 200 cards** end to end. Getting there was not two hundred rules — it was the machinery a set needs before any of its
cards can be read whole. **A line is a sequence of clauses**, so a card printing two sentences reuses
the effect vocabulary twice instead of restating it capitalized; **a noun phrase is a layered cascade
in two grammatical numbers**, so "black creatures you control" and "creatures with power 2 or
greater" are compositions rather than rules; **two anaphors** ("that creature", "it") are separate
vocabularies because they point at different objects; and **three restriction vocabularies** —
casting, activation, additional costs — reach the `CardScript` slots that say when a card may be
played rather than what it does. Nearly everything else was rows in those lists. Whole-corpus
coverage went from 3,004 cards to 4,287 in the same change, which is the argument for picking a set
as the target rather than picking the number.

Nothing here changes `:mtgish-tooling`, which stays authoritative until a per-set cutover replaces it
(Phase 5). Assay is **not a runtime card loader** and never will be — with one carved-out exception,
the [custom-card sandbox](#the-compiler-and-the-custom-card-sandbox), which compiles a *pasted* card
for a dev-gated Scenario Builder session and never touches the corpus.

## Commands

```bash
just assay parse "Serra Angel"      # normalized lines + the SDK model each parses to
just assay explain "Wall of Omens"  # the same, with a caret on the token a decline died on
just assay compile "Serra Angel"    # the reading as a whole CardDefinition (JSON on stdout)
just assay compile --file card.json # …from a pasted Scryfall object — the custom-card path
just assay-gate                     # the touchstone over the whole corpus; exit 1 on a bug
just assay-report --top 40          # the same numbers, always exit 0
just assay-report --scope           # restricted to Phase 1's own target class
just assay-report --implemented     # restricted to cards that already have a golden — the *grammar* backlog
just assay-report --set POR         # restricted to one set — every card *printed* in it
just assay-report --rank tail       # declines keyed on the parse's tail, with the sole-blocked count
just assay-differential             # Assay's readings vs. the hand-written cards
just assay-explore                  # all of the above in a browser, on the live grammar
just assay-bake                     # re-bless the whole-card verdict ledger (see below)
just assay corpus --refresh         # re-download the Scryfall Oracle bulk (~24 MB, cached 7 days)
```

The corpus is Scryfall's `oracle_cards` bulk file, cached at
`~/.cache/scryfall/_bulk-oracle-cards.jsonl.gz` — the same directory `scripts/card-status` and
`:mtgish-tooling` use, under a `_bulk-` prefix that cannot collide with a set code. Scryfall serves
it as gzipped JSONL, so it streams a card at a time.

**`--set` is membership, and it has to be.** One object per Oracle ID means each card carries a
single *representative* printing, so its `set` field says which printing Scryfall shows it under —
not which sets it was printed in. Filtering on that field showed **53 of Portal's 200 cards**: Blaze
is shown as `bbd`, Raise Dead as `w17`, Wild Griffin as `cn2`, and Portal's own reprints of older
cards are credited to the older set, so a set loses cards in both directions. `--set` and the
explorer's set box therefore ask Scryfall for the set's card list and join on Oracle ID
(`corpus/SetMembership.kt`), cached per set at `~/.cache/scryfall/_setlist-<code>.tsv`. Per set
rather than one global index because the filter is always one set at a time — a `default_cards`
Oracle-ID → sets map costs a 77 MB download to answer a ~200 KB question. A set that cannot be
resolved matches **nothing** and says so; degrading to the whole corpus would be a report lying about
its own population. The Set *column* still shows the representative printing, and is labelled as such.

## Where things are

```
syntax/     Phrase kernel — templates, slots, both directions, memoization, the parse cap
normalize/  Scryfall text -> canonical ability lines, every pass with its inverse; reminder glosses
corpus/     the Scryfall Oracle bulk: download, cache, stream; per-set membership for `--set`
grammar/    the rules, by topic — Primitives, Keywords, Cardinals, Conditions, Filters, Spells,
            Targets, Steps, Continuations, Triggers, Mana, Costs, Activated, Replacements, Statics,
            Restrictions, and the effect-topic files Library, Hand, Combat, Graveyard, Stack,
            SelfSteps
            Filters is the noun phrase for a permanent and Spells the one for a spell — same
            GameObjectFilter, different head noun, disjoint positions
            Steps is the clause vocabulary and the sentence/sequence machinery every other file
            slots into; Activated is the cost-colon-effect sentence; Statics is the continuous-
            ability slot; Restrictions is the three "when may this happen" vocabularies;
            Continuations and SelfSteps are the two anaphors ("that creature" / "it")
gate/       the touchstone, the fineness report, the differential
compile/    a whole reading as a CardDefinition — the custom-card sandbox's engine
explore/    the browser UI — a loopback HTTP server over the live grammar and both gates
cli/        assay parse | explain | compile | gate | report | differential | explore | corpus
```

## The three things that make this different

**The grammar runs backwards.** A rule registers `build` (text→model) and `match` (model→text)
together, and a canonical rule *cannot be constructed* with only one of them. That buys the gate:

```
print(parse(normalize(t))) == normalize(t)      // or: declined, and counted
```

**There is no Assay IR.** Rules parse straight into `mtg-sdk` types through the SDK's own
companion factories. So "can Argentum express this?" collapses into "did it parse?", and a decline
names a missing capability in Argentum's own vocabulary rather than in a third ontology's.

**Declining is success.** Unparseable text is counted and ranked by cards blocked, never
approximated. The bottom of the fineness report is a continuously-updated SDK gap report.

## Reading the verdicts

| Verdict | Meaning |
|---|---|
| `ROUND_TRIP` | Printed back byte-for-byte. |
| `VARIANT` | An alternate spelling normalized to the canonical one; reparsing the printed line gives the identical model, so nothing was lost — only the spelling moved. |
| `MISMATCH` | Printed something the grammar does not read back the same way. **A bug. Must be 0.** |
| `AMBIGUOUS` | Two rules, two different models, one text. **A bug. Must be 0.** Never resolved by picking one. |
| `DECLINED` | Not covered. Counted, ranked, and named — not a bug. |

`MISMATCH`, `AMBIGUOUS`, and a non-invertible normalization pass are what `just assay-gate` exits
non-zero on. Declines are not failures.

## Phase 1 results (whole corpus, 2026-08)

```
Cards assayed                    34882
Ability lines                    64753  (37998 unique)

Round-trips byte-exact           26264   405.6‰ (40.6%)
Alternate spelling normalized    1436
Declined                         37053
Ambiguous — distinct readings    0
Print mismatch                   0
Normalization not invertible     0
Full inverse not reproduced      0
Redundant readings (same model)  0

Cards fully covered              8102 / 34882   232.3‰ (23.2%)
Vanilla + keyword-only cards     1444 / 1712   843.5‰ (84.3%)   <- Phase 1 target
Portal (set POR)                 200 / 200     1000.0‰ (100%)   <- the Portal band's target
Legions (set LGN)                145 / 145     1000.0‰ (100%)   <- the Legions band's target
Bloomburrow (set BLB)            60 / 280      214.3‰ (21.4%)   <- the Bloomburrow band, in progress
Reminder-text glosses            2870 matched · 114 differed · 965 unglossed
```

Fineness is **parts per thousand**, per the assay the module is named for — 841.1‰ is 84.1%.

The **line count fell** with the modal band and that is not a regression: a card printing
"Choose one —" and three bullets used to be four lines, and it is one ability. Normalization now
keeps the rows together, so 2,040 rows stopped being counted as abilities of their own. Percentages
against that denominator are therefore not comparable across the change; the whole-card number is.

The machinery holds: **zero** ambiguities, print mismatches, or non-invertible normalizations
across 64,753 ability lines. The 84.1% on Phase 1's own target class is not the round trip
faltering — every remaining line in that class declines because the SDK has no vocabulary for the
keyword, which `just assay-report --scope` lists in rank order.

**A whole set at 1000‰ is worth more than the percentage it moved.** A rule family's coverage number
says how much of one shape the grammar reads; a *set* is an arbitrary cross-section of Magic, so
reading all of one means the grammar has no systematic hole in that era rather than no hole in that
family. Portal is a deliberately simple set, which is what makes it the right first one — and the
318 alternate spellings above are mostly its doing, because a card printing "A and B" or "A, then B"
now reads correctly and prints back as the full-stop form.

## The Legions band

Legions is 145 cards and every one of them is a creature, which makes it the opposite of Portal in
exactly the way that is useful: Portal proved the grammar could read a set of *spells* with simple
sentences, and Legions proves it can read a set whose sentences are all about permanents that grant,
count, transform and tax. Reading all of it needed nine pieces of machinery and, after those, rows.

**Subtypes, and the case problem they exposed.** A tribal set is built out of one noun phrase —
"Sliver creature", "non-Zombie creature", "Bird and/or Cleric permanent" — so `Filters` grew a
subtype layer, its negation and its disjunction. That immediately hit something the grammar had
never met: a subtype is a **proper noun**, and `SentenceCase` lowercases every sentence start
before the grammar sees it. "Sliver creatures get +1/+0." is a whole line and
"{T}, Sacrifice a Goblin: Goblin creatures get +2/+0 …" starts a second sentence after the colon, so
undoing the lowercasing at the text boundary would mean guessing which of a line's sentence starts
were proper nouns. It belongs to the leaf instead: `Primitives.subtype` reads the lowercased
spelling, and **only for a word the SDK names as a type**, so nothing is guessed and no common noun
acquires a second reading.

That gate is the whole rule, and the differential proved why. An earlier attempt retried the line
*as printed* when the ordinary reading declined — which reads position 0 with its capital intact, and
therefore reads "**Other** creatures you control get +0/+1." as creatures of a type called *Other*.
Byte-perfect in both directions, and about a tribe Magic does not have. It bought a few dozen cards
whose first word is a subtype nobody published a list for; declining those is the better half of that
trade, and "Other" is now a real prefix on the lord rules instead.

**Costs are an atom vocabulary.** `{2}{B}, {T}, Sacrifice a Goblin` is three costs, so `Costs`
became a list of atoms plus one comma-joined run — and `{1}, {T}`, which used to be a rule, stopped
being one. A cost is also the single clause Oracle capitalizes that is **not** a sentence start:
"Sacrifice a Goblin: …" is lowercased by the case pass at a line start and left alone after a comma,
so each verb atom is instantiated twice from one template and only the capitalized instance prints.

**Lords, and grants in both of their printed shapes.** "Sliver creatures get +1/+0." is one shape
with three members — a stat modifier, a keyword, or a whole quoted activated ability — over a group
`Filters` names. The quoted grant slots `Activated` unchanged, which is what makes
"All Slivers have "{T}: Regenerate target Sliver."" a row rather than a rule. The *unquoted* grant is
its twin: "Whenever a Sliver deals combat damage to a player, its controller may draw a card." is a
`GrantTriggeredAbility` whose noun lands on the grant and never on the event, and whose subject is
"its controller" because the sentence is written from the granting card's point of view rather than
the gaining permanent's. Those third-person clauses are reachable **only** from a grant, and that is
deliberate: outside one, "its controller" means the controller of whatever the sentence was just
talking about, so a global spelling would misread "Whenever a creature dies, its controller loses
1 life" as a sentence about you.

**Counted variables.** "…gets +X/+X until end of turn, where X is the number of Elves on the
battlefield" defines its number in a trailing clause, "…equal to the number of +1/+1 counters on it"
in a prepositional one, and "…for each +1/+1 counter on it" in a third. All three put a
`DynamicAmount` where a numeral would go, so `Amounts` is one file with a count vocabulary and the
verbs that slot it.

**Morph.** The keyword was already a `Keywords` row; the band added the trigger it pays off
("When ~ is turned face up, …", 27 of the set's cards) and the effects that turn other permanents
over. Amplify came with it, and it is the one line in the grammar whose two halves land in **two
different card slots**: the SDK spells it as a bare `Keyword.AMPLIFY` plus an
`EntersWithRevealCounters` carrying the printed number, so the line denotes a keyword *and* a
replacement effect and there is no `Numeric(AMPLIFY, n)` anywhere in the corpus.

**Two smaller pieces with wide reach.** A trigger line can denote **several** abilities, because
"Whenever ~ attacks or blocks" is two events with one payoff. And a sequence's target is declared at
its first mention, which is not always the *first* clause — Fleshformer prints "~ gets +2/+2 and
gains fear until end of turn. Target creature gets -2/-2 until end of turn.", where the introducing
clause is second. `Steps` now finds the owning clause by printability rather than assuming index 0,
and at most one position can satisfy it, so the split stays deterministic.

Whole-corpus coverage went from 4,287 cards to 6,157 in the same change, and the differential's
compared population from 1,636 to 2,387 — which is again the argument for picking a set as the target
rather than picking the number.

## The counters band

Four sentences — "Put a +1/+1 counter on target creature.", the same clause aimed at the source
("…on ~.") and at the target an earlier clause chose ("…on it."), and the entry replacement
"~ enters with two +1/+1 counters on it." Whole-corpus coverage went 6,157 → **6,335 cards**, the
differential's compared population 2,387 → **2,431**, and its confirmed count 2,383 → **2,425**.

**It is the first band chosen by ranking the backlog rather than by picking a set, and the ranking is
the part worth reading.** The token table's top row was `you` at 595 implemented cards — a trigger
*subject*, which the step triggers already proved over-promises: 410 cards declined on "At the
beginning of…" and adding every prefix moved whole-card coverage by 23, because a line dies on its
first unknown token and a trigger's real blocker is usually after the comma. So the ranking that
decides work is **cards whose line dies at the verb**, where everything before it already read, and
by that measure counters are the largest family in the corpus: **1,025 implemented cards carry a
counter line the grammar could not read, and 656 of them decline on nothing else**. The verb also multiplies rather than adds —
`Triggers`, `Activated` and the modal rules all slot `Steps.step` whole, so one clause vocabulary
arrives in every context already wired. That is the opposite trade from a prefix, and the two
worked examples now sit either side of it.

**Two leaves, and both of them own a spelling no rule above them can see.** `Primitives.counterKind`
reads the noun and is gated on `CounterType.fromName` — the SDK's own answer to "is this a counter",
the same function `StatePredicate.HasCounter` parses with — because the model field is a bare
`String` and an ungated leaf would read *any* word as a kind and round-trip a counter Magic does not
have. That is `creatureSubtype`'s argument, and the "Elves" → `Elve` failure it exists to prevent.

The second leaf is the **indefinite article**, and it is inside the leaf for `statModifiers`' reason:
English picks "a" or "an" from the sound of the next word, so two rules — one per article — would
leave printing undetermined by the model, which is invariant 2 rather than a preference. It can be
one leaf because the corpus states the rule without an exception: across all 34,882 Oracle texts
**no counter kind is ever spelled both ways** — 223 kinds take "a", 38 take "an", disjoint. The
letter rule predicts all but three ("an hour", "an hourglass", "a unity"), only `hourglass` is a kind
the SDK names, and `token` re-reads what it writes on every call, so a wrong article could not
survive a corpus run.

**A two-word kind needed a lookahead, and the reason is a kernel property rather than a grammar
one.** "first strike" and "double strike" are counter kinds, and a leaf reads exactly *one* regex
match — `token` does not retry a shorter one when the gate rejects — so a greedy second word swallows
the template's own "counter" and declines every single-word kind. The noun's pattern therefore spells
out what it cannot be. Worth knowing before writing any leaf that can span a space.

**The band's real risk was the anaphor, and the machinery for it already existed.** "Put a +1/+1
counter on it" means the source in "Whenever ~ attacks, put a +1/+1 counter on it" and the *target*
in "Tap target creature an opponent controls and put a stun counter on it" — both readings round-trip
byte-perfectly, so nothing but the split could tell them apart. `SelfSteps.putCountersOnSelf` and
`Continuations.putCountersOnThatPermanent` are reachable from disjoint positions, exactly as
`SelfSteps.anaphoric` and `Continuations` have been since the differential caught "Untap target
creature. It gets +2/+4" meaning the wrong creature. This is the second sentence to need both, which
is the evidence that split generalized rather than patched one card.

**What the gate found: 44 newly-compared cards, 42 of them confirmed, and 2 divergences that are one
finding.** Landing on top of the divergence sweep is what makes that legible — against a baseline of
4 the band's own contribution is readable directly, where against 122 it would have been noise.

- **A card says "another" where its text says "an" — 8 cards.** Donatello, Way with Machines and
  Mm'menon, Uthros Exile both print "Whenever **an** artifact you control enters" and are authored
  with `binding = OTHER`, which excludes the source. The corpus maps the two spellings correctly
  almost everywhere — 33 cards print "an" and bind `ANY`, 40 print "another" and bind `OTHER` — so
  these are the exceptions rather than a convention, and a grep finds six more: Rimefire Torque,
  Airbender Ascension, Path of Discovery, Gossip's Talent, Death Match and Mana Echoes.
  **It is unobservable on all eight today**, and saying so is the honest half: every one of them is
  an enchantment or artifact triggering on a creature entering, so the source can never *be* the
  entering permanent and the binding never gets to matter. It is latent rather than harmless — an
  effect that makes Donatello an artifact as it enters is all it takes — and it is **not folded**,
  because folding would stop the gate noticing the first card where the binding is observable.

That is the whole of it: both new divergences are that one finding, and nothing else the band
brought into the population disagreed. A third — Invigorating Boon, the "you may on a triggered
ability" spelling — was divergent when the band was written against the pre-sweep baseline and is
not any more, because the sweep fixed that family while this was in flight.

**No new bug in a hand-written card**, and that is worth recording beside the aura band's same
result. Every bug this gate has found — Meteor Golem, Voltaic Construct, Dwarven Miner, Recollect
and Eternal Witness, and the 22 the sweep turned up — was a clause lost *inside a filter on a longer
sentence*. A counter sentence is short and has one filter, which is the same reason the auras added
none: there is very little in it to drop.

## The equipment band

`Equip {§}` was the **largest single sentence shape in the corpus** — 563 cards, against 342 for the
next one — and the last of Phase 1's two headline findings still declining. It is now read, together
with the two attached-permanent sentences an Equipment shares with an Aura. Whole-corpus coverage
went 6,335 → **6,400 cards**, byte-exact lines 22,944 → **23,861**, and the differential's compared
population 2,431 → **2,449**, every one of the 18 new cards confirmed.

**The band is two changes in two different files, and which half went where is the whole point.**

**The noun is normalization's.** An Equipment prints "Equipped creature gets +2/+0." for a model that
is *byte-identical* to an Aura's: the static's affected set is `GroupFilter.attachedCreature()` —
`Permanent` scoped to `AttachedTo` — which says "the thing this is attached to" and nothing about
auras, so Bonesplitter's golden and Holy Strength's carry the same `ModifyStats`. Which word a card
prints is a function of its type line, exactly like the self-reference noun, and the model has
nowhere to keep it. A second grammar rule would therefore have given one value two printed forms and
left `unparse` to choose — ambiguity by construction, which is invariant 2 rather than a preference.
So `Normalizer.canonicalizeAttachmentNoun` abstracts "equipped creature" onto "enchanted creature"
and restores the printed word positionally, and *every* static rule in `Statics` reads both card
classes without knowing there are two. `Statics`' KDoc predicted this pass before it existed; the
band carried the prediction out rather than revising it.

**The keyword is a line rule, because equip is lowered rather than stored.** "Equip {1}" is
`CardDefinition.equipCost` *plus* a synthesized activated ability carrying CR 702.6a's attach effect,
sorcery timing and target requirement. That is one printed line filling two slots in two different
objects, which is `Grammar.amplifyLine`'s shape and the reason `CardFragment` grew an `equipCost`
field — a fragment is the only place a line's two contributions can meet.

**The rule does not reproduce the lowering; it calls it.** `CardBuilder.equipAbility`'s body moved to
`ActivatedAbility.equip` in the same change, and both callers use it. A second copy in `grammar/`
would have agreed with the cards exactly until someone edited one of them, and the differential would
then have reported every Equipment in the corpus over a change nobody made to a card. It is the one
place the module's "build through an SDK facade" rule needed the facade to be *created* first, since
equip's curated surface was a DSL method a parser cannot call.

**`equipCost` is the first compared field outside `CardScript`.** Comparing only the ability would
confirm an Equipment that can never be equipped: `CardValidator` requires an Equipment type line
wherever the field is set and `CardLinter` reads it to decide whether a permanent can ever attach, so
a card carrying the ability without the cost is a different — and worse — card. The differential's
header now names it beside the script slots.

**The band is also the third worked example of a decline rank overstating its work**, and this time
the overstatement was measured in advance rather than after. Of a 400-card sample blocked by
`Equip {§}`, 248 declined on *nothing but* equipment-shaped lines — which predicted ~350 whole cards.
The actual figure is 65, and the gap is one word: the sample counted "Equipped creature gets +2/+2
and has trample **and** lifelink" as an equipment shape, and the grammar's joined sentence takes one
keyword rather than a run. So the prediction was right about which cards the band reaches and wrong
about which *lines* it finishes. The residue is now visible and small: 385 cards decline on nothing
but an "Enchanted creature …" sentence, and their tail is genuinely long — the largest single one is
10 cards ("doesn't untap during its controller's untap step"), and a keyword-run generalization of
the joined form is worth 22. There is no fourth large family hiding behind this one, which is the
useful half of the finding.

## The spell-cast band

"Whenever you cast a noncreature spell, …" — the spell-cast triggers, and with them the first noun
phrase the grammar has for a **spell** rather than a permanent. Whole-corpus coverage went
6,400 → **6,583 cards**, byte-exact lines 23,861 → **24,139**, and the differential's compared
population 2,449 → **2,495**.

**It is the second band picked by ranking the backlog, and the ranking method is the part that
changed.** The counters band ranked by "cards whose line dies at the verb"; this one ranks by *what
the grammar could not read* — every declined line is re-parsed, the parse's death offset taken, and
the **tail from that offset** is what the families are keyed on. That is the one key that neither
over- nor under-counts a prefix: a line that dies at "you cast" has already read "Whenever ", so the
family it names is the missing *event* rather than the word the report's table shows. By that
measure a spell-cast event is far and away the largest family in the corpus — **504 cards decline on
nothing but a spell-cast trigger**, against 263 for the next one, and 936 touch one.

**And it was measured before it was written, which is the habit the equipment band's overstatement
bought.** Substituting a known-good prefix ("When ~ enters, ") into all 712 of those cards' declined
lines and re-parsing says how many payoffs the grammar can already read: **252 of the lines and 234
whole cards**. The band delivered 183, and the residue is nameable rather than mysterious — the
`SpellCastPredicate` riders ("from your hand", "a kicked spell", "a spell that targets ~"), the
colour disjunction ("a blue or black spell"), "a colorless spell", and "your first spell during each
opponent's turn", which is not an each-turn ordinal at all. For comparison, the same measurement run
on modal spells — the family the token table and the shape table both rank first — says **126 whole
cards**, because a modal card is finished only when *every* one of its bullets reads.

**The whole band is rows plus one noun phrase, and no SDK change at all.** `SpellCastEvent`,
`NthSpellCastEvent` and `CastThisSpellEvent` were already modelled with curated facades in front of
them, so this is the cheapest thing the module can be doing: a grammar gap whose answer was already
written, and which the differential confirms the moment it parses.

- **The noun phrase is [`Spells`](src/main/kotlin/com/wingedsheep/assay/grammar/Spells.kt), and it is
  a family rather than rows in `Filters` because of the head noun.** A permanent phrase's head is the
  card type ("creature", "nonbasic land"); a spell phrase's head is the literal word "spell" with the
  card type in front of it as an adjective. `GameObjectFilter.Creature` is therefore printed
  "creature" in one file and "creature spell" in the other — one printed form per model in two
  disjoint positions, not two forms for one. The layers are a deliberate *subset* of `Filters`',
  because a spell has no controller, is never tapped or attacking, and has no power to compare: what
  is left is the three axes a card carries on the stack, its types, its colour and its mana value.
- **The subtype join is "or" here and "and/or" there, and that is not two spellings of one model.**
  `Filters.anySubtype`'s inner is a type noun, so the value it builds always carries a card-type
  predicate under the `Or`; this one never does. Deriving the join from the head noun would be a rule
  reading a templating habit instead of a model.
- **The caster is a field on the event, so "you" / "an opponent" / "a player" are three rows over one
  skeleton** — not a subject vocabulary in a slot, which would also let the rule print "each player
  casts", a sentence no card writes.
- **The effect clause is `Steps.triggeredStep`, for the filtered-trigger reason.** The event names an
  object of its own — the spell being cast — so "it" in the payoff is that spell, the third anaphor
  position exactly as a filtered enters-trigger is. "When you cast this spell" is the one row that
  takes the *source* cascade instead, and it has to: there the spell being cast **is** the source.
- **Widening `filteredTriggerRule` is what made the family rows.** Its `article: Boolean` became a
  noun-phrase parameter, so a cast trigger passes `Spells.indefinite` through the identical rule the
  enters and becomes-blocked triggers use, and nothing about the shape was copied.

**What the gate found: 46 newly-compared cards, and 4 divergences that are four different things** —
which is roughly the ratio the differential is supposed to have, and the first time a band has
produced one of each kind.

- **A parser bug of the reversible-but-wrong class — Storyteller Pixie.** The subtype layer read
  "an **Adventure** spell" as `Any.withSubtype(Adventure)`. The card is right and the grammar was
  wrong: CR 715.3 makes an Adventure spell one *cast as* an Adventure, which is what
  `SpellCastPredicate.CastAsAdventure` says in its own KDoc — "this is about how the card was cast,
  not what the card is" — and the same adventurer card cast as its creature half does not satisfy it.
  The reading round-tripped byte-perfectly and denoted a trigger the engine would never fire, which
  is precisely what the touchstone structurally cannot see. Fixed by spelling the phrase as a row of
  its own and having `Spells.spellSubtype` refuse the word, since one printed form with two models is
  ambiguity by construction rather than a preference.
- **A card bug of the bare-tribal-noun class — Adeliz, the Cinder Wind.** "Wizards you control get
  +1/+1" filtered on `Creature.withSubtype(Wizard)`; a bare tribal noun names every *permanent* with
  the subtype, which is the reading Zombie Master proves by printing both spellings on one card. It
  is the residue of the migration that closed that finding: Adeliz was not in the compared population
  then, and is now. Unobservable today, and fixed for the same reason the other 103 were.
- **A card bug of the "you may" class — Daring Archaeologist.** "You may return target artifact card
  from your graveyard to your hand" was spelled `optional = true` on the *target requirement*, which
  is the SDK's phrasing for "up to one target" — a strictly different ability, and exactly the
  conflation the trigger-`optional` collapse removed from the engine. The consent belongs on the
  ability. Fixed with the scenario test that asserts the observable halves: the requirement's minimum
  is 1, and declining asks for no target at all.
- **The standing `ManaColorSet.Specific` finding, recurring — Spider Manifestation.** "{T}: Add {R}
  or {G}." as one `AddManaOfChoiceEffect` where 165 cards write two abilities. The README's own note
  said none of the thirteen was compared "because each one's rider declines anyway"; this band read
  the rider. Read afterwards and classified a **card bug**: the note says `Specific` earns its place
  on the riders the two-ability form cannot express, and this line has no rider — the card was in the
  wrong group. Fixed to two abilities, with a scenario test. The finding below is unchanged and still
  not folded.

**Where the ranking points next.** On the same tail ranking, the row under `you cast` at 504 was
`When ~` at 263 and then a flat run — `enchanted creature` 183, `for each` 175, `Until end of` 170,
`Each player` 165 — and none of those is one sentence the way a cast trigger is. (Those counts are
the pre-band measurement and can only have risen, since a card blocked by two families was
sole-blocked by neither.) A flat tail is the shape the equipment band's residue had, and it is the
signal that the next target is a *set* rather than a family.

## The Bloomburrow band

Bloomburrow is 280 cards, every one of them implemented by hand here, which makes it the first set
picked *because* the goldens exist: every line it declines is a grammar gap whose known-good answer
is already written, and the differential confirms each one the moment it parses. The band took the
set 42 → **58 cards** and the whole corpus 6,583 → **7,177**, and it is four pieces of machinery plus
rows — the ratio the module's "cards covered per rule" curve is watching for.

**A granted keyword is a run, not a keyword.** `Keywords.keywordRun` spells "trample",
"lifelink and indestructible" and "trample, hexproof, and indestructible" as the list the model
actually holds, and every grant position slots it: to a target, to a group, to the source, to the
enchanted permanent. It *replaced* rules rather than adding them — `SelfSteps` carried a
two-keyword rule with no singular sibling, which is what a family looks like before it is one, and
`Statics.conditionalSelfStatic` collapsed a `pairForm` boolean into a three-member enum that also
gained the keyword-only sentence ("As long as you've lost life this turn, ~ has flying and
vigilance") for free. One grant is the bare effect and several are a composite, because that is what
`CompositeEffect` means and what every hand-written card holds; a rule that printed the singular as
a one-element composite would have disagreed with all of them.

**A token's count, colours and keywords are slots.** Six rows out of two axes — the count ("a",
a number word, "X") and the keyword rider — plus a colour *run* with `keywordRun`'s shape over
`Set<Color>`. The sets have no order and the printed sentence needs one, so colours print WUBRG and
keywords print in `Keyword`'s declaration order: both are `Color`/`Keyword`'s own, and a card that
built its set the other way round still prints the sentence Oracle prints. The predefined nouns
(Food, Treasure, Clue, Blood, Map, Lander, Shard) are a second family, each row calling the facade
the SDK publishes for it — with "investigate" deliberately left out, because CR 701.36a makes it the
same model as "create a Clue token" and two canonical spellings would leave printing undecided.

**An ability word is printed shape, and belongs to normalization.** CR 207.2c: *"they have no
special rules meaning and no individual entries in the Comprehensive Rules."* So `Landfall — `,
`Threshold — ` and `Valiant — ` come off the line, are recorded per line index, and go back on in
`restore` — the alternative being a grammar rule per ability word wrapping every sentence the grammar
already reads, which is the multiplicative cost "lift, don't re-spell" exists to refuse. The list is
**CR 207.2c's, verbatim, rather than a pattern**: CR 207.2d's *flavor* words have the identical
printed shape and are unbounded, and so is a Saga's `I —` and a Class's `Level 2 —`. Worth 106 cards
corpus-wide on its own, and it is what let the differential see the landfall bug below.

Then three rows: Valiant's trigger (one `TriggerSpec` the SDK already publishes), "deals N damage to
target opponent", and Threshold's graveyard count — which turned the hand-size comparison into a
two-member shape over (player, zone, direction).

**What it found.** Fifteen bugs in hand-written cards and one in `mtg-sdk`, every one surfaced by the
differential on the day a line stopped declining:

- **`Triggers.LandYouControlEnters` was `TriggerBinding.OTHER`** — one facade, 29 cards. No landfall
  ability prints "another"; the distinction is invisible on a creature and load-bearing on a *land*
  with a landfall trigger, which under `OTHER` would silently not see itself enter.
- **Five more bare-noun-is-permanents cards** — Kargan Dragonrider, Corsair Captain, Lathliss,
  Inside Source, Voice of the Woods, all `IsCreature` where the printed noun names only a subtype.
  The same class the bare-tribal-noun migration fixed; these were never comparable before.
- **Sunstar Expansionist read its intervening "if" as a resolution-time gate** — "When this creature
  enters, **if** an opponent controls more lands than you, …" is CR 603.4, checked when the trigger
  would go on the stack *and* again on resolution, not a `Gate.WhenCondition` that always triggers.
- **Seven BLB cards printed text the card does not have** — five keyword runs in the wrong order
  (Scryfall prints "Reach, vigilance"), Hunter's Talent on the pre-Bloomburrow "enters the
  battlefield" wording, and Quaketusk Boar with no `oracleText` at all.
- **Shocking Sharpshooter restated a documented default** — `damageSource = Self` is what `null`
  already means for a permanent's own triggered ability. 72 other cards carry the same redundancy
  and will surface as the grammar widens; it is *not* folded, because the field means something
  when it is set to anything else.

**Where the ranking points next, inside this set.** Gift is the largest family left by a wide margin
— **17 BLB cards are sole-blocked by it** — and it is two halves rather than one: the `Gift a card`
keyword line and the `If the gift was promised, …` rider, the second of which needs its base clause
to read first. After it: the Class levels (`{3}{U}: Level 2`, 10 cards), modal spells (8), and
"Spend this mana only to cast …" (6).

## The cost band

What you pay, everywhere you pay it. Whole-corpus coverage went 7,177 → **7,451 cards**, byte-exact
lines 24,993 → **25,377**, the differential's compared population 2,698 → **2,747** — and, being an
`mtg-sdk`-shaped band, it needed no new SDK type at all: every atom it reads was already in
`CostAtom`, waiting for a sentence.

**It is the largest band the tail ranking has produced, and the probe agreed with it before a line
was written.** Ranked by the parse's tail, the top rows are the modal bullets and a scatter of
trigger prefixes; the cost family does not appear as a row at all, because a cost is *the text before
a colon* and the tail keys on what came after. Ranking the colon lines directly — substituting `{T}: `
for every cost the grammar could not read and re-parsing — says **465 whole cards are blocked on
nothing but the cost clause**, against 104 for the modal band and 111 for cost reduction. A second
probe, greedy over the individual atoms, ranked the rows: "Discard a card" alone finishes 86 cards,
counter removal 49, the singular tap 35, "Sacrifice another" 35. The band delivered **274**, which is
the sum of exactly the rows that were written; the residue is named below and each piece of it is a
different band.

**One vocabulary, two contexts, because the SDK says so.** `CostAtom`'s own KDoc calls itself "the
one cost language": a payable thing is declared once and each *context* carries it through its own
`Atom` wrapper. Assay had it the other way round — `Costs` read a list of cost sentences for an
activated ability, and `Restrictions.additionalCostLine` was a separate rule that read
"sacrifice {filter}" and nothing else. Two vocabularies over one English. So `Costs.atoms` is now a
`Phrase<CostAtom>`, and the two contexts are two *lifts* of it. Adding "discard a card" to the
activation side gave "As an additional cost to cast this spell, discard a card." for free, and the
test for that property is one assertion in each of the two files.

**What is deliberately *not* an atom is the other half of the argument.** "Sacrifice ~", "Exile ~",
`{T}`, `{Q}`, "Exert ~" stay `AbilityCost` cases and are unreachable from the spell side — for the
reason `CostAtom` gives for keeping `excludeSelf` off it: *a spell being cast has no source
permanent*. That is a rule rather than a gap, and `RestrictionsTest` asserts the decline.

**Capitalization stopped being an `alternate` and became a parameter.** A cost atom is the one clause
Oracle capitalizes that is not a sentence start, so while costs lived in one position the lowercase
spelling could be an `alternate` — parseable, never printed. It no longer holds: in
"As an additional cost to cast this spell, **sacrifice** a creature." the lowercase form is
*canonical*. The vocabulary is therefore a function of its leading word's spelling, instantiated
twice, which is also what stops a row existing in one capitalization only.

**Three card bugs, all of the same kind, all invisible in play.** The differential went 0 → 3 the
first time the gate could read a cost's noun phrase, and every one of them was a hand-written card:

- **Wirewood Symbiote** and **Fungal Plots** spell "an Elf you control" and "two Saprolings" as
  `GameObjectFilter.Creature.withSubtype(…)`. A bare tribal noun means any *permanent* of that
  tribe — the same correction the 103-card migration made everywhere the grammar could already see,
  and these two survived it because their nouns were inside a **cost**, which nothing read until now.
  Unobservable today, since every printed Elf and Saproling is a creature.
- **Gene Pollinator** writes `Costs.TapAnotherPermanent()` for "Tap an untapped permanent you
  control" — a text with no "another" in it, and a filter left at the facade's unqualified default
  where the noun is "permanent". The `excludeSelf` restated what the co-paid `{T}` already
  guarantees.

**Where the ranking points next, in this family.** The same greedy probe names the rows left, and
they are three different bands rather than more rows:

- **The self costs that are activated from another zone** — "Discard ~" (25 cards) and
  "Exile ~ from your graveyard" (22). Both need `ActivatedAbility.activateFromZone`, which the cost
  clause *determines* — you cannot discard a permanent, and "from your graveyard" says where the card
  is. That makes the cost rule's result a pair of (cost, zone) rather than a cost, which is a change
  to `Costs.cost`'s type and to `Activated`'s four call sites: a band of its own, not a row.
- **The keyword-labelled costs** — "Exhaust — {5}{U}{U}" (6), "Power-up — {5}{G}{G}" (8),
  "Boast — {4}{R}" (7), "Max speed — {3}" (10), "Renew —", "Forecast —", "Waterbend". Each is a flag
  on `ActivatedAbility` plus a printed prefix; one shape, seven rows.
- **Filter gaps, not cost gaps** — "Sacrifice a Food" (14) and "Sacrifice another creature or
  artifact" decline in [`Filters`](src/main/kotlin/com/wingedsheep/assay/grammar/Filters.kt), not
  here: the artifact subtypes the SDK publishes no list for, and the type disjunction.
- **`{S}`** (20 cards) is an **SDK gap** — `ManaCost` cannot express snow mana, so `Primitives.manaCost`
  declines rather than inventing a symbol. That one is `add-feature` work.

## The spell-cost band

What a spell costs, and what changes it. Measured on its own against the 7,451 this band and the
modal band branched from, whole-corpus coverage went to **7,577 cards**, byte-exact lines 25,377 →
**25,551**, and the differential's compared population 2,747 → **2,795**; merged with the modal band
the two compose to 7,714 and 2,832. Like the cost band before it, no new SDK type:
`ModifySpellCost` already had every field.

**The probe picked it, and it picked it against both other rankings.** On the tail ranking the
family reads 265 cards blocked and 112 sole-blocked, which is fourth. What moves it to first is step
two — substituting a known-good line for the family's own span and re-parsing — because this family's
span is the *whole line*, so the substitution is exact rather than a prediction:

| family | tail rank `sole` | probe: whole cards finished |
|---|---|---|
| **`This spell costs …`** | 112 | **112** |
| modal (`Choose one —` + bullets) | 0 (363 reached) | 75 |
| `~'s power and toughness are each equal to …` | 67 | 68 |
| library look-at-top-N | 119 | 64 |
| `Whenever one or more …` | 130 | 32 |
| fronted `Until end of turn, …` | 186 | **0** |

That last row is the whole argument for step two in one line: the largest sole-blocked count in the
table finishes *nothing*, because every one of its payloads is a construct the grammar also lacks.

### The shape: one sentence, three vocabularies

`ModifySpellCost(target, modification, gating)` has three fields, and the printed sentence has
exactly three variable parts, so [`SpellCosts`](src/main/kotlin/com/wingedsheep/assay/grammar/SpellCosts.kt)
is their product rather than one rule per sentence:

```
  <subject> cost(s) <amount> less|more to cast <clause>.
     │                 │            │             └── nothing · if <condition> · for each <count>
     │                 │            │                 · if it targets <filter> · , where X is <var>
     │                 │            └── the direction: two SDK families, not a sign
     │                 └── generic or coloured: also two SDK families, split on the printed cost
     └── this spell · spells you cast · <quality> spells you cast · <quality> spells
```

The subject is a slot shared by every sentence, which is why "Creature spells you cast cost {1} less
to cast." cost one row rather than a parallel set — 148 cards arrived with the same five clauses the
`This spell` subject uses.

### Filters, instantiated a third time: spell position

A spell on the stack **is not a permanent**, and two rules follow from that fact rather than from a
spelling:

- A bare subtype in spell position means `Any.withSubtype`, not the battlefield noun phrase's
  `Permanent.withSubtype` — the reading the differential took 103 cards to settle for the
  battlefield. So `Filters.spellQuality` is the cascade instantiated for the position, exactly as
  `SelfSteps.retargetable` is instantiated per anaphor, and the bare-subtype row there is
  **canonical** where the battlefield one is an `alternate`.
- A `StatePredicate` — tapped, attacking, face-down — is a fact about a permanent, so a spell filter
  may not carry one. That guard is why Dream Chisel *declines*: its
  "Face-down creature spells you cast cost {1} less to cast." has a dedicated
  `SpellCostTarget.FaceDownYouCast`, and reading it as a creature filter with `IsFaceDown` on it is
  a different value that prints back byte-identically. The differential caught it; the guard closes
  it rather than folding it.

### The finding: `FixedIf…` restates `OnlyIf`, and the corpus is split

`CostReductionSource` carries `Fixed`, `FixedIfControlFilter`, `FixedIfCreatureAttackingYou`,
`FixedIfCreatureDiedThisTurn` and `FixedIfVoid`. Every one says "reduce by *n* when *P* holds",
which is what `ReduceGeneric(n)` under `CostGating.OnlyIf(P)` says — and the hand-written corpus
writes the same sentence both ways, 21 cards each.

The grammar emits **the gate**, and the reason is reach rather than counting: its condition slot is
the whole of `Conditions`, so one rule reads every "… if <condition>" sentence the grammar will ever
know, while the `FixedIf…` cases are five sentences that cannot become six without an SDK change.
The one exception is `FixedIfAnyTargetMatches`, which stays canonical for "if it targets …" because
what it tests — the spell's own target list — is not a `Condition` and has no gate spelling.

So seven goldens diverge on purpose, and that divergence *is* the finding: **the `FixedIf…` family
should fold into `OnlyIf`**, and until it does, two cards printing one sentence carry two models.
`Ghalta, Primal Hunger` is the same finding one level down — `TotalPowerYouControl` against
`TotalPropertyAmongPermanentsYouControl(Power, Creature)`, which the SDK's own KDoc already calls
the generalization of it.

### Leonin Vanguard: an intervening-if that lost its scope

Widening `Conditions` made this line readable, and the differential immediately reported it:

> At the beginning of combat on your turn, **if you control three or more creatures**, this creature
> gets +1/+1 until end of turn **and** you gain 1 life.

`Steps.conditionalClause` slotted a single atom and sat inside the clause run, so the condition
scoped the *first* clause and the second was joined after it. That round-trips byte-exactly and means
something else — and inside a trigger it means something else in the rules, because `Triggers` lifts
a **top-level** gate into `interveningIf` (CR 603.4, re-checked on resolution) and a gate buried
under a `Composite` is not top-level. The ability silently lost its intervening-if.

The fix is the one the pay-gates already carry, applied to the same shape: the rule's consequence is
a clause *run*, and the rule is sentence-terminal — out of `simpleClause`/`laterClause`, into
`clause`, with `gatedConsequence` as its slot so no second gate can open inside it. This is
Kalastria Highborn's lesson a second time: **a gate's scope runs to the end of the sentence.**

Making it terminal cost twelve cards of the shape "Counter target spell. **If you control a blue
creature, draw a card, then discard a card.**", which the ledger's diff reported the moment it was
re-baked — the artifact doing exactly the job it was committed for. The answer is not to relax the
rule but to name the position: `runEndingInScopedClause` is a run of ordinary clauses that *ends* in
a scoped one, with the scoped clause as the phrase's last slot so nothing can follow it. The
intermediate fix that only widened the full-stop join was wrong and the gate said so within one run
— "…draw a card, then discard a card." had two readings, the condition over both clauses or over the
draw with the discard joined after, which is the same scope leak one join further along.

### Card bugs it found, all of one known class

Five hand-written cards spell a **bare tribal noun** as `Creature.withSubtype` where the settled
reading is `Permanent.withSubtype` — the 103-card migration's rule, in positions that migration did
not reach (a cost gate's condition, a target filter, a lord's group). They are reported rather than
fixed here, because each lives in a different era module and the fix re-blesses the corpus-wide card
snapshot:

| card | where |
|---|---|
| Goblin Warchief [SCG] | "Goblins you control have haste" |
| Krosan Warchief [SCG] | the regenerate target |
| Grow Extra Arms [SPM] | the target filter |
| Tombstone, Career Criminal [SPM] | "target Villain card from your graveyard" |
| Dragon's Prey [TDM] | "if it targets a Dragon" |

### What is left in the family, with counts

- **The subject vocabulary** — "Instant and sorcery spells you cast" (20 lines) needs the type
  disjunction in its adjectival form; the bare colours ("Red spells", 25) and the remaining subtypes
  are rows in `Filters.spellQuality`, not new sentences.
- **`CostGating.NthOfTypePerTurn`** — "The first spell you cast each turn costs {1} less to cast."
  (6 goldens): a subject shape rather than a clause, and the `gating` field's third case.
- **The leading condition** — "If you weren't the starting player, this spell costs {1} less to
  cast." (3). Both orders exist for one model, so it is an `alternate` of the trailing form, exactly
  as the conditional statics do it.
- **`Conditions` itself** is the ceiling on the "if …" clause and always will be: 74 lines print a
  condition, and the grammar names eleven. Every row added there also enriches every intervening-if,
  every cast restriction and every conditional static — which is why it is the highest-leverage
  place left in this family and not a member of it.

## The modal band

"Choose one —" and the rows under it. Measured on its own against the 7,451 both this band and the
spell-cost band branched from, whole-corpus coverage went to **7,588 cards** and the differential's
compared population 2,747 → **2,784**; merged with the spell-cost band the two compose to 7,714 and
2,832, because they share no lines. It is also the first band that made the *line count go down*:
2,040 bullet rows stopped being counted as abilities of their own, because they never were any.

**The band is three changes, and only one of them is in `grammar/`.**

**A bullet is a continuation, so the split belongs to normalization.** Oracle lays one modal ability
out over several printed rows, and the ability split pass was reading each row as an ability — which
is how "• Destroy target artifact." came to be 2,015 declines under one dead token and hundreds of
tail rows, the case the ranking section names as the reason `TOKEN` exists at all. A lone bullet
denotes nothing; the thing it is a mode *of* is on the row above. So [`Normalizer`](src/main/kotlin/com/wingedsheep/assay/normalize/Normalizer.kt)
joins a bullet onto the line above it, and this is the one join whose inverse is **free**: the joined
line carries its own newlines and `restore` already joins lines with `\n`, so nothing is recorded and
nothing is replayed. Every other spelling — a spacer, a sentinel — would have needed an inverse to
get wrong.

**A bullet is also a sentence start, which is the third one.** `SentenceCase` already knew about the
line start, the ability cost's colon and the full stop; it now knows about `\n• `. That is what lets
the modal rules slot the existing effect vocabulary mid-sentence instead of needing a capitalized
copy of every verb — the same argument the full stop carried, and the file's own KDoc predicted this
shape ("if you find yourself wanting a capital inside a template, the answer is almost certainly
another sentence start"). The corpus states the rule too: of 2,121 bullet rows, every one opens on an
uppercase letter or a symbol and none on a lowercase letter.

**And a modal block is a *clause position*, not a line.** That is where the leverage is.
[`Modal`](src/main/kotlin/com/wingedsheep/assay/grammar/Modal.kt) is offered at `Steps.step`, beside
the sentence and the self-terminating clauses, so every context that already slots a step got modal
abilities without being told: **446** cards print one of the four headers as a whole line and
**204** print it after a trigger's comma ("When this creature enters, choose one —") or an activated
ability's colon ("Sacrifice this artifact: Choose one —"), and the second group cost nothing. A *mode* is one whole sentence from the
enclosing cascade, so every verb the grammar can read is a mode and a mode's targets come with it —
which is the shape `Mode` exists for, per-mode requirements being the SDK's own worked example
(Cryptic Command).

Because the family is a function of the cascade's sentence rule, it is instantiated once per anaphor
position, exactly as `SelfSteps.retargetable` is. And the bullets slot `sentence` rather than `step`,
so a mode is never itself modal — not a safety rail but the thing that makes the rule constructible,
since a family reaching the rule it belongs to is left recursion.

**Four headers, and two of them are disjoint by mode count.** `ModalEffect` says "how many" with two
numbers and English says it with four phrases: "Choose one" `(1, 1)`, "Choose two" `(2, 2)`, "Choose
one or both" `(2, 1)`, "Choose one or more" `(n, 1)`. The last two collide at two modes, so rather
than ordering an alternation they are made disjoint by what they can spell — "both" is a word about
exactly two things, and the corpus agrees without exception: all 56 cards printing "one or both" have
two modes and all 21 printing "one or more" have three or more. A three-mode `(2, 1)` is a model no
header can print and it declines.

**A mode's description is a fold, and it is a fold by construction rather than by accident.**
`Mode.description` is "Human-readable description of the mode", defaulting to `effect.description` —
presentation, never executed. Hand-written cards spell the printed row out with the card's own name
in it ("Boros Charm deals 4 damage to target player or planeswalker"), and the text reaching the
grammar has had that name abstracted to `~` before any rule sees it, so reproducing the row is
unavailable *by construction* and would put a tilde in a string shown to a player. The rule therefore
leaves the field at its default and never invents prose, and `Folds.dropModeDescriptions` drops it
from both sides — scoped to a `ModalEffect`'s modes rather than added to the presentation *key* list,
because `description` is not a rare name and `AlternativeCostEffect` carries one that is part of what
it does. Everything that decides what a mode *does* is still compared, and so are the count fields
above it, which is where two of the findings below came from.

**What it found: eight divergences, every one a card bug, and all eight are fixed here.** The band
brought thirty-seven more cards into the compared population and eight of them were wrong the first
time the grammar looked. Each fix is a card change, with a scenario test where the behaviour moved:

- **Winterflame** and **Scour for Scrap** fake "one or both" as *three* modes with `chooseCount = 1`
  — tap, damage, and a third mode that does both. `ModalEffect` has `minChooseCount` for exactly this
  (CR 700.2), and the hand-written spelling is a different card: it offers a mode the card does not
  print, and it reports one chosen mode where `SpellCastEvent.chosenModesCount` should say two. Both
  are now `chooseCount = 2, minChooseCount = 1` over the two printed modes, with
  `WinterflameScenarioTest` and `ScourForScrapScenarioTest` covering one mode, both modes, and the
  mode count itself.
- **Split Up** destroyed each half of the board with a `ForEach` over the group where every other
  sweep in the corpus gathers first and destroys the collection. That is not a spelling: gathering is
  what makes the creatures leave together, and one at a time changes what a dies-trigger sees. It is
  `Effects.DestroyAll` now, with `SplitUpScenarioTest` asserting both halves and a two-trigger sweep.
- **Bejeweled Warg** carried `countsAsModalSpell = false` on a printed "Choose one —" trigger. The
  field's own KDoc says `true` is for exactly that wording and `false` is for the non-modal mechanics
  that reuse the type (Gift); CR 700.2 makes a *spell or ability* modal on that wording, so the flag
  was saying something untrue about text that plainly is modal. It was also inert, since the only
  reader is the spell-cast path a triggered ability never reaches — which is why it survived review.
  Removed; the card's existing scenario test still passes unchanged.
- **Four cards are the bare-tribal-noun family again** — Bejeweled Warg's "target Wolf you control",
  Black Panther's "another nontoken Hero you control", Misery Charm's "target Cleric" and Vitality
  Charm's "target Beast", all `IsCreature` where the printed noun names only a subtype. The same
  class the 103-card migration fixed everywhere the grammar could already see; these four sat inside
  a modal block, which nothing read until now. All four now name `Permanent`, which is a filter edit
  and a re-bless — the treatment that migration established.
- **Misery Charm and Decoy Ploy** put no `IsPermanent` on "target Cleric card from your graveyard"
  where `TargetFilter.PermanentInYourGraveyard` — a facade that migration created, and whose KDoc
  says a bare tribal noun in front of "card" names any permanent card — says they should. Two cards
  on one side of a decided question and two (Angel of Flight Alabaster, Lord of the Undead) on the
  other. **The grammar was flipped to match the minority and the flip was reverted**: the SDK facade
  documents the reading and the migration created it, so reversing a decided question inside a
  band's PR would be the gate lying about which side is wrong. Both cards now go through the facade.
  Decoy Ploy's was the wider miss — `GameObjectFilter.Any` would let a Villain *instant* be chosen.

**One print mismatch, and it was not in this family.** A two-colour token line became reachable for
the first time and `Tokens` printed "white and green" where Exhibition Magician prints "green and
white". WUBRG is a *cycle*, and a printed pair starts at whichever colour leaves the other within two
steps forward — the adjacency Magic names its allied and enemy pairs by. A plain ordinal sort spells
five of the ten pairs backwards; the corpus writes all ten, 369 times between them, and agrees
without exception. Its one counterexample is prose rather than a token — Frenemy of the Guildpact's
reminder text naming "blue and green" as an example of an enemy pair — and reminder text is
stripped before any rule sees it.
Three or more colours is deliberately left alone, because eleven phrases is not enough to settle it
and two of them name the same three colours in different orders.

**Where the ranking points next, in this family.** The header is no longer what blocks a modal card —
the top rows keyed on a modal tail are now the modes' own *effect* vocabulary ("• Create a token
that's a copy of…", 103 cards; the damage-prevention modes, 57). What is left of the header itself is
the variants that reach a *different* `ModalEffect` field rather than a different count: "choose up to
one —" (27 cards), "Choose one. If [condition], choose both instead." (30), "choose one that hasn't
been chosen" and its this-turn sibling (20), and "choose one at random" (5), which has no SDK field at
all and is a finding rather than a gap in a rule.

## The counting band

*How many* — one vocabulary, and the three places a card puts it. Whole-corpus coverage
7,714 → **7,823 cards** (+109); the baked verdict ledger 7,513 → **7,618 whole**, with 105 cards
gained and **none lost**. No new SDK type, and the largest thing it changed is not a rule at all but
where a value is allowed to land.

**The band is the SDK's own factoring, read back.** `DynamicAmount` is the one language for "a
number the game works out", exactly as `CostAtom` is the one language for "a thing you pay" — and
the grammar had it the way the cost band found `Costs`: [`Amounts`](src/main/kotlin/com/wingedsheep/assay/grammar/Amounts.kt)
held a three-row `count` and **seventeen bespoke clauses**, each restating one verb over one
count. That is the mtgish curve in miniature. The fix is the cost band's: make the count a real
layered vocabulary and make each sentence a *lift* of it.

The vocabulary now layers the way [`Filters`](src/main/kotlin/com/wingedsheep/assay/grammar/Filters.kt)
does — one layer owning one field, never a combinator that can also print the others:

| Layer | Reads | Model |
|---|---|---|
| battlefield tally | "the number of Swamps you control" | `AggregateBattlefield(player, filter)` |
| zone tally | "the number of creature cards in your graveyard" | `Count(player, zone, filter)` |
| unfiltered zone tally | "the number of cards in your hand" | `Count(player, zone)` |
| aggregation | "the greatest mana value among artifacts you control" | the same aggregate, `aggregation` + `property` |
| counters on the source | "the number of +1/+1 counters on ~" | `EntityProperty(Source, CounterCount)` |
| player value | "your life total" | `YourLifeTotal` |
| multiplier | "twice …" | `Multiply(inner, 2)` |

### A `*` is half in the box and half in the text

The band's centre of gravity is the **characteristic-defining ability** (CR 604.3), and it is the
first line whose meaning does not land in `CardScript` at all. `Nightmare's power and toughness are
each equal to the number of Swamps you control.` is not an ability the engine executes — the SDK
puts it in `CardDefinition.creatureStats`, as `CharacteristicValue.Dynamic(…)`, which is what the
printed `*` means. So [`CardFragment`](src/main/kotlin/com/wingedsheep/assay/grammar/CardFragment.kt)
grew a fifth slot, one field per characteristic, and the compiler is where the two halves of a `*`
finally meet — the header knows there is a star and only the line knows what defines it.

Two fields rather than one `CreatureStats` because Yavimaya Kavu prints the halves on **separate
lines**, defining its power from red creatures and its toughness from green ones. A `CreatureStats`
needs both at once and neither line has both, so the fold is per characteristic.

The pairing in [`CardCompiler`](src/main/kotlin/com/wingedsheep/assay/compile/CardCompiler.kt) is
fail-closed in both directions, which is what makes it safe to have opened at all: a star with no
defining line still declines (as it always did), a defining line over a printed *number* declines
too, and the star's arithmetic is **checked rather than trusted** — Oracle prints Lhurgoyf's
toughness as `1+*` where the model spells it `*+1`, so the offset is compared as a number and
"…toughness is equal to that number plus 1" cannot compile onto a creature whose box says plain `*`.

The differential now compares the stat box as well, and that is where the band paid.

### Where the clause goes is decided by the model, not by rule order

A damage sentence puts its "equal to …" clause in two places and **both are real Oracle**: 195
printed lines trail it ("deals damage to target creature equal to the number of Mountains you
control") and 152 lead with it ("deals damage equal to its power to target creature"). Neither is a
minority spelling to decline — but two rules that can each print one model is printing decided by
alternation order, which this module treats as a latent bug rather than a preference.

The split the corpus actually draws is on the **shape of the amount**: a property read off an object
is a light noun phrase and leads; a tally is a heavy one and trails. That is a fact about the model,
so the two orders take disjoint halves of `DynamicAmount` and each refuses the other's — and the
*minority* order for each half is registered as an `alternate`, parsed and never printed, so a card
printing it comes back as a variant instead of declining. English is following the heavy-NP rule
here; `EntityProperty` is where the light ones live.

**Life is deliberately not in the band, and the reason is the same rule pointing the other way.**
"You gain 1 life for each creature you control" and "You gain life equal to the number of creatures
you control" are one model, and Oracle prints the first 131 times against the second's 23 — so the
"for each" family is already canonical there and adding the clause would be a second rule that can
print the same value. Making it an `alternate` does not work either: the "for each" rules only spell
*battlefield* aggregates, so a life gain counting a graveyard would parse with nothing able to print
it. The next step is to give "for each" the same vocabulary treatment this band gave "equal to", and
that is a band of its own rather than a row.

### Six card bugs, one of them a card doing something else entirely

The differential reported six divergences the day the grammar could read these lines, all six fixed
in the same change:

- **Revenant** counted creatures on the *battlefield* where its text says creature cards in your
  *graveyard* — a generated draft that nobody had reviewed, and a different card. It gets the
  band's one new scenario test, pinning the zone, the filter and whose graveyard is counted.
- **Heedless One, Nameless One, Reckless One** read "the number of Elves on the battlefield" as
  creatures. A bare tribal noun names every *permanent* with the subtype — the reading
  `TargetFilter.PermanentInYourGraveyard`'s KDoc settled and the 103-card migration established, and
  Zombie Master is the card that proves it deliberate. Filter edits plus a re-bless, no new tests.
- **Regal Bunnicorn** spelled its battlefield tally `Count(You, BATTLEFIELD, …)` where 603 goldens
  write `AggregateBattlefield` and 49 write the other. Same value, same evaluation, one printed form
  per model — so the grammar emits the majority spelling and this card moved to it.
- **Torrent of Fire** left the noun out of its aggregate, passing the default `Any` filter where the
  line prints "permanents". A no-op on the battlefield and still not what the card says.

The last two are the shape this module keeps producing: **two SDK spellings of one value**, recorded
as findings in `Amounts`' own KDoc rather than papered over. `Count(player, BATTLEFIELD, filter)`
restates `AggregateBattlefield`; `AggregateZone` with its default aggregation restates `Count`
(17 goldens against 236). The grammar emits one of each pair and says which.

### What is left in the family

The counts themselves. The top remaining tail row is `damage equal to …` at 116 cards, and its
examples are now all **entity properties** — "equal to its power", "equal to that creature's
toughness", "equal to the sacrificed creature's power" — which is 72% of every "deals damage equal
to" line in the corpus and 41% of the life ones. That is not another vocabulary row: "its" means the
source in one clause and the target in another, so it is a fourth **anaphor position**, and the
module's rule is to instantiate the vocabulary per position rather than to add an `oneOf` branch.
Sized by the probe at **47 whole cards**, and it is the natural next band.

## The top-of-library band

The top of the library — how many cards you see, what you keep, and where the rest go. Whole-corpus
coverage 7,823 → **7,910 cards** (+87); the baked verdict ledger 7,618 → **7,701 whole**, with 83
cards gained and **none lost**. No new SDK type, and the grammar shrank: four whole-sentence rules
became one three-layer vocabulary in a file of its own,
[`TopOfLibrary`](src/main/kotlin/com/wingedsheep/assay/grammar/TopOfLibrary.kt).

**It was picked by measuring, and the three families it covers are disjoint.** The tail ranking put
`cards of your …` (165 cards), `card of your …` (98) and `exile the top …` (90) at three separate
places in the list; the probe sized them at 64, 27 and 40 whole cards, and their blocked-card sets
turned out to share **zero** members — so 131 predicted against `enters tapped unless`'s 74, the next
best. Five other candidates were probed and dropped, and two of them are worth recording because the
tail rank ranked them *above* this one: the aura payoffs (`enchanted creature deals …` and its four
siblings, 404 cards between them) probe at **7–10 whole cards each**, because the missing piece is
never the attached-permanent subject — it is the payload behind it.

**The band is the SDK's own factoring, read back — the third time.** `Patterns.Library` publishes
these sentences as recipes whose *parameters are exactly the words that vary*: `keepDestination`,
`restDestination`, `restOrder`, `count`, `filter`. The grammar was calling them with every parameter
frozen, so `lookAtTopAndKeep` could spell hand-and-graveyard and nothing else — and "…and the rest on
the bottom of your library in any order", which the corpus prints far more often, was a rule nobody
had written rather than a *word* nobody had slotted. The fix is the cost band's and the counting
band's: three layers, every sentence a lift.

| Layer | Reads | Model |
|---|---|---|
| how many | "the top card", "the top four cards", "the top X cards" | `TopOfLibrary(count)` |
| where a pile goes | "into your hand", "onto the battlefield", "on the bottom of your library" | `CardDestination` |
| …and in what order | "…in a random order", "…in any order", and the empty suffix | `CardOrder` |

The count layer carries its own noun, which is what makes the grammatical number free: "the top
card" and "the top four cards" are one slot, and no sentence above it can get the agreement wrong.
The order layer is [`Filters`](src/main/kotlin/com/wingedsheep/assay/grammar/Filters.kt)' rule over a
different value — one printed suffix per `CardOrder`, the empty one included, so exactly one row can
express any given value and the printer never chooses.

### Three splits the model decides, not the alternation

- **"the rest" against "the other."** English writes "the other" when the remainder is exactly one
  card, and the corpus does it **16 times to 0**. So the two rules take disjoint halves of one value
  space on `count - keep == 1`; without that guard both could print Tower Geist and the alternation's
  *order* would pick, which this module treats as a bug that has not surfaced yet.
- **The impulse anaphor agrees with the count.** "That card" against "those cards" is decided by the
  number already in the sentence, so the rule *checks* the agreement rather than spelling it in two
  rows per duration — a second place that decides the number is a second place to get it wrong.
- **Which word order is canonical flips with the duration.** Every impulse duration is printed both
  ways, so neither is a minority to decline, but the majority changes: "this turn" **trails** 115
  lines to 40, while `UntilEndOfNextTurn` **fronts** 59 to 16 and `UntilNextEndStep` fronts 8 to 5.
  A template with the duration as a slot would print the minority spelling for two of the three.
  This is the counting band's damage-clause lesson arriving again, with a different field deciding.

### Eleven cards it found, and a fold

The band put `SelectFromCollectionEffect` under the differential for the first time, and every card
it newly compared that disagreed was wrong:

- **Prophetic Bolt** kept **all four** cards it looked at (`keepCount = 4` under a text that says
  "put one of those cards into your hand") and buried the rest in the graveyard rather than on the
  bottom of the library. Generated by `:mtgish-tooling` and never reviewed — precisely the failure
  mode [`../mtgish-tooling/README.md`](../mtgish-tooling/README.md) warns about, caught by running.
- **Stock Up** sent the remainder to the graveyard instead of the bottom of the library.
- **Impulse**, **Dig Through Time**, **Stock Up**, **Prophetic Bolt** and **Adventurous Impulse** all
  dropped `restOrder`, so "in any order" resolved as "in the order they were on top" — the controller
  never got the choice the card grants. Adventurous Impulse also dropped the reveal on the kept card.
- **Ashe, Princess of Dalmasca** and **Boughside Wanderers** dropped `showAllCards`, so a player told
  to "look at the top five cards" was shown only the ones they could take.
- **Alania's Pathmaker**, **Kulrath Zealot**, **Sizzling Changeling** and **Gundabad Opportunist**
  each restated `Patterns.Exile.impulse`'s pipeline by hand under a different collection key. Not a
  behaviour bug, but exactly what `mtg-sdk/AGENTS.md`'s "use the facades, not raw constructors" is
  for, and all four now call it.

All eleven are fixed in this change, and the differential is back to its 13 standing divergences with
**32 more cards compared** (2,854 → 2,886).

The one **fold** added is `selectedLabel`/`remainderLabel`, `prompt`'s two siblings on the same
object and documented by the SDK in the same words ("Shown in the UI"). It loses nothing: the labels
are *derived* from the two destinations by `defaultDestinationLabel`, and the destinations are still
compared, so a label that disagreed because the destination did is still caught — by the destination.
`showAllCards` is the nearest miss and stays compared, because it decides which cards the player is
shown rather than what they are told about them.

### What is left in the family, and the probe's fifth data point

+87 against a predicted 131 — the probe over-stated by 1.5×, which is the same direction and roughly
the same size as every band before it. The gap is legible rather than mysterious, and it is one thing:
`SelectionMode` has four cases and this band reads two of them. `ChooseExactly` and `ChooseUpTo(1)`
are in; "Put **up to two** of them into your hand" (`ChooseUpTo(n)`, 56 cards / 35 sole-blocked) and
"You may put **any number of** them into your hand" (`ChooseAnyNumber`) are not, and both rose in the
ranking *because* of this band — the lines now get further before dying.

Both are blocked on the same **SDK finding**, which is reported here rather than routed around:

- `Patterns.Library.lookAtTopAndKeep` hardcodes `SelectionMode.ChooseExactly` and takes no `filter`.
- `Patterns.Library.lookAtTopRevealMatchingToHand` hardcodes the hand as the kept card's destination,
  so "You may put a land card from among them **onto the battlefield** tapped" (~20 lines) has no
  recipe to call.

Both are one parameter each, and both are a *capability* change to `mtg-sdk` — `add-feature` work
with the SDK's own bar, not something this module may do on its own. Hand-building the pipelines here
instead would restate recipes the SDK already owns, which is the exact curve this file exists to stay
off. The other residue is genuinely different constructs: the whose-library slot ("of target
player's library", 21 cards), the single-card conditional ("Look at the top card. If it's a land
card, you may put it onto the battlefield tapped.", 88 cards / 68 sole), and the dynamic count
("Look at the top X cards of your library, **where X is** …", 38 cards).

## The conditional-tapped-entry band

The lands that enter tapped *unless* something is true — the check lands, the fast lands, the slow
lands, the Duskmourn cycle, and every one-off beside them. Whole-corpus coverage 7,910 → **7,967
cards** (+57); the baked verdict ledger 7,701 → **7,758 whole**, with 57 cards gained and **none
lost**. No new SDK type; one grammar template, four new rows in the condition vocabulary, and
two facade entries in `mtg-sdk` that name a composition the cards were already spelling by hand.

**It was picked by measuring, and it was the top of the list on both numbers.** The tail ranking put
`enters tapped unless …` at 107 cards blocked and **75 sole-blocked**, and the probe — substituting
"~ enters tapped." for the whole clause — also said **75 whole cards**, because all 107 of the
family's lines parse once the clause does. Every other family in the top twenty was probed in the
same run; the next best were `unless its controller pays …` (58), `Spend this mana only …` (57) and
`Prevent all damage …` (55), and the two families the tail rank put *above* this one both measured
near zero — fronted `Until end of turn, …` (188 sole-blocked) at **0**, and `As ~ enters, …` (54
sole-blocked) at 4, because every payload behind them is missing too.

**75 predicted, 57 delivered, and the gap is a list rather than a mystery.** The probe replaces the
family's whole clause with a known-good one, so its number is what the band is worth *if every
condition in it can be spelled*. Seventy-five of the 107 lines now read; the other 32 are the four
residue classes at the end of this section, and two of them are deliberate refusals rather than
unwritten work. That is the honest reading of this probe shape: it sizes the sentence, not the
vocabulary the sentence slots.

**The band is a frozen field, which is the top-of-library band's lesson on a different type.**
`EntersTapped(unlessCondition, payLifeCost)` has three printed shapes and
[`Replacements`](src/main/kotlin/com/wingedsheep/assay/grammar/Replacements.kt) was reading two of
them. The third was written off in that file's own KDoc — "an `unlessCondition` is an arbitrary
`Condition`, and the grammar has no condition vocabulary yet" — and by the time the spell-cost band
had built [`Conditions`](src/main/kotlin/com/wingedsheep/assay/grammar/Conditions.kt), that sentence
had stopped being true without anyone noticing. So the whole band is one template with
`Conditions.condition` in it, and the 107 lines spell 36 distinct clauses of which 35 belong to the
vocabulary, where every other position that takes a condition gets them for free.

### Two articles, and where the "or" lives

"You control a **Mount or Vehicle**" is one noun phrase and one filter with a subtype `Or` in it.
"You control a **Plains** or an **Island**" is two noun phrases with the verb elided, and it is a
disjunction of *conditions*. The hand-written cards drew the line in exactly that place before the
grammar did — Country Roads holds one `Exists` over a `Mount|Vehicle` filter, Sulfur Falls holds
`Any(Exists(land Island), Exists(land Mountain))` — so the second article is what keeps the two
rules disjoint, and it is a fact about the text rather than a convention this module invented. That
is what lets the rule be canonical in both directions instead of an `alternate`.

### "Other" is a field on the amount, and twenty cards were doing arithmetic instead

The fast lands say "unless you control two or fewer **other** lands" and the slow lands "two or more
other lands". Twenty goldens wrote that as *total lands ≤ 3* / *≥ 3* — the printed number plus one,
over a tally that excludes nothing. That is equal to the sentence only while the source is itself a
land already on the battlefield when the condition is checked, which is true of every card printing
the line today and of nothing the shape guarantees.

`DynamicAmount.AggregateBattlefield` has had `excludeSelf` all along, so the literal reading was
always available; `GameObjectFilter` has no self-exclusion, which is why the word belongs to the
*count* and the two rules are further corners of the existing `countAtLeast` shape rather than a new
one. The differential named all twenty the day the rows landed, and all twenty moved in the same
change through two new facade entries — `Conditions.YouControlOtherAtLeast` / `…AtMost`, naming an
existing composition rather than adding a capability, so the cards and the grammar rule are one
definition and neither can drift. `BloomingMarshScenarioTest` is the behaviour proof for the
`AtMost` half (the `AtLeast` half already had the five slow-land tests).

### What the gate found: 21 new divergences, and 21 fixed here

The band brought **199 more cards** into the compared population (2,886 → 3,085) and every card that
disagreed was one of two things. Twenty were the arithmetic above. The twenty-first was Cori Mountain
Monastery, which became comparable only because its tapped-entry line now reads — and it turned out
to hand-restate `Patterns.Exile.impulse` out of raw constructors under a different collection name,
the fifth instance of the class the top-of-library band found four of. Both are fixed here, and the
differential is back to its 13 standing divergences at **99.6% agreement**.

### What is left in the family, and the next band it named

Thirty-two of the 107 lines still decline, and the residue splits cleanly:

- **Two SDK gaps, reported rather than routed around.** "You have two or more opponents" (10 cards)
  and "your opponents control eight or more lands" (5) have no condition the SDK names, and a
  `Compare` assembled here would be a second spelling of something `Conditions` should own.
- **One refusal the module's own invariant demands.** "~ enters tapped unless **it's your turn**"
  (Horned Loch-Whale) declines even though the SDK names `IsYourTurn`, because
  `SpellCosts.leadingGate` already prints that value as the fronted "During your turn, …" clause and
  a second row would be a second printed form for one model. The row was written, and the
  `every spell-cost rule prints what it parses` meta-test rejected it within the same run — which is
  what that meta-test is for, and worth recording as the first time it caught a *cross-family*
  collision rather than a dead `match`.
- **One `Filters` layer, and it is the next band.** "You control a **legendary** creature" (6 cards
  here) declines because the grammar has no legendary layer — and **416 declined lines corpus-wide
  mention "legendary"**, which is a band of its own rather than a row to bolt onto this one. It is
  the documented shape: one layer that owns one field.
- **The rest are genuinely separate constructs** — the bare non-creature subtype ("an Equipment",
  which the `bareSubtype` gate declines on purpose), the colour disjunction over a permanent noun,
  and the two "you revealed a Dragon card this way **or** you control a Dragon" compounds.

## What Phase 1 already found

The report is two documents at once, and the second one is about `mtg-sdk`:

- **Two keyword abilities of identical shape are modelled two different ways.** `Enchant creature`
  (1,289 cards) is an aura's attachment restriction and `Equip {2}` (621 cards) is a
  `CardDefinition.equipCost` field — neither is a `KeywordAbility`, so neither had anything to
  parse *into*. They were the two largest keyword-only decline families in the corpus.
  **Only one of them turned out to be a sentence.** Enchant is a plain `TargetRequirement` in a
  plain `CardScript` slot, so the aura band reads it as an ordinary filtered target and the whole of
  `Filters` arrives with it — "Enchant land" and "Enchant creature you control" are rows in a list,
  not rules. Equip is not the same shape at all: it lowers at authoring time into `equipCost` *and*
  a synthesized activated ability with its own timing, effect and target requirement, so reading it
  means reproducing a lowering rather than a sentence, and it reaches past `CardScript` into a slot
  `CardFragment` did not model. That is why the pair stopped being one finding — and the equipment
  band below is what closing the second half cost: a new `CardFragment` field, a shared factory in
  the SDK, and a normalization pass, against the aura band's one rule.
- **`PROTECTION_FROM_EACH_OPPONENT` and `ProtectionScope.EachOpponent` are two spellings of one
  thing.** Registering both would be genuine ambiguity, so the grammar deliberately spells it only
  one way (see `Primitives.protectionScope`).
- **The printed separator is not recoverable from the model.** ~31 older cards print
  "Flying; banding"; a flat `List<KeywordAbility>` has no room for the separator, so they come back
  as `VARIANT`. Same class as line grouping, which normalization owns instead.
- **Reminder text is a function of the ability *and* the card's types.** Printed glosses say "this
  creature" / "this artifact" / "this land", which a `KeywordAbility` alone cannot produce —
  `Reminders.gloss` takes the noun as a parameter for exactly that reason.
- **~40 keyword abilities have no `Keyword` enum constant at all** — Exalted, Infect, Echo,
  Soulshift, Bloodthirst, Scavenge, Backup, Megamorph, Unleash, Extort, Evolve, Myriad, Unearth,
  Mentor, Afterlife, Enlist, Champion, Eternalize, Skulk, Melee, Battle cry, Reinforce, Devoid,
  Dethrone, Phasing, Cumulative upkeep, … — ranked by cards blocked in the report's bottom table.

## The prevention band

Damage prevention, as one SDK type and the product of its fields. Whole-corpus coverage 7,967 →
**8,042 cards** (+75); the baked verdict ledger 7,758 → **7,829 whole**, with 71 cards gained and
**none lost**. No new SDK type and no new SDK field; one new grammar file, and two whole-sentence
rules deleted from the files they had accreted in.

**It was the second-best family on the probe and the best one available.** The tail ranking's top
rows were probed in one run: `Prevent all damage …` measured **55** whole cards and
`Prevent all combat …` **46**, against `Spend this mana only …` at 56, `life equal to …` at 52 and
`unless its controller pays …` at 40. The two prevention rows are one construct and disjoint
families, so the band was worth what they add up to and nothing else in the top forty came close.

**101 predicted, 75 delivered — and the reason is a new one.** The rule of thumb from the spell-cost
band was that the probe is exact when the family *owns the line*, and this family does: the
substitution replaced whole prevention sentences and every other line on those cards had to parse
too. It still overstated by a quarter, because a family that is one SDK type's **product** has
members the type cannot reach. Roughly forty of the corpus's prevention lines carry no duration at
all ("Prevent all damage that would be dealt to ~.") and are not this type — a permanent's standing
prevention is `ReplacementEffect.PreventDamage` over an `EventPattern`, a vocabulary the grammar
does not have yet — and the rest name things the fields cannot hold: noncombat damage, players as a
recipient group, damage divided among any number of targets. **Read a whole-line probe as exact only
when the construct behind the line is uniform.**

### The shape: four axes, and where each one lives

`PreventDamageEffect(target, recipientGroup, amount, scope, direction, sourceFilter, …)`. The printed
sentence has a variable part for each, and
[`Prevention`](src/main/kotlin/com/wingedsheep/assay/grammar/Prevention.kt) is their product rather
than a rule per combination:

- **`scope`** is one word, so it is a slot — "damage" or "combat damage" — in every sentence.
- **`amount`** changes the quantifier ("all" versus "the next 2" versus "the next X") and `null` is
  one of its values, which a slot cannot carry: a `build` returning null means the surface denotes
  nothing, so `Phrase<DynamicAmount?>` would make "all" unparseable. Three instantiations of the
  shape instead, over disjoint halves of `DynamicAmount` so printing stays determined by the model.
- **`direction`** changes the clause frame, so it is three frames rather than a word.
- **`target`** is a recipient vocabulary — the noun phrase, the `EffectTarget` behind it, and the
  requirement the script declares for it, moving together. Written once and instantiated per anaphor
  position, exactly as `SelfSteps.retargetable` is: `Continuations` takes the "that creature" member
  and nothing else, which is what stops a line whose whole content is a dangling reference.
- **`sourceFilter`** is a **layer**, never a member of a frame. One optional suffix ("… by attacking
  creatures", "… by a source of your choice", "… by creatures") owns that field and strips precisely
  it before delegating, and it refuses any inner clause that is not `ToTarget` — otherwise a
  group-sourced shield would have two printed forms, one from the layer and one from the silencing
  frame.

The fourteen prevention facades in `Effects` are the finding, not the build path. Each freezes a
different subset of the same six fields — `PreventAllCombatDamage()`, `PreventCombatDamageToAndBy`,
`PreventAllDamageDealtBy`, `PreventNextDamage` — so they are points on the product this file spans,
and picking one per combination would be a `when` over the model reproducing a mapping nobody wrote
down. The type is the curated surface here, exactly as `Replacements` argues for the
replacement-effect constructors.

### The redundancy report found the rule this band generalized

`Combat` carried `"prevent all damage that would be dealt to you this turn by attacking creatures"`
as a whole sentence. The new layer reads the same text into the same model, and two rules producing
one model for one text is grammar **redundancy** — reported, not gated, and the leading indicator of
a hard ambiguity. It went from 0 to 2 in the same run that added +75 cards, which is what surfaced
it; deleting the old rule put it back to 0. **After generalizing a family, read the redundancy
number, not just the coverage number** — it is the only signal that says the sentence you just made
composable was already spelled somewhere else.

### Two collisions the SDK cannot see, and one it can

- **The Fog has two English sentences and one model.** `PreventDamageEffect(scope = CombatOnly)`
  leaves `target` at its `EffectTarget.Controller` default, and `PreventDamageExecutor` reads exactly
  that configuration as *global* prevention rather than as a shield over the controller. So "prevent
  all combat damage that would be dealt to you this turn" (Inkshield, Take the Bait) denotes the same
  value as "prevent all combat damage that would be dealt this turn" and cannot be told apart from
  it. The Fog rule owns the value, every recipient frame refuses it, and the two cards that mean the
  narrower thing decline and are counted.
- **"By attacking creatures" is spellable twice** — as `PreventionSourceFilter.AttackingCreatures`
  and as a `FromGroup` over the same three words. The dedicated case is what the hand-written cards
  carry (Deep Wood), so the group layer refuses that filter outright and the `FromGroup` spelling of
  it has no printed form. Same class as `PROTECTION_FROM_EACH_OPPONENT`: two SDK spellings of one
  thing get one rule and a note.
- **The silencing frame's canonical word order flips with the scope.** Oracle writes `FromTarget`
  as "prevent all combat damage that would be dealt **by** target attacking creature this turn" and
  as "prevent all damage target creature **would deal** this turn", and the majority flips with the
  kind of damage — the passive leads 11 lines to 3 for combat damage, and the active is the only
  spelling the corpus uses for damage in general. Each rule takes the half it wins and refuses the
  other, and the minority spelling for each half is an `alternate`. That is the top-of-library band's
  duration finding one field over: the canonical order *is* decided by the model, just not by the
  field you would guess.

### What the gate found, and what the residue is

The band brought **60 more cards** into the compared population (3,085 → 3,145) and every one of them
agreed — the differential stayed at its 13 standing divergences and 99.6%. That is the less common
outcome and it is worth stating plainly: `PreventDamageEffect` was already under comparison through
the two rules this band replaced, so the fields were being checked before the sentences generalized.

Three cards moved from `LINE_DECLINED` to `LINES_DO_NOT_FOLD` — Restrain, Azorius Ploy, Festival of
the Guildpact. That is not a regression but a **second** gap surfacing behind the first: each prints
two effect paragraphs on two lines ("Prevent all combat damage … this turn." / "Draw a card."), and
`CardFragment.merge` refuses two lines that both claim to be *the* spell effect, because a
`CardScript` has one `spellEffect` and concatenating them would invent an order nothing checked. The
57 cards in the differential's `lines do not fold into one card` bucket are that same shape, and it
is a band of its own.

The rest of the residue is named above: the duration-less statics waiting on an `EventPattern`
vocabulary, `PreventionScope`'s missing noncombat case, a recipient set of *players* that a
`GroupFilter` cannot hold, and the divided-damage sentence. One more is a `Filters` gap rather than a
prevention one — "creatures your opponents control" (Thwart the Enemy, and 86 cards corpus-wide) has
no controller layer yet, which is the same shape as the `legendary` layer the tapped-entry band
measured at 416 lines.

## The batch-trigger band

CR 603.2c's "one or more". Whole-corpus coverage 8,046 → **8,076 cards** (+30, over the fronted
duration band it merged with); the baked ledger 7,833 → **7,863 whole**, 30 in and **none out**; the
differential's divergence count 13 → **14**, the four new ones being card bugs fixed in the same
change. No new SDK type and no new SDK field; rows in
[`Triggers`](src/main/kotlin/com/wingedsheep/assay/grammar/Triggers.kt) over one generalized shape,
two noun phrases in [`Filters`](src/main/kotlin/com/wingedsheep/assay/grammar/Filters.kt).

**It was the tail ranking's number-one family and the probe cut it by an order of magnitude.**
`one or more …` blocked **355 cards**, was the sole blocker of **140**, and owned **359 declined
lines** — the largest family in the report by every column. The probe over its own span
(`^Whenever one or more [^,]*, ` → `When ~ enters, `) put the payoff at **89 lines and 35 whole
cards**. The gap is the family's payoffs, not its prefixes: sub-family by sub-family the probe read
26 of 68 combat-damage lines, 17 of 34 leave-your-graveyard, 14 of 34 counters-placed — and **6 of
48** attack lines and **2 of 24** enters lines, because what follows a batch trigger is
overwhelmingly "that many", "them" and "those creatures".

**That is the band's real finding, and it is about the collection.** A batch names a *set*: the
engine hands the resolving ability a captured collection, and Oracle's payoffs address it in the
plural. The grammar has no vocabulary for that, and the whole residue of this family sits behind it —
261 lines still open, led by 56 combat-damage payoffs. The batch **collection** is the next band, and
it is worth more than any of the trigger prefixes were.

### The controller clause belongs to the sentence, not to the noun

Every batched `EventPattern` in `mtg-sdk` folds an *absent* `controllerPredicate` to "you control" —
`PermanentsEnteredEvent` and `CreaturesYouControlDiedEvent` say so in their KDoc and their detectors
do it, and `OneOrMoreDealCombatDamageToPlayerEvent` is scoped to the observer before its filter is
consulted at all. So the bare plural noun does not mean here what it means on the battlefield, and
`Filters.plural` is the wrong slot: it would print "creatures you control" from `ControlledByYou`,
which is a second spelling of what the event already says. The answer is the spell-cost band's —
**a position can need its own instantiation of `Filters`** — so `Filters.pluralSubject` is a third
instance of the cascade that stops one layer below the controller clause, and each scope is a row:

| Printed | `controllerPredicate` | Facade |
|---|---|---|
| "one or more creatures **you control** die" | *absent* | `OneOrMoreCreaturesYouControlDie` |
| "one or more creatures die" | `ControlledByAny` | `OneOrMoreCreaturesDie` |
| "one or more creatures **your opponents control** die" | `ControlledByOpponent` | `OneOrMoreCreaturesAnOpponentControlsDie` |

Three printed forms, three SDK facades, one row each — and `ControlledByYou` has no printed form,
which is the `ManaColorSet.Specific` treatment and the same membership check: a card in the minority
whose line otherwise reads is a card to look at. Crossed with the family's "other"
(`excludeSource` / `excludeSelf`) that is one six-row product per family rather than six rules.

### The rider was worth more than any single family

"This ability triggers only once each turn." is `TriggeredAbility.oncePerTurn`, a rider on the
*ability* and part of no event — so **one rule reaches every trigger sentence the grammar can read**.
It had to be written here because the fail-closed reconstruction each trigger family performs
compares the whole model: until this rule existed a capped ability *refused to print*, so every card
carrying the rider declined no matter how ordinary its trigger was. 49 of this family's own lines
carry it, and so do a hundred-odd outside it.

Two things fell out of it immediately. The rider made four previously-unreadable lines readable, and
**all four were card bugs the differential then caught** — three of the bare-tribal-noun family
(Flying Octobot's "another Villain you control", Mary Jane Watson's "a Spider you control", Invasion
Tactics' "Allies you control", all reading `Creature` where a bare tribal noun names *permanents*)
and one real rules bug: Stocking the Pantry and Terrasymbiosis both print "Whenever **you** put one
or more +1/+1 counters…" and both used the shared `PlusOneCountersPlacedOnYourCreature` facade, which
leaves `placedBy` null — the passive wording, which fires on an opponent's placement too (CR 122.6).
All five fixed here.

And it moved the fail-closed meta-test's witness. `TriggersTest` asserted that an ability carrying
`oncePerTurn` refuses to print; that assertion is now false *because the rider is spelled*, so the
witness moved to `triggersOnce` ("This ability triggers only once."), which is still nobody's row.
**A fail-closed test names a field no rule spells, so it has to move every time a band spells one.**

### Two write-offs, and one finding left standing

**Attacks are out on purpose.** "Whenever one or more Merfolk you control attack" is `YouAttackEvent`
and looks like the family's second-biggest row, but eight of its printed lines say "attack **a
player**" — a narrowing (`AttackPredicate.DefenderIsPlayer`) that lives only on the per-creature
`AttackEvent`, so the two English sentences would collapse to one model and printing would be
underdetermined. The probe reads 6 of 48 lines behind it regardless. "During your turn" is the other:
it is `triggerRestriction = Conditions.IsYourTurn`, a clause nobody has written, at 7 lines.

**`OneOrMoreDealCombatDamageToPlayerEvent` is the one event in the family with no `dsl.Triggers`
facade.** The five hand-written cards using it write the raw `TriggerSpec` and so does the rule, which
keeps the grammar and the cards one definition — but naming it is the right change, and it is an
`mtg-sdk` one.

**Ghoulish Procession stays divergent, and it is an engine gap.** "Create a 2/2 black Zombie creature
token with decayed" is a keyword on the token per CR 702.147, so the grammar reads
`CreateTokenEffect.keywords` — but the engine realizes Decayed's "can't block" and its
attack-sacrifice trigger only off `CounterType.DECAYED`, so the keyword alone is inert and the card
writes `initialCounters = {decayed: 1}`. The card's spelling is the one that *works* and the
grammar's is the one the text states; neither is right to change on its own, so the gap is reported.
Same shape as the display-only keywords the report's bottom table already ranks.

## The fronted duration

"Until end of turn, target creature gets +3/+3." — the same sentence the grammar already read, with
its duration at the other end. Whole-corpus coverage 8,042 → **8,046 cards** (+4); the baked ledger
7,829 → **7,833 whole**, four cards gained and **none lost**. No new SDK type, no new grammar family
in the usual sense: one kernel capability, one derivation, and one line on each of thirteen rules.

**The number this band is worth is the one it measured, not the one it moved.** "Until end of …" is
the second row of the tail ranking — 265 cards blocked, 189 sole-blocked, 267 lines — and every
ranking the module has is over the text a line *dies on*, which here is its first clause. So the
probe was run the way [ranking a band](#ranking-what-to-write-next) prescribes, with one wrinkle:
the family's construct is not a prefix that can be substituted for, it is a *position*, so the
measurement moves the duration to the back of each declined line and re-parses. Five of 266 lines
come back readable. The other 261 decline again on what follows:

| what the payload says | lines |
|---|---|
| "becomes a 5/5 green Plant creature …" — animating a permanent | 54 |
| granting a quoted ability — `"When ~ dies, …"` | 42 |
| "has base power and toughness 4/4" | 32 |
| pumping by a count — "+1/+1 for each creature you control" | 14 |
| a colour, a type removal, a blocking restriction, a damage rider, … | 119 |

That is the band's actual product: **four named families, ranked, each of which lands in both word
orders the day it is written.** A family that is a clause position measures its payload rather than
itself, and the four cards this one finished are the ones whose payload was already in the grammar.

**The probe was exact for once, and the reason is worth keeping.** Every previous band overstated —
234 predicted against 183 delivered, 101 against 75 — and the modal band understated because its
payoff sat in lines that had never declined as a family. This one predicted 5 lines and 4 whole
cards and delivered 5 and 4, because the substitution is not an approximation of the construct here:
moving a word is exactly what the rule does. **A probe that performs the family's own transformation
rather than standing in for it has no gap to be wrong in.**

### The position, and why it is not thirteen more rules

Oracle prints this duration trailing 5,370 times on 4,862 cards and fronted 823 times on 810 — one
model, two spellings, and by the sixfold majority the trailing one is canonical. The fronted form
therefore has to *parse and never print*, which is what `alternate` has always been for. What it
could not be is thirteen sibling rules: the fronted spelling of "target creature gets +3/+3 until
end of turn" needs the identical `build` and `match`, and a second copy of a rule's two halves is the
drift the whole bidirectional discipline exists to prevent — they agree until someone edits one.

So the kernel grew the one generic thing it was missing: `PhraseBuilder.alsoSpelled(template, name)`
registers an additional surface template on the *same* rule, sharing its closures and alternate by
construction. It knows nothing about durations;
[`Durations`](src/main/kotlin/com/wingedsheep/assay/grammar/Durations.kt) owns the word and the
derivation, and each durational rule adds a single self-derived line:

```kotlin
phrase("target {filter} gets {mod} until end of turn", name = "pump target") {
    frontedDuration()   // ← derives "until end of turn, target {filter} gets {mod}" from the template above
    …
}
```

Two details in the derivation are load-bearing. It **requires** a trailing duration rather than
quietly doing nothing, so a rule that is not durational fails at construction — every rule is built
during object initialization, which makes that the first thing a test run reports. And it fronts
into the template's **last sentence**, not onto its front: "Choose a creature type. Each creature you
control becomes that type until end of turn." fronts as "Choose a creature type. Until end of turn,
each creature you control becomes that type.", which is what the duration scopes over and what
Oracle prints. Prefixing the whole template would have read a sentence no card prints.

**Two rules keep their fronted literal, and that is the corpus's decision rather than an
inconsistency.** `Combat`'s attack-and-block tax and `TopOfLibrary`'s two cross-turn impulse
durations print fronted on *every* card that has them, so there the fronted form is canonical and
belongs in the template. Which order is canonical is a fact about the duration — "this turn" trails
115:40 while `UntilEndOfNextTurn` fronts 59:16 — and this band changes nothing about that; it only
gives the one duration whose majority is trailing its minority spelling.

## The entry band

"When ~ enters …" and "As ~ enters, …" — the tail ranking's **first and second** families, 230 and
210 cards blocked, 131 and 54 of them solely. They are one band because they are one moment: CR
614.1c's "as" happens *during* the entry and CR 603.2's "when" happens after it, and the grammar had
a rule for the bare form of each and nothing for what the corpus actually prints around them.
Whole-corpus coverage 8,076 → **8,102 cards** (+26); the baked ledger 7,863 → **7,888 whole**,
25 cards gained and **none lost**; byte-exact round-trips 26,196 → 26,264, alternate spellings
1,423 → 1,436. The differential's compared population rose to 3,412 and its divergences 14 → **15**,
which is the finding below. No SDK change: rows and slots in
[`Replacements`](src/main/kotlin/com/wingedsheep/assay/grammar/Replacements.kt) and
[`Triggers`](src/main/kotlin/com/wingedsheep/assay/grammar/Triggers.kt).

### The join: one printed sentence, two abilities, and a table that was measured

`Triggers` already had the shape — `pairedTriggerRule`, written for "Whenever ~ attacks or blocks",
whose KDoc says a `TriggeredAbility` watches one event and Oracle's "or" here joins two. It had one
row. What was missing was not machinery but the *list*, and the list is a corpus question rather than
a design one, so it was counted before it was written:

| the join | lines | how the corpus spells it |
|---|---|---|
| enters or attacks | 164 | "Whenever" |
| attacks or blocks | 41 + **14** | 14 older cards say "When" — Mardu Blazebringer, Windscouter |
| enters or dies | 25 + **6** | 6 predate the word and spell CR 700.4 out — Ichor Wellspring |
| enters or leaves the battlefield | 14 | "When" |
| enters or is turned face up | 7 | "When" |

The two bolded columns are `alsoSpelled`, the kernel capability the fronted-duration band added:
same rule, same `build`, same `match`, a second surface that parses and never prints. That is what
keeps "is put into a graveyard from the battlefield" from being a sixth rule whose two halves agree
until someone edits one of them.

**The cross product was not written, and the reason is the module's own rule.** Ten self-events would
be forty-five joins; the corpus prints five. What is left out is left out for stated reasons rather
than for scale: "or specializes" (5) and "or the creature it haunts dies" (7) name events with no
`TriggerSpec`; "or transforms into <name>" (7) names one event per card; and "blocks or becomes
blocked by a creature" (39) is not a second *self*-event at all but a filtered one, so it belongs to
a rule that can slot the filter.

### The finding: `AnyOf` is a second SDK spelling of the join, and it is not folded

`dsl.Triggers.or` lowers to `EventPattern.AnyOf` — **one** ability watching both events — and
Rakish Scoundrel's golden uses it, with a KDoc arguing that one printed ability should fire once.
It is a good argument. It is also the minority: for "enters or is turned face up" the split is 3–3
(Gadget Technician, Offender at Large, Rakish Scoundrel against Ponyback Brigade, Efreet Weaponmaster,
Culvert Ambusher), and across every other join in the table it is 0–60. So the grammar prints the
majority, Rakish Scoundrel is the differential's fifteenth divergence, and **the fold list does not
grow**: two abilities and one `AnyOf` ability are genuinely different models — they differ the moment
anything copies, counts or removes an ability — and folding them would be the gate agreeing with
itself. A `match` that accepted both would be worse still: two readings of one text is the definition
of `AMBIGUOUS`.

### The product: eight noun phrases where there were two constants

`EntersWithChoice`'s own KDoc says it "replaces the former EntersWithColorChoice,
EntersWithCreatureTypeChoice, and EntersWithCreatureChoice with a single parameterized type", and the
grammar was holding two frozen calls of it — `choose a color`, `choose a creature type`. The same
move the top-of-library band made on `Patterns.Library` applies here, with the axes split by *how
Oracle spells them*:

- **The kind of choice is a noun phrase**, so it is a rule parameter and each is a row: a color, a
  creature type, another creature you control, a basic land type, an opponent, and `CardNamePool`'s
  three — a land card name, a nonland card name, a card name.
- **Who chooses is one word position**, so it is a slot: "choose" against "an opponent chooses", one
  vocabulary the whole noun list shares. Callous Oppressor is the only card that needs it today and
  it costs one rule rather than eight.
- **A chosen number carries data past the kind**, so CR 614.1c gets a rule of its own with the two
  numerals as `minValue`/`maxValue` — Shapeshifter, Talion.
- **`lookAtOpponentHand` is a flag whose spelling the corpus decides.** Every line that has the look
  also says "any card name"; every line without it says "a card name". So Sorcerous Spyglass is one
  sentence with the flag set, not a prefix on the plain rule — a prefix would print "look at an
  opponent's hand, then choose a card name", which no card says.

**Three write-offs, each with its reason, each asserted as a decline in `ReplacementsTest`.**
`ChoiceType.MODE` (~40 lines) carries an `id`, a `description` and an `iconKey` that the printed
"choose Khans or Dragons." does not contain — Outpost Siege's golden spells all three — and a
reconstruction built from invented fields is the reversible-but-wrong class in its purest form. A
bare "choose a number." (4 lines) has nothing to say about the bounds, and `0`/`0` would mean "always
zero". `allowedCreatureTypes` (2 lines) wants a capitalized creature-type run with an Oxford "or"
that no vocabulary here has yet.

### What is left in the family

"As ~ enters, …" dissolved as a decline family: 34 of its 231 declined lines now parse and the rest
re-keyed onto whatever follows them. "When ~ enters …" fell 230 → **177 cards** and "~ enters or …"
183 → **21**. Two things account for most of the residue, and neither is a rule this band should have
written:

- **The reveal lands** (~22 lines) — "As ~ enters, you may reveal a Faerie card from your hand. If
  you don't, ~ enters tapped." Secluded Glen, Game Trail, and the Lorwyn and Zendikar duals.
  `EntersTapped` has `payLifeCost` and `unlessCondition` and no third field for this, so it is an SDK
  change and an `add-feature` rather than a row here.
- **"When ~ enters the battlefield, …"** (~90 lines) — the pre-2024 templating, which Wizards never
  re-issued for the cards that still carry it. 194 corpus cards have the phrase and **185 of them are
  Un-sets, Mystery Booster playtest cards, Alchemy or tokens**; the probe says 8 of the ~100 lines in
  this family would parse if the spelling were accepted. A normalization for it would be correct and
  would buy almost nothing, which is why it is recorded here rather than written.

The genuinely open row is the one the ranking now puts first in this family: "When ~ enters **and**
whenever you cast …" and "When ~ enters **and** at the beginning of your upkeep, …" — a join over two
*different* trigger prefixes rather than two self-events, which wants a rule that slots the trigger
vocabulary on both sides.

## The step-trigger band

"At the beginning of **each opponent's end step**, …" — the third row of the tail ranking, and the
fourth time the answer has been *the SDK already factored this and the grammar was calling it with
every argument frozen*. Whole-corpus coverage 8,102 → **8,105 cards** (+3); the baked ledger
7,888 → **7,891 whole** — Goblin Pyromancer, Parasitic Bond and Wanderlust — with **none lost**; the
"At the beginning …"
decline family **197 cards / 106 sole-blocked / 200 lines → 20 / 20 / 20**, and the twenty that remain
are a different construct entirely (`Other enchantments have "At the beginning of your upkeep, …"` —
a *granted* ability, not a prefix). MISMATCH, AMBIGUOUS and redundant readings stay at 0; the
differential's 15 divergences and 3,412 compared cards are unchanged, so nothing here changed what an
already-readable card means.

**The frozen arguments.** `dsl.Triggers.phase(step, player, binding)` is the SDK's one language for
"at the beginning of a step" — its own KDoc says to "reach for this factory for any other combination
of (step, player, binding)" — and `YourUpkeep`, `EachEndStep`, `BeginCombat` and the rest are calls to
it with all three fixed. [`Triggers`](src/main/kotlin/com/wingedsheep/assay/grammar/Triggers.kt) was
calling *the constants*: thirteen whole-prefix rules, one per printed sentence. So "at the beginning
of each opponent's end step" was not a rule nobody had written, it was a **pair of words nobody had
slotted** — the [top-of-library band](#the-top-of-library-band)'s lesson on a `TriggerSpec` instead of
on a `Patterns` recipe.

[`Phases`](src/main/kotlin/com/wingedsheep/assay/grammar/Phases.kt) is that pair of words, and
`Triggers` now slots it exactly once, so the plain step trigger and the graveyard-zoned one ("At the
beginning of your upkeep, **if ~ is in your graveyard**, …" — Ghastly Remains) share every spelling.

**The whose-turn layer is `Player.possessive`, not a table.** Every word this position needs —
"your", "each player's", "each opponent's", "the chosen player's", "enchanted player's" — is already
derived by the SDK so that zone and step descriptions do not each restate it. The rule takes the
`Player` and asks how it is spelled, which makes the grammar and the model one definition; a table
copied in here would agree exactly until someone added a `Player`. What the layer does *not* do is
range over the whole type: `possessive` is total ("target player's", "defending player's") and a step
whose turn belongs to a *target* is a sentence no card writes, so the membership is an explicit list.
**The derivation owns the spelling; the family owns the membership.**

**Three frames, because English has three.** The possessive form is the only one that takes the
whose-turn layer as a slot. `Player.Each` is spelled *both* ways and the majority flips with the step
— "each upkeep" 100:83 over "each player's upkeep", "each end step" 98:23, and 0:13 for the draw
step, where only the possessive form is ever printed — so it is one rule per step with its own
`alsoSpelled` list, exactly as [the top-of-library band](#the-top-of-library-band)'s impulse durations
are. `Player.Each` is consequently absent from the possessive slot, which is what leaves exactly one
rule able to print each of those models. And beginning-of-combat names the *phase*: Oracle puts whose
turn it is in a trailing clause ("combat on your turn", "combat on each opponent's turn"), so
`Step.BEGIN_COMBAT` is absent from the step noun and reachable only there — which is what stops the
possessive frame inventing "your combat".

**The binding is the third argument, and it is the aura frame.** "At the beginning of the upkeep of
enchanted creature's controller" is `phase(Step.UPKEEP, Player.You, ATTACHED)` — the SDK's own worked
example, and what Unstable Mutation's and Lingering Death's goldens write. `Player.You` there is not
an approximation: the binding is what re-scopes "you" to the attached permanent's controller, so an
`ATTACHED` step has no second player it could name.

### What the prefixes read, exactly

| | occurrences | spellings |
|---|---|---|
| read before | 2,572 | 12 |
| read now | **2,720** | **25** |
| still declining | 179 | 20 |

The thirteen new spellings are each opponent's end step and draw step, each player's draw step and
first main phase, the chosen player's upkeep, enchanted player's upkeep, combat on each opponent's
turn, the two attached frames, and four second spellings that parse without printing — "the end step"
(pre-2015 templating for `Player.Each`; Skizzik's golden reads it as `Triggers.EachEndStep`), "each of
your postcombat main phases", "each of your upkeeps" and "precombat main phase".

The 179 that remain are four groups, and only the first is a band:

| what it is | occurrences |
|---|---|
| **delayed triggers** — "the next end step", "your next upkeep", "that turn's end step", "combat this turn" | 132 |
| a non-creature attachment noun — "enchanted **land**'s controller" | 9 |
| an anaphoric controller — "its controller's upkeep", "its owner's upkeep" | 10 |
| not real Magic, or not one step — playtest cards, Archenemy schemes, "each of your main phases" | 28 |

The delayed trigger is a `CreateDelayedTriggerEffect` inside a resolving effect, so it is a *clause in
a sentence* rather than a line prefix, and reading it as an ordinary step trigger would mean a
permanent that sacrifices itself every turn. The attachment noun is printed shape decided by an Aura's
`enchant` line: `normalize/` canonicalizes the attachment *adjective* ("equipped" → "enchanted") and
fixes the noun at "creature", so widening it is a normalization change reaching every "enchanted
creature" in the grammar. Both are write-offs with an expiry date, in the sense
[the conditional-tapped-entry band](#the-conditional-tapped-entry-band) records.

### The payload was the point, and now it is visible

+3 whole cards is what this band *moved*, and it is the [fronted duration](#the-fronted-duration)'s
lesson a second time: **a family that sits at the front of its line measures its payload, not
itself.** The tail ranking keys a line on the text it dies at, so an opening clause is where every
unreadable sentence dies — 197 cards "blocked" were mostly cards whose payload the grammar cannot read
either. The probe said so before the band was written: substituting a known-good prefix into all 200
declined lines leaves **17** parsing.

What changed is that those cards are now keyed on the thing that actually blocks them, and the
ranking says what it is. The family that inherited them leads the table:

```
  cards  sole   lines  tail                       example
  171    79     173    .                          At the beginning of the end step, sacrif
```

— "sacrifice ~." as a bare step, which the grammar reads only as "sacrifice ~ **unless you pay** …".
`SacrificeSelfEffect` beside `SacrificeEffect` is two SDK spellings to classify before that row can be
written, which is the shape
[`Steps.sacrificeFiltered`](src/main/kotlin/com/wingedsheep/assay/grammar/Steps.kt) already records
for the filtered half. Behind it are the triggering **player** as a subject ("that player draws an
additional card" — nearly every each-player step has one, and the grammar has
`Player.TriggeringPlayer` in eight bespoke clauses and no vocabulary), and the delayed trigger above.
Each of those lands on all twenty-five step spellings the day it is written, which is what this band
buys.

### One finding for the engine

`Player.EnchantedPlayer` on a `StepEvent` — the ten Curses that say "At the beginning of enchanted
player's upkeep, …" — is expressible in `mtg-sdk` and is what the grammar reads. The engine's
`TriggerMatcher.matchesPlayerForStep` handles `You`, `Each`, `EachOpponent` and `ChosenOpponent` and
falls through to `else -> true` for everything else, so a Curse's upkeep trigger would fire on *every*
player's upkeep. That is an engine gap rather than an SDK one, so it is reported here rather than
routed around: no Curse has a golden today and none of them reads whole yet, so nothing is currently
relying on it.

## The target quantifier

"Destroy **up to one** target creature." — and every other word English puts in front of "target".
Whole-corpus coverage 8,111 → **8,186 cards** (+75); the baked ledger 7,897 → **7,969 whole** (+72,
with **none lost**); 99 more lines round-trip byte-exact, 17 more normalize as a variant, and 116 fewer
decline. MISMATCH, AMBIGUOUS and redundant readings stay at **0**. The differential compares 25 more
cards (3,765 → 3,790) and its divergences go 35 → 43 — eight new ones, all classified below, none a
parser bug, and the original 35 are the same 35 by name.

Read in two halves: the table itself is +61 cards over two families, and slotting it into the four
remaining target-taking sentences is +14 more. The second half is what tested the first — the claim
that a quantifier is a *row* only means something if a sentence that never had one can take the rows
unchanged, and one of those four (the compound) matched a hand-written card's model byte-for-byte on
the first try.

The family was the top of the tail ranking read five ways: `up to one …` at 174 cards / 99
sole-blocked, `up to two …` at 61 / 38, `Up to two …` at 27 / 25, `up to three …` at 23 / 17 and
`up to X …` at 19 / 12 — five rows of one construct, which is itself the signal that what was missing
was a *position* rather than five rules.

### Six rows, because the noun disagrees in number

Every prefix this grammar has factored so far became a **value in a slot** — the trigger join's
`Prefix`, the fronted duration, `Phases`' whose-turn layer. This one cannot, and the reason is one
word to the right of it. "Up to one target **creature**" and "up to two target **creatures**" put the
noun in different numbers; [`Filters`](src/main/kotlin/com/wingedsheep/assay/grammar/Filters.kt) keeps
its singular and plural as two separately-instantiated cascades because English pluralization is a
table column and not a suffix rule; and a `{filter}` slot is one phrase fixed at declaration time. A
slotted quantifier would have to leave the noun's number undetermined, which is exactly what the round
trip forbids.

So the quantifier is a **row**, a rule that uses it is a *family* of rules, and
[`Targets.quantifiers`](src/main/kotlin/com/wingedsheep/assay/grammar/Targets.kt) is the table:

| printed | noun | requirement |
|---|---|---|
| `target creature` | singular | `TargetPermanent(filter)` |
| `up to one target creature` | singular | …`optional = true` |
| `two target creatures` | plural | `count = 2` |
| `up to two target creatures` | plural | `count = 2, optional = true` |
| `up to X target creatures` | plural | `optional = true, dynamicMaxCount = XValue` |
| `any number of target creatures` | plural | `unlimited = true` |

The rows are exhaustive over the *printed* forms rather than over the SDK's fields. "One or two target
creatures" is a `minCount` below its `count` and is a seventh row nobody has needed yet
(`Combat.returnOneOrTwoTargets` still spells it whole); "target creature an opponent controls" is a
filter, not a quantifier; a `sameController` or a `totalManaValueAtMost` is a rider on the noun phrase
and belongs to a layer above the list, exactly as `Filters`' controller clause does.

### `plural` is one column with two consequences

A plural quantifier is exactly one that admits more than one target. That single fact decides both
halves of the model: the noun comes from `Filters.plural`, *and* the effect is written once per chosen
target (`ForEachTargetEffect` over `ContextTarget(0)`) instead of once against the requirement. A
singular row — bare "target creature", or "up to one target creature", which caps at one and merely
permits none — keeps the `BoundVariable` reference every single-target rule already used. There is no
row where the two come apart, which is why it is one column and not two, and why the
`effectOver`/`memberOf` pair that performs the wrapping lives once beside the table rather than in each
verb.

`up to X` is the row worth a note. `dynamicMaxCount` caps the count and says nothing about the
minimum, so `optional = true` is not redundant beside it — without it an X of zero would fizzle the
cast.

`DynamicAmount.XValue` rather than `CastX` is **not** a majority-spelling choice, and calling it one
would be the wrong lesson to leave behind: the SDK draws a semantic line between the two and calls it
load-bearing. `XValue` resolves from the transient resolution context and is populated only while the
object carrying it resolves; `CastX` is the durable, object-scoped reading that rides onto the permanent
a spell leaves behind. So the right one follows from *where the requirement sits*. A spell effect
(Doppelgang, Icy Blast) resolves with that context live → `XValue`. A trigger whose trigger carries the
announced X — cycling, "when you cast this spell" — is also `XValue`, because `TriggerDetector` routes
the announced `{X}` into the trigger's context for exactly that reason (Valor's Flagship reads it that
way, and Rampaging War Mammoth is the line this row wins). A trigger reading the X off the permanent
*afterwards* — Lost in the Maze's "When ~ enters, tap X target creatures" — must be `CastX`, and
`XValue` there is silently zero. This row only ever lands in the first two positions; a row that could
reach the third would have to translate at the lift, and no `DynamicAmount` exists at all for "the X of
an arbitrary activated ability", so that position needs SDK vocabulary before it needs a template.

Only the *bare* wording maps, for two different reasons. "…, where X is the number of verse counters on
~" defines X from the board rather than from the cost — a different `DynamicAmount` behind a trailing
clause `Amounts` owns. "Tap **X** target creatures", no "up to", declines for a stronger reason: it
means *exactly* X where this row means at most X. The corpus prints 40 such lines and models them with
this very requirement, because `TargetObject.minCount` is a plain `Int` that cannot take a
`DynamicAmount` (Icy Blast's KDoc records the approximation). Reading that wording here would be a
lossy normalization rather than a variant, and giving it a rule of its own would make two printed forms
denote one model — the redundant-reading class the gate holds at zero. It stays declined until the SDK
can tell the two requirements apart.

### Two templates, because English agrees past the noun

Five of the seven verbs spell one template. Two do not: "return target creature to **its owner's
hand**" pluralizes to "… to **their owners' hands**", and the agreement reaches past the noun phrase,
where the `Filters` cascade cannot follow it. So the shape takes a singular and a plural template, and
that is also what made the third spelling cheap — Oracle prints the plural possessive **both** ways,
"their owners' hands" 110 times against "their owner's hand" 55, so the minority is
`PhraseBuilder.alsoSpelled` on the same rule (parsed, never printed) rather than a rule of its own.
Scapegoat's "Return any number of target creatures you control to their owner's hand." reads as a
`VARIANT`: the model survives, only the spelling is normalized.

### Six families slot the table, which is the argument that it is one

`quantifiedPermanentSteps` covers the seven one-verb sentences — destroy, regenerate, exile, tap,
untap, return to hand, put on top of library — and the **pump** sentence is the second, sharing
nothing with them but the noun phrase: it carries a fronted spelling, a stat modifier, and a verb that
agrees in number ("Up to one target creature **gets** +2/+0", "Up to two target creatures **each get**
+2/+1"). A quantifier written into one shape would have had to be written into the other. What the two
share instead is the table and the effect-wrapping pair.

Four more followed, and they are the check on whether a row really is a row:

- the **keyword grant** ("Any number of target creatures **each gain** double strike until end of
  turn." — Phalanx Formation), same agreement as the pump one verb over;
- the **compound** ("Up to two target creatures each get +1/+1 **and gain** lifelink until end of
  turn." — Cutthroat Maneuver, Coordinated Assault, Windborne Charge), where every quantified line the
  corpus prints is *plural*, and where both halves are per-target so the whole composite goes inside
  one iteration rather than the iteration being split in two. Oracle attaches the "each" once to the
  pair, so the second verb is bare "gain";
- **damage** and **counters**, which take only the *singular* rows.

That last pair is the interesting one, because a table that claims to be exhaustive has to be able to
say **which rows a sentence takes**. `Targets.singularQuantifiers` is that declaration, and the
criterion for using it is sharp: a family takes the whole table when its plural changes only the
*noun*, and the singular rows alone when its plural is a **different sentence**. Damage over several
targets is not "deals 3 damage to up to two target creatures" — English writes it "divided as you
choose among …", a different requirement (`DivideDamage`). Counters over several targets is not "put a
+1/+1 counter on up to two target creatures" — English writes "on **each of** up to two target
creatures", the distribute sentence and its own family. Handing those two the plural rows would not
merely win nothing; it would read a distribute model as a sentence that means something else, which is
the reversible-but-wrong class the fail-closed matching exists to catch. So the subset is a
declaration with a reason attached, and the singular rows alone are worth 30 printed lines (19
counters, 11 damage).

Between them the four added **+14 whole cards** (8,172 → 8,186) with the gate still at 0/0/0: Abandon
Reason, Burning Sun's Fury, Chainsaw, Coordinated Assault, Cryogen Relic, Cutthroat Maneuver,
Invigorated Rampage, Karfell Kennel-Master, Press the Advantage, Quickbeam, Stress Dream, The Art of
Tea, Wild Pack Squad, Windborne Charge — and Invigorated Rampage picked the compound up inside a
*modal bullet*, which is the modal band and this one composing with nothing said between them.

### The eight new divergences, classified

| card | what differs | class |
|---|---|---|
| Calamitous Tide, Essence Fracture, Second Breakfast | the card unrolls its multi-target effect as a `Composite` of `BoundVariable("creature[0]")`, `…[1]`; Assay writes `ForEachTargetEffect` | second SDK spelling — majority printed, minority reported |
| Offender at Large | `EventPattern.AnyOf` versus two abilities | the entry band's standing finding, new member |
| Seize Opportunity | a stored-collection *name* (`impulseExiled` vs `exiledCards`) | pre-existing gate gap, exposed |
| **Quickbeam, Upstart Ent** | the trigger filter: Assay reads "another Treefolk you control" as `IsPermanent`, the card narrows it to `IsCreature` | **card bug** — the bare-tribal-noun migration's own class, a leftover |
| **Stress Dream** | `order: ControllerChooses` on the bottom-of-library move, in the sentence's *other* clause | pre-existing `TopOfLibrary` rule gap, exposed |
| **Cryogen Relic** | `Effects.AddCounters("STUN", …)` where `CounterType.STUN` is the string `"stun"` | card-side spelling; harmless, `fromName` uppercases |

The four sentences that took the table added the last three, and **all three agree on the requirement
the quantifier produced** — the divergence is elsewhere in the card every time, which is the evidence
that the rows transplanted cleanly. Quickbeam is the one worth acting on and the strongest of the
three: its effect *and* its `count = 2, optional = true` requirement match Assay byte-for-byte,
including the `ForEach(Targets)` wrapping the whole `Composite` rather than two iterations — an
independent hand-written confirmation of the compound's model — and the only thing left over is a
trigger filter narrowed to `IsCreature`, which means a Treefolk *artifact* entering does not trigger
it. It is not fixed here: it is a card in another set and would pull that set's snapshot into a grammar
PR, which is the same reason the three indexed-unroll cards are named rather than cleaned up.

The first is the interesting one and it is **not** a card bug: `EffectContext.buildNamedTargets`
publishes `"$id[$i]"` for every position of a multi-count requirement, so the indexed unroll is a
supported spelling that resolves to nothing for a position nobody chose. It is a *minority* one — six
occurrences across five sets, against a corpus that writes the iteration everywhere else, including
for "destroy two target lands" — so the grammar prints `ForEachTargetEffect` and the gate names the
three cards. They are a safe mechanical cleanup, and until then the unroll is fixed at the declared
count in a way the iteration is not.

Seize Opportunity is worth stating separately because it is **not** about quantifiers at all: the two
models agree on the "up to two" mode exactly, and differ on the name of a collection its *other* mode
stores. A collection name is arbitrary in the same way a target slot's name is, and
`Differential.normalizeSlots` normalizes the second but not the first. That is a gap in the gate rather
than in either model, and this band is the first thing to have driven a card into it — and then Stress
Dream and Cryogen Relic drove two more, which turns a one-off into a pattern worth naming: **a band
that finishes a card's last declining line inherits every unnormalized field in its other clauses.**
The gap to close is `normalizeSlots`, which already covers target slot names and covers neither
collection names, ability ids, nor the `order` field.

### What the band uncovered, in order

The ranking's five quantifier rows collapsed — `up to one …` from 174 cards to **17**, `up to two …`
from 61 to 22, and `up to three …`, `up to X …` and `Up to two …` off the table entirely. What replaced
them is the payload behind the prefix, which is the product:

| tail family | cards | sole | what it needs |
|---|---|---|---|
| `any number of …` | 123 | 76 | divided damage, distributed counters, players, graveyard-zoned targets — the row is now everything the table's sixth row does *not* reach |
| `, where X …` | 59 | 50 | a trailing clause defining X for a target **count**, i.e. `dynamicMaxCount` fed from `Amounts`' existing vocabulary |
| `one other target …` | 50 | 34 | the `other` modifier, which has two SDK spellings to classify first — `TargetFilter.other()` and the `TargetOther` wrapper |
| `each of up …` | 29 | 16 | "put a +1/+1 counter on each of up to X target creatures" — the distribute sentence |
| `choose up to …` | 28 | 18 | the *modal* quantifier, which `Modal`'s KDoc already records as not a row of its header |

The `, where X …` row is the band's own residue and the cheapest of the five: the prefix now reads and
only the clause that defines the number is missing. `any number of …` is the honest one to read
twice — the table's sixth row cost two lines and finished no card, because the corpus writes that
quantifier almost entirely in sentences no verb here covers. It is in the table because the table
claims to be exhaustive over printed quantifiers, not because it paid.

## The trigger join

"When ~ enters **and** whenever you cast a spell with mana value 5 or greater, draw a card." — Up the
Beanstalk, and the top row of the tail ranking by every column: **177 cards, 88 sole-blocked, 179
lines**. Whole-corpus coverage 8,105 → **8,111 cards** (+6); the baked ledger 7,891 → **7,897 whole**
— Bant, Grixis and Naya Sojourners, Necklace of Girion, Shrine of Burning Rage and Up the Beanstalk —
with **none lost**; 12 more lines round-trip and 13 fewer decline. MISMATCH, AMBIGUOUS and redundant
readings stay at 0. The differential compares one more card (3,764 → 3,765) and confirms it, and its
35 divergences are the same 35 by name, so nothing here changed what an already-readable card means.

**The join is the product that [the entry band](#the-entry-band)'s table deliberately was not**, and
the difference is in the printed sentence rather than in the model. Both produce two
`TriggeredAbility`s from one line. Oracle *contracts* five pairs of self-events into a single trigger
word — "enters or attacks", "attacks or blocks", "enters or dies" — and that is a counted list,
because a cross product of the vocabulary would have been forty-odd rules for five sentences. The
"and" join is different: each half prints **its own trigger word** ("and whenever …", "and at the
beginning of …", "and when …"), so the halves are complete clauses, and one rule that slots the
vocabulary on both sides *is* the family. 112 corpus lines, 31 of them opening with "When ~ enters".

### The prefix became a value

`Triggers` held forty-odd rules of the shape `"$surface, {effect}"`, and there was no way to say "a
trigger's `when` clause" — which is exactly what a join needs twice. Spelling the prefixes a second
time for the join was the one thing this module may not do, so the file now has three pieces where it
had one:

- `Prefix` — a `Phrase<TriggerSpec>` plus the effect cascade its payoff takes. The cascade cannot
  travel *inside* the prefix, because it is a property of the event: a trigger whose event names an
  object of its own reads "it" as that object (`Steps.triggeredStep`) and every other trigger reads it
  as the source (`Steps.step`).
- `sentence(prefix)` — `"{trigger}, {effect}"`, the one fail-closed reconstruction every family now
  shares. It reads the event straight off the ability rather than comparing it against a constant, and
  nothing is lost by that: `TriggerSpec` is exactly `(event, binding)`, so the spec a printed ability
  denotes is *total*, and whether this prefix can spell it is the prefix's own `match` to refuse.
- `event` — the prefixes as one alternation, which is the whole of the join rule.

The rows themselves did not change: `triggerRule`, `filteredTriggerRule`, `slottedTriggerRule`,
`batchProduct`, `nthCastRule` and `countersPlacedRule` all return a `Prefix` now and read the same
surfaces they did. The one trigger sentence that is *not* a prefix plus a payoff stayed a whole-line
rule — the graveyard-zoned step trigger, whose rider lands on the ability's `activeZones` rather than
on the event, so it cannot be joined either.

**The payoff is `Steps.step`, and for a join that is the only sound reading.** Two different events
share one effect, so an "it" resolved to a triggering object would mean a different thing under each
of them. The source anaphor is the one that stays true for both: Hoarder's Overflow's "When ~ enters
and whenever you expend 4, put a stash counter on **it**" means the source under either event.

**A pair the contracted table owns is declined in both directions.** `[EntersBattlefield, Dies]`
prints "when ~ enters or dies" and must not also be printable as "when ~ enters and when ~ dies", so
the guard is on the *pair* rather than on the alternation's position — "one printed form per model"
is a property of the model, and `oneOf` ordering deciding it is the thing invariant 2 forbids. No
corpus line writes a contracted pair the long way, so the guard costs nothing. A join of an event with
itself is declined for the same reason and a simpler one: it denotes two identical abilities.

### The ranking's number-one row was a template literal

This is the part worth more than the +6 cards. A `TemplatePhrase` fails at the start of the literal it
could not match, and the old rules made the whole `when` clause *and its comma* one literal —
`"when ~ enters, "`. So every line that began "When ~ enters" and then did anything else declined at
**offset 0**, and `DeclineKey.TAIL`, which keys a family on the text from the decline onward,
collapsed 179 unrelated lines into one family named after a prefix the grammar had read since Phase 1.
177 cards, 88 of them sole-blocked — the largest row in the table, and not a piece of work at all.

Splitting the prefix off moved those declines to the end of the prefix, where the construct that
actually blocks them starts. The family did not shrink; it **dissolved**, into per-payload rows the
ranking can act on:

```
before                                        after (the same lines, re-keyed)
  cards sole lines tail                          cards sole lines tail
  177   88   179   When ~ enters …               15    4    15    the battlefield, create …
                                                 14    11   14    the battlefield, choose …
                                                 …and ~100 more rows
```

Tail families went 10,086 → 10,202 for that reason. The lesson generalizes past this band: **a rule
whose template swallows a whole clause into one literal makes its declines unreadable to the ranking**,
and the fix is the same one that enables reuse — make the clause a slot. The
[fronted duration](#the-fronted-duration) and [step-trigger](#the-step-trigger-band) bands each found
the tail ranking over-stating a front-of-line family; this one found *why* it happens, and it is a
property of the grammar's own factoring rather than of the key.

### The probe under-stated, for a reason that names its blind spot

The feasibility probe over the family predicted 9 parsing lines and **3** whole cards; the band
delivered 12 and **6**. Only the second time a probe has erred low, and the reason is new: it
substitutes a known-good prefix into the lines of *one family*, so it can only see the payoffs behind
that family's key. Three of the six cards are the Sojourners cycle, whose join opens "When you cycle
~ …" and was keyed elsewhere entirely. **A rule with more than one slot reaches more than one family,
and a family-scoped probe is a floor for it** — the same shape as
[the modal band](#the-modal-band)'s under-statement, where the family was a clause position rather
than a line.

### What is left in the family

82 join-shaped lines still decline, and because the rule reads both halves out of the vocabulary the
residue classifies itself — substitute a known-good half and see which side was the blocker:

| what blocks it | lines |
|---|---|
| the **second** half is an event with no rule | 41 |
| **both** halves read — the payload is the blocker | 20 |
| neither half reads | 16 |
| the **first** half is an event with no rule | 5 |

Seventeen of those 41 are one sentence: "Whenever an enchantment you control enters and whenever you
**fully unlock a Room**, …", whose first half the grammar already reads. That is the next row and it is
one prefix — written once, it lands on every context that slots an event, the join included, which is
precisely what a prefix vocabulary buys. Behind it the second halves are singletons ("when you
sacrifice it", "whenever you expend 4", "whenever it attacks while saddled", "whenever you solve a
Case", "whenever a player taps a Mountain for mana"), and the 20 payload-blocked lines are the ordinary
backlog — amass, seek, conjure, heist, "choose left or right".

One structural gap worth stating rather than discovering twice: the trigger cap rider
("This ability triggers only once each turn.") wraps a **single** ability —
see [the batch-trigger band](#the-batch-trigger-band) — so a two-ability line cannot carry it. Three
corpus joins print it, all of them blocked on their event as well, so it costs nothing today; the fix
is to move the rider up to the line, where it would reach the contracted pairs too.

## The differential gate

`just assay-differential` diffs Assay's reading of a card against the `CardDefinition` a human wrote
from the same text. The touchstone proves a parse is *reversible*; this is the gate that asks whether
it is *right*, and it runs on an asset the incumbent pipeline structurally could not have — the
committed card goldens under `mtg-sets/src/test/resources/snapshots/cards/`, decoded through
`mtg-sdk`'s own `CardLoader`. Reading them is a file read, so the SDK-only dependency rule holds.

**Scoping is fail-closed.** A card is compared only where Assay reads *every* line of it. Comparing a
partially-read card would count a keyword Assay never saw as agreement, so everything else lands in a
named population bucket instead and the denominator stays visible.

```
  Hand-written cards                 9619
    compared                         3412
    not yet covered by the grammar   5524
    script slot not modelled yet     125
    lines do not fold into one card   57
    multi-face (out of scope)        302
    Oracle text differs from golden  199
    golden would not decode            0

  Confirmed — models agree           3397   995.6‰ (99.6%)
  DIVERGENT — read every one           15
```

**Thirteen of the fifteen are one finding, on purpose.** They are the spell-cost band's
`FixedIf…`-against-`OnlyIf` split — seven goldens plus their relatives, diverging because the grammar
emits the gate where the corpus writes both spellings; see [that band](#the-spell-cost-band) for why
the gate is the reading with reach. The fourteenth is Ghoulish Procession's decayed token, an engine
gap the [batch-trigger band](#the-batch-trigger-band) records; the fifteenth is Rakish Scoundrel's
`EventPattern.AnyOf`, the two-spellings-of-one-join finding the
[entry band](#the-entry-band) deliberately did not fold. Nothing else is open: the modal band
took the count off zero with eight of its own and every one turned out to be a **card** bug, all
eight fixed in the same change — as were the four the batch-trigger band's rider exposed.

Zero was never the property. The count goes up every time the grammar reaches a slot nobody had
compared before, and that rise is the gate earning its keep rather than a regression — what matters
is that every divergence is read and classified as parser bug, card bug or fold.

**The last time the count was at zero, the card that had taken it off zero was a card bug.** The Bloomburrow
band's one standing divergence was the already-open `ManaColorSet.Specific` finding, recurring on
Spider Manifestation exactly as the note below predicted it would: "{T}: Add {R} or {G}." written as
one `AddManaOfChoiceEffect` where 165 cards write two abilities. What the finding's own note says
justifies `Specific` is a *rider* the two-ability form cannot express correctly — and Spider
Manifestation's mana line has no rider at all. It was the first of the thirteen to become comparable
and the one that did not belong in the group; it is now two abilities, with the scenario test that
asserts the observable half (activating adds the colour straight into the pool, with no colour
decision to make). The finding itself stands, unchanged and still not folded: the other twelve keep
their riders, and the grammar still never emits `Specific`.

The count was at zero after the sweep and the two findings after it; the spell-cast band took it to
four and three of those were fixed in that band, which is the gate behaving the way it is supposed to
— zero is a checkpoint, not a property. The sweep itself took the count from 122 to 1: every
divergence the gate had
accumulated was read, classified as parser bug / card bug / fold, and acted on. The one it left
standing was Lavaborn Muse, closed by the CR 603.4 split below. What the sweep found, by kind:

- **A bug in the gate itself, and a flaky one.** `AbilityId.generate()` is a global counter and
  `encodeDefaults` is false, so kotlinx re-evaluates the default to decide whether to emit an `id` —
  meaning a golden holding `ability_2` is omitted from the JSON exactly when the counter next returns
  `ability_2`. Two encodes of the same card differed, and the comparison was over the serialized
  *string*, so Blasting Station reported as divergent on some runs and confirmed on others.
  `Differential.sortKeys` compares objects as objects and closes the whole class.
- **22 hand-written cards that were wrong**, all but three of them unreviewed mtgish drafts, and all
  of them wrong in the way this gate is built to see — a clause dropped inside a filter. Fiery
  Cannonade hit every creature rather than every non-Pirate; Eyeblight Massacre every creature rather
  than every non-Elf; Magnetic Flux pumped artifacts *or* creatures rather than artifact creatures;
  Kangee's block trigger pumped every flier rather than every *blocking* flier; Joust Through gained
  3 life where the card says 1; Visara never stopped regeneration at all. Seven more read "sacrifice
  it unless you pay {G}{G}" as `PayCost.OwnManaCost` — the card's *printed* cost, which for the two
  lands among them is `{0}`, i.e. a sacrifice that never happens.
- **Four parser bugs**, each of the reversible-but-wrong class the touchstone cannot see: an
  intervening-if left duplicated in the effect as well as lifted into `interveningIf`; Chromatic
  Sphere read as an instant-speed ability because the mana effect sat under a composite (CR 605.1a
  says "could add mana", not "does nothing else"); a two-pass reading of "creatures you control get
  +3/+3 **and** gain trample"; and two sentences printed in a spelling the corpus never uses.
- **One SDK finding acted on, one filed.** Acted on: `manaAbility = true` now derives
  `timing = TimingRule.ManaAbility` in `CardBuilder`, so the two spellings of one fact can no longer
  drift (24 cards carried only one, and the AI's `ExpiringGrantWindow` branches on `timing`).
- **One engine bug, and the only finding so far whose fix was in neither a card nor a rule: the
  engine never performed CR 603.4's second intervening-if check.** Beastbond Outcaster drew its card
  even when the 4-power creature was killed in response, and nine cards had hand-written a redundant
  resolution-time gate to compensate — a second condition the printed line does not spell, which is
  what made Lavaborn Muse a *divergence* here. See
  [Lavaborn Muse, and the CR 603.4 split](#lavaborn-muse-and-the-cr-6034-split).

The sweep's last pass closed three more, one per kind — a card bug, a scope bug and the third
anaphor — and each is worth reading for its shape rather than its card:

- **Two card bugs, `TriggerBinding.OTHER` for text that says "an artifact you control"** (Donatello,
  Way with Machines and Mm'menon, Uthros Exile). `OTHER` claims a self-exclusion the text does not
  make, and neither card is an artifact, so nothing observable changes *today* — which is exactly the
  reason it survived review, and exactly what "they happen to agree today is not a reason" rules out
  as a fold. The reading only becomes observable if the creature is ever made an artifact.
- **Kalastria Highborn — the gate's scope stopped at the first clause.** "You may pay {B}. If you do,
  target player loses 2 life **and** you gain 2 life." read as `Composite[Gated{LoseLife}, GainLife]`,
  i.e. you gained the life even when you declined to pay. The outer clause-sequence rule took the
  " and " join before the gate could. The fix is structural rather than an alternation reorder: the
  pay-gates are **no longer members of `simpleClause`/`laterClause`**, so nothing can be joined after
  one, and their consequence slots a clause *run* that owns the rest of the sentence. That is one
  reading, not a preferred one — and it is what Oracle templating means, as Extort's own reminder
  text shows ("you may pay {W/B}. If you do, each opponent loses 1 life **and** you gain that much
  life"). The run shape is now `Steps.clauseRun`, shared by the line and the consequence.
- **Tattered Ratter — the third anaphor, and the reversible-but-wrong class again.** "Whenever a Rat
  you control becomes blocked, **it** gets +2/+0" pumped the *Ratter*: `Primitives.self` and
  `SelfSteps.anaphoric` both built `EffectTarget.Self`, and the wrong reading round-tripped
  byte-perfectly. A blanket remap inside `filteredTriggerRule` would have broken "Whenever a creature
  dies, **~** gets +1/+1", because after parsing the two spellings are the same model — the
  distinction only exists at parse time. So the vocabulary is written once as
  `SelfSteps.retargetable` and *instantiated per position*: the source cascade reads both spellings as
  the source, and the filtered-trigger cascade reads the **name** as the source and the **pronoun** as
  `EffectTarget.TriggeringEntity`. Disjoint surfaces, disjoint models, nothing for the printer to
  choose. `Steps.Cascade` is the shape both instances share, so `Steps.step` is not duplicated — only
  the dozen combinators above the atoms are built twice, and every leaf is shared. 545 filtered-trigger
  lines in the corpus spell "it" in that position; the gate's round-trips rose by 4 and its
  alternate-spelling count fell by the same 4, which is the pronoun becoming canonical for its own
  model.

### Zombie Master, and the 103 cards behind it

The last card bug the sweep closed was the longest-standing one, and it is the clearest example of
what this gate is *for*. `Filters.bareSubtype` read a bare tribal noun ("Zombies") as a **creature**
filter. A bare creature-type noun actually names every *permanent* with the subtype — the adjectival
"Zombie creatures" is what narrows it — and Zombie Master proves the distinction is deliberate rather
than stylistic by printing both spellings on one card, with the ability its bare-noun line grants
spelled "Regenerate this **permanent**".

Flipping the one `build` took the differential from 2 divergences to **104**, which is why it had
been reverted twice before: 103 hand-written cards spelled the bare noun as a creature filter, and
for almost all of them the two select the same permanents. That is precisely why it survived review —
an error that is unobservable on the cards that carry it is invisible to everything except a
differential.

It landed as a card migration *first*, then the grammar line, with the differential as the check at
every step: 104 → 26 → 4 → 1. The residue at each stage was the interesting part, because the cards
that did not fall to the mechanical edit were the ones spelling the filter some other way — and
three of those turned out to be **gaps in the SDK's own vocabulary**, with no way to write the
bare-noun reading at all:

| Added | For the printed form |
|---|---|
| `DynamicAmounts.permanentsWithSubtype` | "the number of **Slivers** on the battlefield" |
| `Conditions.ControlPermanentOfType` | "if you control a **Rabbit**" |
| `TargetFilter.PermanentInYourGraveyard` | "target **Zombie card** from your graveyard" |

Each sits beside its creature-scoped twin, and the reference doc now carries the table that says
which to reach for. A reading the SDK could not express is the finding this module exists to
produce; that it took a card migration to surface three of them is the argument for running the
migration rather than filing the divergence.

Twelve cards needed per-occurrence care rather than a blanket edit, and Kavu Monarch is the one to
know: "Kavu **creatures** have trample" and "whenever another **Kavu** enters" are two filters in one
card, and only the second moves. A replace-all would have round-tripped byte-perfectly and been
wrong — the same class the gate exists to catch, reintroduced by the fix for it.

### Lavaborn Muse, and the CR 603.4 split

The last one to fall was the one the gate had been *waiting* on, and it is the only divergence so far
whose fix was in the engine rather than in a card or in a rule. Lavaborn Muse carried its
intervening-if twice — once as the trigger's condition and once as a `ConditionalEffect` around the
effect — because the engine checked the condition only at trigger detection, so a card that wanted CR
603.4's second check had to hand-write it. That second copy is a condition the printed line does not
spell, which is what made it a divergence rather than only a rules bug, and the grammar was right
both times: Phage the Untouchable, which carried *only* the condition, was reported for the mirror
reason.

The engine fix split the overloaded field into `interveningIf` (CR 603.4 — checked when the trigger
would fire and again on resolution) and `triggerRestriction` (CR 603.2 — checked only when it fires,
which is what "Whenever this creature attacks *while* you control a Dinosaur" means, and what Burning
Sun Cavalry and Seasoned Warrenguard have scenario tests asserting). Re-read against the printed text
rather than against the field, the corpus's 510 sites are **377 intervening-"if" / 44 "while" / 89
other trigger-time restriction**, and the Comprehensive Rules overruled the first reading five times
— Offspring, Soulbond, Suspend, Impending and Gift all print an "if" that looked like a mechanic gate
(CR 702.175a, 702.95a, 702.62a, 702.176a, 702.174b), while max speed is "*as long as*" (702.178a) and
is therefore the opposite.

With the second check in the engine, all nine compensating gates are deleted — Lavaborn Muse,
Farsight Mask, Bloodhall Priest ×2, Asylum Visitor, Heir of the Wilds, Convalescent Care, Oversold
Cemetery and Edgar Markov — and the two models agree. `Triggers.abilityFor` writes `interveningIf`
and never `triggerRestriction`, so a "while" card declines rather than printing an "if" sentence that
means something else.

**The differential was at zero here, and the spell-cast band moved it off — exactly as this
paragraph predicted.** Four divergences over 46 newly-compared cards, three of them fixed on the spot
(one parser bug, two card bugs) and the fourth — Spider Manifestation, under the standing
`ManaColorSet.Specific` finding — read afterwards and fixed as a third card bug, since that card's
mana line carries none of the riders the finding says the type earns its place on. Zero is a
checkpoint, not a destination: it means every card the grammar reads whole agrees with its golden on
the day it is measured, and the next band of rules is expected to move it off again.

The divergence count is not meant to stay at zero — it rises every time the grammar reaches a new
class of card, and each rise is the gate earning its keep. The five it opened with, the eight the
first band of spell rules produced, and the six the land band produced are all fixed or classified
below. The aura band is the first new card class that added **none**: the 40 auras it brought into
the population all agreed, and a by-hand sweep of every golden printing one of those three sentences
found no disagreement either. That is a fact about the cards rather than about the gate — an aura in
this band is two lines with nothing to drop, where the bugs the gate has found were all a clause
lost *inside* a filter on a longer sentence.

The **Portal band** took the compared population from 930 cards to 1,636 and the divergence count
from 6 to 30, which is the ratio the gate is supposed to have: six new cards read for every one that
disagrees. All thirty are classified below and they fall into six families, four of which are the
already-open "two SDK spellings of one thing" findings. Two are new bugs in hand-written cards, and
one was a bug in the parser that only this gate could have caught.

Five separate things have to hold before a card is compared, and each has its own bucket. Every one
of them was added after the gate was caught claiming a check nobody had performed:

| Guard | Why |
|---|---|
| Assay reads every **line** | A keyword whose line declined would look like agreement. |
| The text is the **same text** | A golden carries the wording it was authored from; if that is not what Scryfall serves, Assay is reading one card and diffing another. Compared normalized, so inconsistently-included reminder text is not a difference. |
| The definition uses only **modelled slots** | A keyword the SDK lowers to a triggered ability at authoring time leaves content the grammar cannot produce. Confirming it would claim a check nobody performed. |
| The lines **fold into one card** | A `CardScript` has one `spellEffect`; a card printing two effect paragraphs means a sequence the grammar has no rule for. Neither keeping the first nor concatenating them is honest, so it is counted. |
| The card has no **unread abilities** | A keyword the SDK lowers at authoring time puts an ability in the script that no text line prints — prowess, provoke, rampage, training and mobilize become triggers; cycling, equip, morph and level up become activated abilities. A card carrying more of either than Assay read is carrying content nobody printed. One-directional: *Assay* having more would mean the grammar invented an ability, and that must diverge loudly. |

A divergence never fails the build — it is a finding to classify as **parser bug**, **card bug**, or
**fold**. Only an undecodable golden exits non-zero. The fold list lives in `gate/Differential.kt`
and is reviewed rather than grown silently: every entry is a divergence the gate stops reporting, so
each one has to say why it is not a difference.

The **Legions band** took the compared population from 1,636 cards to 2,387 and the divergence count
from 30 to 122 — about nineteen new cards read for every one that disagrees, which is the ratio the
gate is supposed to have. All 122 fall into eleven families, and every family is classified below;
seven of them are the already-open "two SDK spellings of one thing" findings recurring in new
sentence shapes, three are new inconsistencies of the same kind, and one is a bug in a hand-written
card.

The band also found the gate lying to itself for the **fifth** time, and the first time in the
*other* direction: it reported six Sliver lords as divergent over an `AbilityId`. Ability ids are
canonicalized by position, but only in a card's top-level ability lists — and a `GrantTriggeredAbility`
carries a whole ability *inside* a static, one level further in. The gate was comparing a counter.
Found the way all five were, by running it on a card class it had never reached.

### What the gate has found

- **A card bug of the Meteor Golem class, from the Legions band.** **Flamewave Invoker** prints
  "{7}{R}: Flamewave Invoker deals 5 damage to **target player or planeswalker**" and declares a
  plain `TargetPlayer`, so the ability cannot be pointed at a planeswalker at all. Its own
  `oracleText` carries the clause it does not implement, which is the same signature every card bug
  this gate has found has had. Not fixed here — a card fix wants its own change and the scenario test
  that asserts the negative — but it is the highest-value thing in the band's output.
- **Legions' remaining 121 divergences, by family.** Each is a spelling difference the SDK permits in
  two ways, and the grammar emits one of them per the module's rule:
  - **A created token's art (19 cards).** `CreateTokenEffect` carries an `imageUri` no printed word
    determines; a rule that invented one would be inventing a URL. The text round-trips perfectly and
    the field is simply not in it.
  - **Mana-ability-ness (17).** The already-open finding, recurring: cards that set `isManaAbility`
    and leave `timing` at its `InstantSpeed` default. The band widened the *derivation* to match CR
    605.1a — "Add one mana of any color" and "Add three mana in any combination of {R} and/or {G}"
    are mana abilities as much as "Add {G}" is, and reading only the two symbol effects had made
    Blood Celebrant, Goblin Clearcutter and Wirewood Channeler instant-speed abilities that use the
    stack. Chromatic Sphere remains, because its mana step is inside a composite.
  - **"You may" on a triggered ability (~10).** `optional = true` versus a `MayEffect` wrapping the
    effect. *Since resolved by removing the flag from the SDK — see the closed finding below.*
  - **A mass effect written as a pipeline (~19).** `ForEachInGroup` versus a `Patterns.Group` recipe
    for the same sweep, and the already-documented `DealDamage(n, PlayerRef(Each))` versus
    `ForEachPlayer` split for "each creature and each player".
  - **A tribal noun's card type (~11).** "a Zombie card" is `Any.withSubtype(Zombie)` on Corpse
    Harvester and `Creature.withSubtype(Zombie)` in the grammar, which reads the bare noun as a
    creature — right in "target Sliver" and stricter than the text in "a Zombie card". The same split
    shows on the group sweeps whose filter omits `IsCreature`.
  - **`TargetCreatureOrPlaneswalker` versus the general filtered target (3).** The standing finding
    below, recurring in three new sentence shapes, still not folded and for the same reason.
  - **A `Gate.MayPay` cost's atom (6), a `GrantDynamicStatsEffect` holding a fixed bonus (3), a
    `descriptionOverride` (several), an explicit `fromZone` on a move that does not need one (2), and
    `ForceSacrificeEffect` versus `SacrificeEffect` for a bare "sacrifice a permanent" (1).** Each is
    one concept with two spellings and neither is broken; the grammar emits the one whose model says
    what the sentence says.
  - **Phage the Untouchable, on its own.** The band taught `Triggers` to read an intervening-if the
    way CR 603.4 defines it — a condition printed between the event and the effect is checked twice.
    At the time the engine checked it only once, so a card that wanted both checks had to carry the
    condition *and* a `ConditionalEffect`, and Phage carried only the condition. The CR 603.4 split
    settled it in the grammar's favour: `interveningIf` is now both checks, the compensating gates
    are deleted, and Phage was never wrong — the engine was.
- **Two more bugs of the Meteor Golem class, from the Portal band.** **Recollect** prints "Return
  target card from **your** graveyard to your hand" and filters on `TargetFilter.CardInGraveyard`,
  which is *any* graveyard — so it can be pointed at an opponent's. **Eternal Witness** is the same
  card text inside an enters trigger and has the same filter. Elven Cache and Déjà Vu, the other two
  cards printing that sentence, both scope it with `ownedByYou()`, which is what makes these two a
  bug rather than a spelling. Both are generated renders that dropped a word, and both carry the
  clause they do not implement in their own `oracleText`. Fixed, each with the scenario test that
  asserts the *negative* — a card in an opponent's graveyard is not a legal target — which is the
  half that fails without the fix.

  A grep for the same filter found a third of the class the gate could not have: **Revive** prints
  "Return target **green** card from **your** graveyard to your hand" and filters on the unowned,
  uncoloured `CardInGraveyard`, so it ignores both words. It is not fixed here because the grammar
  declines its line — no rule reads a colour on a card noun — so the differential never compared it,
  and a fix wants the rule first so the gate can confirm it.
- **A parser bug of the reversible-but-wrong class — found, and fixed.** "Untap target creature. It
  gets +2/+4 until end of turn." read "it" as the *source* rather than as the target the first clause
  chose, because the same four words mean the source in "Whenever this creature attacks, it gets
  +2/+0". The line round-tripped byte-perfectly the whole time and meant a different creature, which
  is exactly what the touchstone structurally cannot see. The fix is the split between
  `SelfSteps.anaphoric` and `Continuations`: an anaphor resolves to the most recently mentioned
  object, so once a clause has introduced a target the pronoun is that target, and the two
  vocabularies are reachable from disjoint positions so no text has both readings. It affected
  Inspirit and Gerrard's Command.
- **"Deals N damage to each creature and each player" has two SDK spellings, and eight cards use the
  minority one.** `DealDamage(n, PlayerRef(Player.Each))` and
  `ForEachPlayerEffect(Player.Each, [DealDamage(n, Controller)])` are equivalent for a fixed amount —
  the second rebinds a controller the sentence does not need. Earthquake and Hurricane write the
  first and are confirmed; Fire Tempest, Howling Gale, Volcanic Spray, Magma Giant, Devastate,
  Hammerfist Giant, Rain of Embers and Steam Blast write the second and diverge. The grammar emits
  one, per the rule that two SDK spellings get one rule and a finding. Nothing is broken; it is one
  `Effects` helper away from the corpus having a single spelling.
- **A nested plain `CompositeEffect` is the same sequence as its flattening — folded, narrowly.**
  Cruel Tutor nests `Patterns.Library.searchLibrary` inside its outer composite and Bitter Revelation
  splices the same recipe's steps into a flat one, for the same printed sentence; Angelic Blessing's
  "gets +2/+2 and gains flying" is a two-element composite alone and three flat elements once a
  "Scry 1." follows it. A composite with `stopOnError` false and no description override is an
  ordered run and nothing more, so the two are one value written two ways. The fold splices only that
  shape and never reorders, so `[a, [c, b]]` still disagrees with `[a, b, c]`.
- **A bug in a hand-written card — the outcome this whole thing is for.** Meteor Golem's printed text
  is "destroy target nonland permanent **an opponent controls**"; its definition filtered on
  `TargetFilter.NonlandPermanent`, so the golem could be pointed at its own controller's board and
  the engine offered those permanents as legal targets. A generated render that dropped a clause,
  committed and unnoticed. Fixed with a scenario test asserting the negative — a permanent you
  control is **not** in `legalTargets` — which is the half that fails without the fix.
  This is also the answer to "why not just diff the printed text": nothing about the card *looked*
  wrong, and its own `oracleText` field carried the clause it did not implement.
- **Two more of exactly that class, from the land band's first run.** Opening `activatedAbilities`
  took the compared population from 653 cards to 890 and immediately found both:
  **Voltaic Construct** prints "{2}: Untap target **artifact creature**" and filtered on
  `TargetFilter.CreatureOrArtifact` — an `Or` where the text is a conjunction, so it untapped any
  creature *or* any artifact, a strictly larger set than the card allows. **Dwarven Miner** prints
  "{2}{R}, {T}: Destroy target **nonbasic** land" and filtered on `TargetFilter.Land`, so it destroyed
  basic lands. Both are generated renders that dropped a clause, both were committed with their own
  `oracleText` carrying the clause they did not implement, and both are fixed with a scenario test
  asserting the *negative* — the permanent the text excludes is not in `legalTargets` — which is the
  half that fails without the fix. Three such bugs in three new card classes is the pattern the gate
  predicted: a divergence appears the first time the grammar reads a class, not later.
- **A parser bug of exactly the class this gate exists for — fixed.** Assay read "protection from
  black and from red" as one `Protection(Colors([BLACK, RED]))`; the cards spell it as two
  `Protection(Color)` abilities. **The cards were right** — CR 702.16g: *"'Protection from [quality A]
  and from [quality B]' … behaves as two separate protection abilities."* The reading round-tripped
  perfectly and meant the wrong thing, so the touchstone could never have caught it. It affected
  Paladin en-Vec, Sabertooth Nishoba and Akroma, Angel of Wrath. The fix is `Keywords.qualityRun`: a
  rule that denotes *several* abilities from one phrase, which is why a keyword line now parses as a
  list of **groups**. It generalized while it was at it — the join is over any quality, not just
  colours, and CR 702.11f gives hexproof the same shape, so "protection from Demons and from
  Dragons", the Oxford-comma three-way, and "hexproof from white and from black" all read now.
  `ProtectionScope.Colors` is consequently a scope the grammar never emits.
- **A second SDK spelling that could not have worked — deleted.** `KeywordAbility.Flanking` (a
  `data object`) and `Simple(Keyword.FLANKING)` both existed and were not equal; the cards used the
  second and the grammar emitted the first. The engine reads flanking off the *projected keyword
  set* (`TriggerAbilityResolver` synthesizes the trigger for anything with `Keyword.FLANKING`), and
  the object overrode no `keyword`, so it never reached `CardDefinition.keywords` — a card authored
  with it would have printed "Flanking" and done nothing. No card used it. It is gone from `mtg-sdk`.
- **Two implementations of Affinity in the corpus.** Frogmite spells it `KeywordAbility.Affinity`;
  Qumulox, Memory Guardian and the five Darksteel golems hand-roll the same text as a
  `ModifySpellCost` static ability. Both work — this is an inconsistency, not a bug — but it is the
  same "one concept, two spellings" family, and it is why those cards sit in the
  `script slot not modelled yet` bucket rather than being compared.
- **The gate lying to itself, for the third time.** The slot-name normalization — which exists
  because the string linking a requirement to the effect reading it is arbitrary — was a textual
  replacement over the serialized script, and the grammar's slot is called `target`, which is *also*
  the name of a field on every targeted effect. So `"target":{…}` was rewritten to `"slot_0":{…}` on
  Assay's side and left alone on any card that had named its slot something else, and six cards
  reported as divergent over a difference that was in neither model. It now walks the JSON tree and
  rewrites only `id` / `name` *values*. Every one of the three has been the gate finding a way it
  could have lied; none was found by reading the code.
- **The gate lying to itself, for the fourth time — the slot fold was scoped to the wrong thing.**
  The positional-reference fold below numbers a script's target slots so `ContextTarget(0)` and a
  named requirement compare equal, and it numbered them *card-wide*, starting at the root. But a
  `ContextTarget`'s index counts within its own **owner** — a `CardScript`, a `TriggeredAbility` and
  an `ActivatedAbility` each declare their own requirements — and a card-wide counter that never
  descended into an ability simply stopped. It agreed with every card for as long as the grammar
  produced only top-level requirements; Trench Wurm, whose whole script is one activated ability with
  a positional target, is what a card looks like when it stops agreeing. Numbering is now per owner.
  The pattern holds: each of the four was found by *running* the gate on a new card class.
- **A positional target reference and a named one — folded, with the SDK's own words for it.**
  Murderous Compulsion and Ureni's Rebuff refer to their target as `ContextTarget(0)` against an
  unnamed requirement; Assay always mints a name. The SDK documents `BoundVariable` as "safer and
  more self-documenting than `ContextTarget(index)`" — the same link written by name instead of by
  position — so the comparison now normalizes both to the requirement's *position*, and what it
  compares is which requirement an effect reads. `ContextTarget(1)` still diverges from anything
  reading slot 0.
- **Open, and not folded: `TargetCreatureOrPlaneswalker` versus the general filtered target.** Hero's
  Downfall spells "target creature or planeswalker" as a dedicated requirement type; 29 other card
  sites spell it as `TargetObject(CreatureOrPlaneswalker)`, and 219 cards in the corpus print the
  phrase. Both are fully wired, down *parallel* code paths — `TargetFinder` hand-rolls the
  hexproof / shroud / can't-be-targeted checks separately for each. That parallel implementation is
  the reason this is not folded: the two agreeing today is an accident of two code paths, not a
  stated equivalence, and folding would stop the gate from noticing if they drift. Unifying them is
  an SDK cleanup with 17 card sites behind it. The damage rules added a second instance — Sear —
  which is what a standing finding is supposed to do: it recurs, in a new sentence shape, unchanged.
- **Open: "you" has two spellings, and asymmetric facade defaults are why.** Nightdrinker Moroii
  writes "you lose 3 life" as `EffectTarget.PlayerRef(Player.You)`; 116 other sites write it as
  `EffectTarget.Controller`, against 20 for the `PlayerRef` form, and the grammar emits the majority.
  Both resolve to `context.controllerId` in the *same* `when` in `TargetResolutionUtils`, so nothing
  is broken — but they are **not** interchangeable in general: the entity resolver in the same file
  handles `Controller` and falls through to `null` for `PlayerRef(You)`. That asymmetry is why this
  is not folded; a fold here would have to be scoped to player-directed effects to be true.
  The mechanism that produced the split is worth naming: `Effects.GainLife` defaults its target to
  `Controller` while `Effects.LoseLife` defaults to `PlayerRef(Player.TargetOpponent)`, so an author
  writing "you gain" takes the default and one writing "you lose" must override — and reaches for the
  shape the signature showed them.
- **Still open: `ProtectionScope.Colors` is one of those spellings.** CR 702.16g defines the joined
  text as two abilities, which is how all but one card in the corpus writes it — Ureni, the Song
  Unending uses `Colors`. The scope is engine-supported (`CardEntityFactory`, `PlayerProtectionRules`)
  so nothing is broken; it is one card and one type away from the corpus having a single spelling.
- **Closed, by deleting the field: a trigger's "you may" said itself twice.** `TriggeredAbility`
  carried an `optional: Boolean` beside its effect, and 106 cards used it where 214 wrapped the
  effect in a `MayEffect` — one sentence, two SDK spellings, bridged here by a `liftTriggerConsent`
  fold. The fold's own justification was the argument for removing the flag: it cited
  `TriggerProcessor.putOnStack` *building* `GatedEffect(Gate.MayDecide, then, otherwise)` from the
  flag on every game, which is a lowering, not an equivalence someone asserted. So the flag went and
  the gate is the model; `optional = true` survives only as a DSL shorthand that lowers in `build()`.
  Both halves of `Triggers.abilityFor`/`scriptFor` lost their lowering, `Granted` lost the same three
  lines, and the fold was deleted. The divergence count did not move, which is what proves the fold
  was folding nothing but the spelling. Two further conflations came out with it, both engine-side:
  a targeted "you may" used to carry its consent by forcing every target slot's minimum to zero
  (so "target creature" silently became "up to one", against CR 603.3d), and the single-legal-player
  target auto-select had to be disabled for optional abilities to stop that consent being skipped.
- **Open: a mana ability says so twice, and 24 abilities say it once.** `ActivatedAbility` carries
  `isManaAbility: Boolean` *and* `timing: TimingRule.ManaAbility`, and `TimingRule.ManaAbility`'s own
  KDoc claims the rules meaning — "does NOT go on the stack (Rule 605.3a)", "can be activated during
  mana payment even without priority" — that the engine actually implements off `isManaAbility`
  everywhere. 620 hand-written mana abilities set both; 24 set only `isManaAbility`, because
  `activatedAbility { manaAbility = true }` sets that flag and leaves `timing` at its `InstantSpeed`
  default. The grammar derives both from CR 605.1a and emits the majority, so Bog Initiate, Wirewood
  Elf and Elvish Aberration diverge. Nothing is broken — every engine read is on `isManaAbility`, and
  the one site that tests `timing == InstantSpeed` (the AI's `ExpiringGrantWindow`) returns early on
  `isManaAbility` first — but this is **not folded**, because the two fields agreeing is a property
  of how cards happen to be authored rather than a stated equivalence, and an ability with
  `timing = ManaAbility` and `isManaAbility = false` would print as a mana ability and use the stack.
  Folding would stop the gate noticing that.
- **Open: "enchanted creature", "enchanted land" and "equipped creature" are one model.**
  `GroupFilter.attachedCreature()` is `GameObjectFilter.Permanent` scoped to `AttachedTo` — it says
  *the thing this is attached to* and nothing about that thing being a creature, or about the
  attachment being an Aura. So all three printed forms denote the identical value, and registering
  more than one rule for it would be genuine ambiguity: several printed forms, one model, nothing
  for the printer to choose. The grammar spells exactly one, the noun nearly every card uses, and
  the others decline. This is not an SDK gap — the model is right, and which word a card prints is a
  function of its *type line*, the same class of printed-shape information as the self-reference
  noun ("this creature" vs "this Equipment") that `Normalizer` already records and restores. When
  the equipment forms are read, they belong in that pass and not as a second rule.
- **`mtg-sdk` has no `Statics` facade.** `dsl` publishes `Effects`, `Triggers`, `Costs` and
  `Conditions`; static abilities have nothing, and hand-written cards construct them directly
  (`staticAbility { ability = ModifyStats(1, 2) }`). The constructor is the curated surface here, so
  `Statics` builds through it — the same situation `Replacements` is in with `EntersTapped`. Two
  facades missing for the two ability kinds that never got one is a small, consistent SDK finding.
  Related, and worth knowing before reading a golden: **two SDK types share
  `@SerialName("ModifyStats")`** — the `StaticAbility` and `ModifyStatsEffect`, the effect behind
  "Target creature gets +3/+3 until end of turn." Different polymorphic hierarchies, so nothing
  clashes, but one card's JSON can show both under one type name.
- **Open: `ManaColorSet.Specific` is a second spelling of a dual land's line.** 165 cards write
  "{T}: Add {B} or {G}." as two `AddManaEffect` abilities sharing a cost, and a much smaller group —
  13 when this was written, 16 goldens today — write it as one
  `AddManaOfChoiceEffect(ManaColorSet.Specific(...))`. Unlike the other entries in this list the
  split has a *reason*: every card in the smaller group carries a rider the two-ability form cannot
  express correctly — "Activate only once each turn" on two abilities permits two activations — so
  the type earns its place. The grammar emits the majority and never emits `Specific`, and none of
  the smaller group is compared today because each one's rider declines anyway.

  **The divergence it threw off is also its membership test.** Spider Manifestation was in the
  smaller group with a bare "{T}: Add {R} or {G}." and no rider on it at all — so its line parsed, so
  the card was compared, so it diverged, and the finding's own reason for the type is what says it
  belonged in the majority. It is now two abilities. Generalized: **a card in the smaller group whose
  mana line *reads* is a card in the wrong group**, because a rider is exactly what makes the line
  decline. That test costs nothing to run — it is the differential, unchanged — and it is why the
  finding is worth leaving open rather than folding.

And the gate paid for itself before its first report: writing it surfaced that "Plains"
de-pluralized to `Subtype("Plain")` — the "Elves" → `Elve` failure, live on the basic land types,
round-tripping perfectly the whole time. `Primitives.pluralSubtype` now ranks candidate readings
against the SDK's own type lists instead of guessing. Running it then surfaced the join and
slot-completeness holes above, each of which was the gate finding a way it could have lied.

## The compiler and the custom-card sandbox

`assay compile` takes a reading the whole way: Scryfall JSON in, a `CardDefinition` out.

```bash
just assay compile "Serra Angel"       # a corpus card
just assay compile --file card.json    # a card that has no Scryfall entry at all
```

The Scenario Builder is where that becomes useful. Its **Custom cards** panel (dev endpoints only)
takes a pasted card object, shows what Assay read — each printed line with its verdict, the
canonical spelling where the author wrote a legal variant, and the caret on the token a decline died
on — and then lets you put the compiled card into any zone and *play* it. The question Assay was
built to answer becomes something you can hold: **is this card expressible in Argentum's vocabulary,
and what exactly does it say?**

Four constraints keep this from being the card loader this module refuses to be, and all four are in
code rather than in a convention:

- **Dev-gated.** `AssayCardService` reads `game.dev-endpoints.enabled`, and the player-facing
  `/api/scenarios` is gated by the same service rather than by a second check.
- **Session-scoped.** The compiled card goes into a `CardRegistry` overlay for that one scenario —
  never the live corpus, never a deck, never another game. Drop the source and the name stops
  resolving, which the tests pin.
- **Whole cards only.** A card any of whose lines Assay cannot read is *refused*, with the line that
  stopped it. Nothing here can produce a card missing an ability, which would look right on the
  board and test green.
- **The corpus is still hand-written.** Ground truth stays a `cardDef` with a passing scenario test.
  Nothing loads `mtg-sets` through this, and the module's own dependency is still `:mtg-sdk` alone.

Two things the compiler does that the grammar deliberately does not. It reads the **header** —
mana cost, type line, power/toughness, loyalty, defense — which is not Oracle text and which no rule
touches; a `*` power declines rather than becoming 0, because mapping a characteristic-defining
ability into the stat slot is grammar work nobody has done. And it **re-mints ability ids**: the
grammar mints one fixed constant per family (no printed word determines an id, and the differential
normalizes by position), but a played card is dispatched on those ids, so two abilities sharing one
would activate the wrong ability.

At today's fineness — the "cards fully covered" line in `just assay-report`, well under a fifth of
the corpus — pasting a *random* real card more often declines than compiles. That is the tool working: the decline names the missing capability, and it
is the same ranked backlog `assay-report` produces. A custom card written in canonical templating
inside a covered family compiles, and one that does not is usually a card to reword.

## The explorer

`just assay-explore` serves the whole module in a browser: the fineness numbers, the ranked
declines with the cards behind each one, every card's reading beside its printed text, the wired
grammar, the differential, and a box for text that was never printed.

**It runs against the grammar on this classpath, not a snapshot of it.** That is the difference
between this and the [mtgish model explorer](https://github.com/i5jb/mtgish) it is modelled on: that
page had to precompute its data and ship the parser as WebAssembly, because the parser was Go in
another repository and the page could not call it. Assay is ours and already linked, so a rule you
just edited is one restart away from being re-measured, and a custom card runs the identical
[`Touchstone`] path a corpus card runs — normalization, self-reference abstraction, reminder
stripping and the invertibility check included — instead of an approximation of it.
`com.sun.net.httpserver` is in the JDK, so the SDK-only dependency rule is untouched.

Three things it shows that no CLI report does:

- **Which cards are behind a decline family**, and how many of those already have a hand-written
  golden. `assay report` ranks the families; clicking one is the backlog it names, split into the
  grammar gaps (answer already written, differential confirms it the moment it parses) and the
  possible SDK gaps.
- **All three rankings side by side.** Keying declines by the token a line *died on* answers "what is
  the grammar missing"; by the **sentence shape** — the line with numbers and mana symbols collapsed
  — answers "which whole sentence needs a rule"; and by the **parse's tail** — the text from the
  decline's own offset on, cut to three words — answers "what construct would let these lines get
  further", which is the one that decides work. They disagree sharply, and the page says why.
  `assay report --rank <token|shape|tail>` prints the same three tables.
- **Whether writing a family would actually finish its cards.** The sole-blocked count says which
  cards a band *reaches*; the probe on a family's page substitutes a known-good prefix for the
  family's own span, re-parses every declined line of every card behind it on the live grammar, and
  says how many whole cards come into coverage. Landfall reads 189 cards blocked and 104
  sole-blocked, and the probe says **48**. That gap is why every band picked without this step has
  been overstated.

The corpus sweep (~5s) runs in the background at startup, so the live parser and the rule tree are
usable before the numbers land; the differential runs on first request and is then cached, because
it decodes 8,874 goldens and most sessions never open it.

**Rule usage numbers are exact rather than indicative.** The kernel records no parse provenance — a
reading is a value, and the rule that produced it is gone by the time the gate sees it. But the
*print* side is deterministic: `oneOf` prints through the first canonical alternative that can
express the value, so "which rule printed this" has one answer, and it is the same walk the round
trip depends on. A rule showing no usage printed nothing in 34,882 cards.

### The explorer inside the app

`game-server` mounts the same page and the same handlers under `/api/assay/explorer`, so the web
client's **Set Completion** view offers it as a tab beside the coverage grid. That is one `<iframe>`
and no second implementation: `explore/ExploreApi.kt` holds every route's behaviour and both servers
— `assay explore`'s loopback [`ExploreServer`] and the Spring controller — are reduced to moving
bytes. A React port of these views would have been free to drift from the gates it displays, which is
the thing "a view, never a second source of truth" exists to prevent.

Unlike the custom-card sandbox next door, the tab is **not** gated. It is a read over public card
text — no state a request can mutate, no game, no account, no corpus write — so the thing to weigh is
resources, not exposure, and the sweep is already lazy: `ExploreApi` is built and its sweep started
on the *first request*, so a server nobody opens it on fetches nothing. Where `~/.cache/scryfall`
does not exist, that first sweep downloads the bulk; where it cannot, the page stays up with its
live parser and rule tree and reports the failure. The differential needs `mtg-sets` test resources
and so answers "no goldens found" off a bootJar — one page degraded, the rest intact.

## The verdict ledger

`just assay-bake` writes `game-server/src/main/resources/coverage/assay-verdicts.json`: one sorted
line per card, saying whether [`CardCompiler`] reads it **whole** and, if not, the decline that
stopped it and the printed line that decline points at. 6,979 of 34,882 cards, at the time of
writing.

It has two readers, and the second one is why the format is what it is.

**The Set Completion view** joins it per card, which turns the *missing* half of that page into a
ranked backlog: a card nobody has authored that Assay already reads end to end needs no new grammar
and no new SDK vocabulary, so it is the cheapest work on the board. The page badges those cards,
filters to them, counts them per set, and can sort every set by how many it has. Baked rather than
computed because the production server has no Scryfall cache — the same reason, and the same answer,
as the coverage denominator in `scripts/gen-set-totals`.

**`git diff`** reads it as the regression check this module has been missing. At corpus size a change
can move thousands of verdicts and "round-trips went up" hides the twelve cards that went *down*.
One card per line, sorted by name, means a re-bake's diff *is* the list of cards whose reading
changed — so re-bless it deliberately, in its own commit, the way the card goldens are. It is
therefore not wired into the build: a stale ledger degrades into an out-of-date badge, while an
auto-regenerated one would erase the only signal that made it worth committing.

It answers with [`CardCompiler`] rather than with line verdicts because "could be implemented using
Assay" is that object's exact question. A card whose every line round-trips can still fail on a `*`
power, a second face, or `CardValidator`, and a badge reading "ready" for a card that cannot be
produced would be worse than no badge.

> Baking it for the first time found a bug the gates could not see: `CardCompiler` **threw** on a
> card with a negative printed power (`CreatureStats` requires a non-negative base and enforces it
> with `require`), so Spinal Parasite and the Un-set creatures crashed the compiler instead of
> declining. Nothing had previously handed it all 34,882 cards; the Scenario Builder's paste box
> would have answered a 500. Negative P/T is now a `HEADER` decline naming the value — an SDK finding
> reported the way every other one is — and constructing the definition is guarded, so any *other*
> model invariant becomes an `INVALID_CARD` decline rather than an exception out of a bulk run.

## Adding a rule

1. Write it in `grammar/`, bidirectionally, through an SDK companion factory.
2. `just assay parse "<a card that uses it>"` — check the verdict, not just that it parses.
3. `just assay-gate` — the number that matters is that `MISMATCH` and `AMBIGUOUS` stay 0.
4. Add the surface form to `KeywordGrammarTest`'s round-trip list.

Three traps the kernel cannot catch for you:

- **A `match` half that quietly matches nothing** still compiles and still parses; it shows up on
  the corpus as a print mismatch far from its cause. The `every keyword rule can print what it
  parses` test exists for this.
- **Reversible but wrong.** "Elves" de-pluralizes to `Elve` and round-trips perfectly while meaning
  nothing. The touchstone structurally cannot catch that class — run `just assay-differential`, which
  is the general answer and has already caught two of these.
- **Templates are mid-sentence, and a line has more than one sentence in it.** Write `"draw a card."`
  and `"add {mana}."`, never `"Draw a card."` — `syntax/SentenceCase.kt` decapitalizes the line's
  first word *and* the clause after each ability cost's `": "`, then recapitalizes both on the way
  out. That is what lets `{T}: Add {G}.` be `Costs.cost` plus an unmodified `Steps` rule instead of a
  second, capitalized copy of the effect vocabulary.
