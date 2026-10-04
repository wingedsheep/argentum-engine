package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.constant
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions as SdkConditions
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.EntersWithKeywords
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyLifeGain
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * "This land enters tapped." — the self-replacements a permanent applies to its own entry.
 *
 * ### Why a whole family for what looks like one sentence
 *
 * `EntersTapped` is one SDK type with three printed shapes, and each of the later two costs nothing
 * once the first is written: the plain form, the shock-land form (`payLifeCost`), and the
 * check-land form (`unlessCondition`). The third was the condition family's first customer and is
 * now a slot rather than a rule — [Conditions.condition] is the vocabulary, and the sentence around
 * it is one template. Printing an `EntersTapped` that carries a condition as "~ enters tapped."
 * would be the reversible-but-wrong class in its purest form: byte-perfect, and a different card,
 * which is why the plain rule is a `constant` on the whole default value.
 *
 * The `match` halves are equality tests against a reconstruction for exactly that reason, so a
 * value carrying a non-default `appliesTo` — a *static* tapped-entry imposed on other permanents,
 * which the SDK spells with the same type — refuses to print rather than claiming to be the
 * source's own line.
 *
 * ### No facade to build through
 *
 * Every other family here goes through an SDK companion facade, per the module's rule. Replacement
 * effects have none: `Effects`, `Triggers`, `Costs` and `Conditions` all exist, and hand-written
 * cards construct `EntersTapped(...)` directly (`replacementEffect(EntersTapped(payLifeCost = 2))`
 * on Steam Vents and Stomping Ground). So the constructor *is* the curated surface here, and the
 * missing `Replacements` facade is a small SDK finding rather than a rule this file should route
 * around.
 */
object Replacements {

    /** "~ enters tapped." — 234 hand-written cards, and every one of them the bare default. */
    private val entersTapped: Phrase<ReplacementEffect> =
        constant("${Normalizer.SELF} enters tapped.", EntersTapped())

    /**
     * "As ~ enters, you may pay 2 life. If you don't, it enters tapped." — the shock lands.
     *
     * The same type with one field set, which is why it is a row beside the plain rule rather than
     * a family of its own. The digit is [Primitives.cardinal] because Oracle spells a quantity of
     * life as a numeral, the convention [Steps] takes both leaves for.
     *
     * The template spells its second sentence mid-sentence ("if you don't") for the reason every
     * template here is written mid-sentence: a full stop is a sentence start, and
     * [com.wingedsheep.assay.syntax.SentenceCase] owns the capital at every one of them.
     */
    private val shockLand: Phrase<ReplacementEffect> = phrase(
        "as ${Normalizer.SELF} enters, you may pay {n} life. if you don't, it enters tapped.",
        name = "enters tapped unless you pay life",
    ) {
        slot("n", Primitives.cardinal)
        build { EntersTapped(payLifeCost = it.int("n")) }
        match { effect ->
            val life = (effect as? EntersTapped)?.payLifeCost ?: return@match null
            if (effect != EntersTapped(payLifeCost = life)) return@match null
            bind("n" to life)
        }
    }

    /**
     * "~ enters tapped unless you control a basic land." — the check lands, the fast lands, the slow
     * lands, and every other conditional tapped entry.
     *
     * One template with [Conditions.condition] in it, for the reason the top-of-library band gives:
     * the SDK type's field *is* the slot, and a rule per printed condition would be one
     * whole-sentence rule per land cycle. The 107 lines behind this in the corpus spell 36 distinct
     * clauses; the sentence is one of them and the other 35 belong to the condition vocabulary,
     * where every other position that takes a condition gets them too.
     *
     * The `match` half is the family's usual reconstruct-and-compare, so a value that also carries a
     * `payLifeCost` or a non-default `appliesTo` refuses to print rather than dropping the half this
     * sentence has no room for.
     */
    private val entersTappedUnless: Phrase<ReplacementEffect> = phrase(
        "${Normalizer.SELF} enters tapped unless {cond}.",
        name = "enters tapped unless a condition holds",
    ) {
        slot("cond", Conditions.condition)
        build { EntersTapped(unlessCondition = it.value("cond")) }
        match { effect ->
            val condition = (effect as? EntersTapped)?.unlessCondition ?: return@match null
            if (effect != EntersTapped(unlessCondition = condition)) return@match null
            bind("cond" to condition)
        }
    }

    /**
     * "As ~ enters, choose a color." — Ward Sliver, and the whole choose-as-it-enters family.
     *
     * A replacement rather than a triggered ability because "as … enters" happens *during* the
     * entry, not after it, which is what `EntersWithChoice` models.
     *
     * ### Who chooses is a word; what is chosen is a noun phrase
     *
     * `EntersWithChoice` calls itself "a single parameterized type", and the sentence is its
     * product. The two axes are spelled in different ways and so are read in different ways. The
     * *chooser* is one word position — "choose" against "an opponent chooses" — with the same noun
     * phrase after it either way, so it is a [chooser] slot and one rule covers both. The *kind* of
     * choice is a rule parameter rather than a slot, because each is a different English noun phrase
     * ("a color", "an opponent", "a nonland card name") rather than a different word in one: the
     * same argument [Library.search] makes about its destinations.
     *
     * ### What is deliberately not here
     *
     * - **`ChoiceType.MODE`.** "As ~ enters, choose Khans or Dragons." is a `modeOptions` list whose
     *   `id`, `description` and `iconKey` the printed sentence does not contain — Outpost Siege's
     *   golden spells all three. A rule would have to invent two of them, and a reconstruction built
     *   from invented fields is the reversible-but-wrong class the module's fail-closed matching
     *   exists to refuse. 40-odd corpus lines, and they wait for a model whose only content is the
     *   labels.
     * - **A bare "choose a number."** (4 lines) against the bounded "choose a number between 0 and
     *   7." ([entersWithChosenNumber], 2 lines). The bounds are two required fields with no default
     *   the text implies, so the unbounded sentence has nothing to say about `minValue`/`maxValue`
     *   and `0`/`0` would mean "always zero".
     * - **`allowedCreatureTypes`** — "choose Elemental, Elf, Faerie, Giant, Goblin, Kithkin,
     *   Merfolk, or Treefolk." (2 lines) needs a capitalized creature-type run with an Oxford "or",
     *   which is [Primitives.scopeRun]'s shape over a vocabulary that does not exist yet.
     */
    private fun entersWithChoice(noun: String, value: EntersWithChoice): Phrase<ReplacementEffect> =
        phrase("as ${Normalizer.SELF} enters, {chooser} $noun.", name = "as it enters, choose $noun") {
            slot("chooser", chooser)
            build { value.copy(chooser = it.value("chooser")) }
            match { effect ->
                val choice = effect as? EntersWithChoice ?: return@match null
                if (choice != value.copy(chooser = choice.chooser)) return@match null
                bind("chooser" to choice.chooser)
            }
        }

    /**
     * Who makes the choice — the one word position `EntersWithChoice.chooser` occupies.
     *
     * Two rows rather than the whole of a player vocabulary, because these are the only two players
     * an as-it-enters line can name: the choice is made during the entry, so there is no target and
     * no triggering player to refer to. A value carrying any other `Player` reconstructs equal in
     * [entersWithChoice] and then fails to print here, which is where the fail-closed check lands
     * for this field.
     */
    private val chooser: Phrase<Player> = oneOf(
        "who chooses",
        constant("choose", Player.You),
        constant("an opponent chooses", Player.AnOpponent),
    )

    /**
     * "As ~ enters, choose a number between 0 and 7." — Shapeshifter, Talion, the Kindly Lord.
     *
     * A rule of its own rather than a [entersWithChoice] noun, because the noun phrase has two
     * numbers in it and they are the SDK fields `minValue`/`maxValue`. CR 614.1c's chosen number is
     * the only `ChoiceType` whose sentence carries data beyond the kind of choice.
     */
    private val entersWithChosenNumber: Phrase<ReplacementEffect> = phrase(
        "as ${Normalizer.SELF} enters, {chooser} a number between {lo} and {hi}.",
        name = "as it enters, choose a number in a range",
    ) {
        slot("chooser", chooser)
        slot("lo", Primitives.cardinal)
        slot("hi", Primitives.cardinal)
        build {
            EntersWithChoice(
                choiceType = ChoiceType.NUMBER,
                chooser = it.value("chooser"),
                minValue = it.int("lo"),
                maxValue = it.int("hi"),
            )
        }
        match { effect ->
            val choice = effect as? EntersWithChoice ?: return@match null
            if (choice.choiceType != ChoiceType.NUMBER) return@match null
            val rebuilt = EntersWithChoice(
                choiceType = ChoiceType.NUMBER,
                chooser = choice.chooser,
                minValue = choice.minValue,
                maxValue = choice.maxValue,
            )
            if (choice != rebuilt) return@match null
            bind("chooser" to choice.chooser, "lo" to choice.minValue, "hi" to choice.maxValue)
        }
    }

    /**
     * Every noun phrase an as-it-enters choice can take, paired with the value it denotes.
     *
     * The three `CARD_NAME` rows are one field's three published values, and the pool is a *word in
     * the noun phrase* rather than a slot for the same reason the kind of choice is: "a nonland card
     * name" and "a land card name" are noun phrases, not one noun phrase with an adjective slot the
     * `ANY` spelling would then have to leave empty.
     *
     * Petrified Hamlet is the standing finding here. Its Oracle spells the land-name choice as a
     * *trigger* — "When this land enters, choose a land card name." — while its golden, and this
     * family, model it as the as-it-enters replacement CR 614.1c describes. The row stays because
     * the value is real and the grammar must be able to print it; the card's line still declines,
     * and which of the two is wrong is a question for the card rather than for this file.
     */
    private val choiceNouns: List<Pair<String, EntersWithChoice>> = listOf(
        "a color" to EntersWithChoice(ChoiceType.COLOR),
        "a creature type" to EntersWithChoice(ChoiceType.CREATURE_TYPE),
        "another creature you control" to EntersWithChoice(ChoiceType.CREATURE_ON_BATTLEFIELD),
        "a basic land type" to EntersWithChoice(ChoiceType.BASIC_LAND_TYPE),
        "an opponent" to EntersWithChoice(ChoiceType.OPPONENT),
        "a land card name" to EntersWithChoice(ChoiceType.CARD_NAME, cardNamePool = CardNamePool.LAND),
        "a nonland card name" to EntersWithChoice(ChoiceType.CARD_NAME, cardNamePool = CardNamePool.NONLAND),
        "a card name" to EntersWithChoice(ChoiceType.CARD_NAME, cardNamePool = CardNamePool.ANY),
    )

    /**
     * "As ~ enters, look at an opponent's hand, then choose any card name." — Sorcerous Spyglass.
     *
     * `lookAtOpponentHand` is a flag on the same value, and the corpus spells the flag and the wider
     * pool together: every line carrying the look also says "any card name", and every line without
     * it says "a card name". So this is one sentence with the flag set rather than a prefix that
     * could ride on any of the [choiceNouns] — a prefix would print "look at an opponent's hand,
     * then choose a card name", which no card says.
     */
    private val entersWithLookedUpCardName: Phrase<ReplacementEffect> = constant(
        "as ${Normalizer.SELF} enters, look at an opponent's hand, then choose any card name.",
        EntersWithChoice(
            choiceType = ChoiceType.CARD_NAME,
            cardNamePool = CardNamePool.ANY,
            lookAtOpponentHand = true,
        ),
    )

    /**
     * The condition a self-entry replacement trails — [Conditions.condition] plus the two clauses
     * whose "it" is the entering permanent itself.
     *
     * "if you cast it" (`WasCast`, Nine-Lives Familiar) and "if you cast it from your hand"
     * (`WasCastFromHand`, the Myojin, Patched Plaything) live here rather than in the shared
     * vocabulary because the pronoun is positional. In this sentence it can only be the source, but
     * in a trigger whose event names a filter it is the object that matched — The Sibsig Ceremony's
     * "Whenever a creature you control enters, if you cast it" is `TriggeringEntityWasCast`, and Wild
     * Pair's "Whenever a creature enters, if you cast it from your hand" has no source-side reading at
     * all. A row in [Conditions.condition] would read both as the source, byte-perfect and wrong.
     */
    private val conditionalEntry: Phrase<Condition> = oneOf(
        "an entry condition",
        Conditions.condition,
        constant("you cast it", SdkConditions.WasCast),
        constant("you cast it from your hand", SdkConditions.WasCastFromHand),
    )

    /**
     * "~ enters with a +1/+1 counter on it.", "~ enters with three -1/-1 counters on it."
     *
     * ### Why `selfOnly` is spelled by the rule and not by a slot
     *
     * `EntersWithCounters` models both "this permanent enters with counters" and Hardened Scales'
     * "creatures you control enter with an extra counter" — the second is what its `appliesTo`
     * default describes, so the *self* reading is the one the flag has to state. The sentence says
     * "~ enters", naming the source and nothing else, so `selfOnly = true` is what this English
     * means; a value with `otherOnly`, a `condition` or a non-default `appliesTo` is a different
     * sentence and the reconstruct-and-compare refuses to print it. That last one matters here:
     * the kicker cards ("If ~ was kicked, it enters with two +1/+1 counters on it") carry a
     * `condition` and decline rather than losing the clause that makes them worth playing.
     *
     * ### The trailing "if …" is the `condition` field, and the kicker one is not
     *
     * "~ enters with a +1/+1 counter on it **if you attacked this turn**." (raid, morbid, the Myojin)
     * is the same value with `condition` set, so it is the same two rules with a [conditionalEntry]
     * slot after the counters rather than a sibling family. A `null` condition is the bare sentence
     * and a non-null one is the trailing clause — disjoint halves of one field, so the four rules
     * cannot print one value twice. The two cast-choice conditions are carved out of the conditional
     * half. `WasKicked`'s printed sentence fronts the condition ("If ~ was kicked, it enters with …",
     * [kickedEntry]), so a second printer here would be two rules for one value; and the shared
     * vocabulary spells `WasBargained` "it's bargained", its *cost*-position tense, which no entry
     * sentence prints.
     */
    private val entersWithCounters: List<Phrase<ReplacementEffect>> = run {
        val notTrailing = setOf(SdkConditions.WasKicked, SdkConditions.WasBargained)
        fun effectFor(kind: CounterType, count: Int, condition: Condition?): ReplacementEffect = EntersWithCounters(
            counterType = kind,
            count = count,
            selfOnly = true,
            condition = condition,
        )
        fun rule(template: String, name: String, quantity: Phrase<*>?, conditional: Boolean) =
            phrase(if (conditional) template.removeSuffix(".") + " if {cond}." else template, name = name) {
                slot("self", Primitives.self)
                slot("kind", if (quantity == null) Primitives.singularCounterKind else Primitives.counterKind)
                if (quantity != null) slot("n", quantity)
                if (conditional) slot("cond", conditionalEntry)
                build {
                    val condition: Condition? = if (conditional) it.value("cond") else null
                    if (condition in notTrailing) return@build null
                    effectFor(it.value("kind"), if (quantity == null) 1 else it.int("n"), condition)
                }
                match { effect ->
                    val enters = effect as? EntersWithCounters ?: return@match null
                    val kind = enters.counterType
                    val condition = enters.condition
                    if ((condition != null) != conditional || condition in notTrailing) return@match null
                    if (quantity == null && enters.count != 1) return@match null
                    if (quantity != null && !(enters.count >= 2 && Cardinals.spellable(enters.count))) {
                        return@match null
                    }
                    if (enters != effectFor(kind, enters.count, condition)) return@match null
                    bind("self" to Unit, "kind" to kind, "n" to enters.count, "cond" to condition)
                }
            }
        listOf(false, true).flatMap { conditional ->
            val suffix = if (conditional) " if a condition holds" else ""
            listOf(
                rule("{self} enters with {kind} counter on it.", "enters with a counter$suffix", null, conditional),
                rule(
                    "{self} enters with {n} {kind} counters on it.",
                    "enters with counters$suffix",
                    Cardinals.word,
                    conditional,
                ),
            )
        }
    }

    /**
     * "~ enters with **X** +1/+1 counters on it.", "~ enters with X charge counters on it." — the
     * announced X, as the count a permanent brings onto the battlefield with it.
     *
     * ### Why this is the one position that may read a bare `X`
     *
     * [Amounts.namesX]'s KDoc carries the argument and the evidence; the short form is that the
     * reading is only legal where the resolution context is live, and here it provably is.
     * `EntersWithReplacements` builds `EffectContext(xValue = spellComponent.xValue)` on the self
     * path, inside the permanent spell's own resolution, so [DynamicAmount.XValue] is the value the
     * text names. A *step* cannot know that — [Triggers] lifts steps — which is why
     * [Amounts.definedByCount]'s two positions take the defined clauses and not this row.
     *
     * ### `EntersWithDynamicCounters` spells self the opposite way round from its fixed sibling
     *
     * A finding rather than a rule: [EntersWithCounters] says self with `selfOnly = true` and
     * [EntersWithDynamicCounters] has no such flag — it is self by *default*, and the global scan
     * that walks the battlefield skips it unless `otherOnly` is set. Its `appliesTo` default
     * therefore says `Creature.youControl()` on a value that never consults it, which reads like a
     * group effect and is not one. Nothing here can fix that; what it can do is refuse to depend on
     * it, so the reconstruct-and-compare below rebuilds the whole value and a golden carrying a
     * non-default `appliesTo`, an `otherOnly` or a condition declines rather than losing the field.
     */
    private val entersWithDynamicCounters: List<Phrase<ReplacementEffect>> = run {
        fun effectFor(kind: CounterType, amount: DynamicAmount): ReplacementEffect = EntersWithDynamicCounters(
            counterType = kind,
            count = amount,
        )
        /** The shared reader: the kind and the amount, or null on any value this family cannot say. */
        fun readAmount(effect: ReplacementEffect, allows: (DynamicAmount) -> Boolean): Pair<CounterType, DynamicAmount>? {
            val enters = effect as? EntersWithDynamicCounters ?: return null
            val kind = enters.counterType
            if (!allows(enters.count)) return null
            if (enters != effectFor(kind, enters.count)) return null
            return kind to enters.count
        }
        val announced = phrase<ReplacementEffect>(
            "{self} enters with X {kind} counters on it.",
            name = "enters with the announced X in counters",
        ) {
            slot("self", Primitives.self)
            slot("kind", Primitives.counterKind)
            build { effectFor(it.value("kind"), DynamicAmount.XValue) }
            match { effect ->
                val (kind, _) = readAmount(effect) { it == DynamicAmount.XValue } ?: return@match null
                bind("self" to Unit, "kind" to kind)
            }
        }
        val defined = phrase<ReplacementEffect>(
            "{self} enters with X {kind} counters on it${Amounts.WHERE_X}.",
            name = "enters with a counted number of counters",
        ) {
            definedByCount()
            slot("self", Primitives.self)
            slot("kind", Primitives.counterKind)
            slot("amount", Amounts.count)
            // The domain is checked here as well as in `match`: returning null drops the reading,
            // which is how a clause the model cannot hold declines instead of parsing.
            build {
                val amount = it.value<DynamicAmount>("amount")
                if (Amounts.namesX(amount)) effectFor(it.value("kind"), amount) else null
            }
            match { effect ->
                val (kind, amount) = readAmount(effect, Amounts::namesX) ?: return@match null
                bind("self" to Unit, "kind" to kind, "amount" to amount)
            }
        }
        listOf(announced, defined)
    }

    /**
     * "~ enters with a +1/+1 counter on it for each other Ooze you control." — Aeve, Kinsbaile
     * Borderguard, Éomer; "~ enters with **two** +1/+1 counters on it for each other nontoken Human
     * you control." — Hamlet Vanguard; "This enchantment enters with a hope counter on it for each
     * creature you control." — Dawn of a New Age, and eighteen more.
     *
     * The third shape of the enters-with count, and the one Oracle spells as a *rate* rather than as
     * a number: the printed numeral multiplies a battlefield tally, which [Amounts.scaled] lowers
     * the way [Statics]' pump family already did. It is a family rather than a row of
     * [entersWithDynamicCounters] because the count is a whole clause with its own two layers, not a
     * `{n}` — and it cannot slot [Amounts.count], whose surface is "the number of …" and whose
     * sentences say "equal to" in front of it. "For each" is the other half of the same model with
     * a different English shape, exactly as [Amounts.drawForEach] is to [Amounts.count].
     *
     * ### "Other" is a row, and it belongs to the count
     *
     * `AggregateBattlefield.excludeSelf` is where the SDK puts it — not a filter predicate, which is
     * the finding the conditional-tapped-entry band recorded for "two or fewer **other** lands" and
     * the same one twenty goldens had written as arithmetic. So the word is a row of this family
     * crossed with [Amounts.scopes], not a layer inside [Filters]: a filter that carried it would be
     * printable in every position a filter is, and only a *counted* noun phrase says "other".
     * Written inline here as the first family that needs it; when a second for-each family wants the
     * word it becomes a published layer beside [Amounts.scopes], which is what happened to the scope
     * clause itself.
     *
     * ### Why the singular row is not the counted row with n = 1
     *
     * [Amounts.scaled] maps a multiplier of 1 to the bare tally, so "a counter … for each X" and a
     * hypothetical "one counter … for each X" would denote the same value — and English prints only
     * the first. [Cardinals.word] therefore starts at two, as it does everywhere else in this
     * grammar, and the article is its own row with no slot in it.
     */
    private fun entersWithCountersPerCount(
        scope: Amounts.Scope,
        other: Boolean,
        counted: Boolean,
    ): Phrase<ReplacementEffect> {
        // The singular row spells no article: [Primitives.singularCounterKind] carries it, because
        // English derives it from the counter's own name ("a +1/+1 counter", "an omen counter") —
        // the same split [entersWithCounters]' two rows take.
        val article = if (counted) "{n} " else ""
        val noun = if (counted) "counters" else "counter"
        val otherWord = if (other) "other " else ""

        fun tallyOf(filter: GameObjectFilter) =
            DynamicAmount.AggregateBattlefield(scope.player, filter, excludeSelf = other)

        fun effectFor(kind: CounterType, filter: GameObjectFilter, multiplier: Int): ReplacementEffect =
            EntersWithDynamicCounters(
                counterType = kind,
                count = Amounts.scaled(tallyOf(filter), multiplier),
            )

        return phrase(
            "{self} enters with $article{kind} $noun on it for each $otherWord{counted}${scope.surface}.",
            name = "enters with counters per ${if (other) "other " else ""}${scope.where}" +
                if (counted) ", several each" else "",
        ) {
            slot("self", Primitives.self)
            slot("kind", if (counted) Primitives.counterKind else Primitives.singularCounterKind)
            if (counted) slot("n", Cardinals.word)
            slot("counted", Filters.filter)
            build { bindings ->
                val filter = scope.narrowing(bindings.value("counted")) ?: return@build null
                effectFor(bindings.value("kind"), filter, if (counted) bindings.int("n") else 1)
            }
            match { effect ->
                val enters = effect as? EntersWithDynamicCounters ?: return@match null
                val kind = enters.counterType
                val tally = tallyIn(enters.count) ?: return@match null
                if (tally != tallyOf(tally.filter)) return@match null
                val multiplier = Amounts.multiplierOf(enters.count, tally) ?: return@match null
                // The article row spells 1 and the counted row spells two and up, so exactly one of
                // them answers for any value and printing is decided by the model.
                if (counted != (multiplier >= 2 && Cardinals.spellable(multiplier))) return@match null
                if (effect != effectFor(kind, tally.filter, multiplier)) return@match null
                val narrowed = scope.narrowing(tally.filter) ?: return@match null
                bind("self" to Unit, "kind" to kind, "n" to multiplier, "counted" to narrowed)
            }
        }
    }

    /** The battlefield tally inside a scaled count, or null when the amount is not one at all. */
    private fun tallyIn(amount: DynamicAmount): DynamicAmount.AggregateBattlefield? = when (amount) {
        is DynamicAmount.AggregateBattlefield -> amount
        is DynamicAmount.Multiply -> amount.amount as? DynamicAmount.AggregateBattlefield
        else -> null
    }

    /**
     * The family: every [Amounts.scopes] row, crossed with "other", crossed with the article and the
     * numeral — and **every row an alternate spelling**.
     *
     * Oracle prints both "~ enters with a +1/+1 counter on it for each other creature you control."
     * (Squad Captain) and "~ enters with X +1/+1 counters on it, where X is the number of other
     * creatures on the battlefield." (Stag Beetle, Custodi Soulbinders) for the *same* model, so one
     * of the two rules has to be the printer. [entersWithDynamicCounters]' `defined` row is the one
     * that can print the **whole** domain — every zone count, life total and battlefield aggregate
     * [Amounts.count] spells — while this family reaches only the battlefield tallies, so it parses
     * and never prints and the cards that spell it come back as variants.
     *
     * That is the counting band's rule applied unchanged ("check which rule can print the whole
     * domain before deciding which is canonical"), and it is why this family raises the number of
     * cards Assay *reads* without moving a single card off its existing round trip.
     */
    private val entersWithCountersPerCount: List<Phrase<ReplacementEffect>> =
        listOf(false, true).flatMap { other ->
            listOf(false, true).flatMap { counted ->
                Amounts.scopes.map { scope ->
                    alternate(entersWithCountersPerCount(scope, other, counted))
                }
            }
        }

    /**
     * "If you would gain life, you gain that much life plus 1 instead." — Heron of Hope, Leyline of
     * Hope, Angel of Vitality; and "…you gain twice that much life instead." — Alhammarret's
     * Archive, the Wind Crystal.
     *
     * The first replacement family here that is not about the source's own entry, and the first
     * whose sentence has a *subject* at all. Both rows are one shape over `ModifyLifeGain`'s two
     * arithmetic fields, spelled the way the type's own `description` spells them — the additive
     * form as "that much life plus {n}" and the multiplicative as "twice that much life" — so a
     * future card printing "three times that much life" is a row and not a rewrite.
     *
     * ### The subject is a constant, and the corpus is why
     *
     * `appliesTo` could take any `Player`, so this looks like a slot. It is not, because the other
     * subjects Oracle prints are **not this type**: "If a player would gain life, that player gains
     * no life instead." is `PreventLifeGain` (Sulfuric Vortex) and "If an opponent would gain life,
     * that player loses that much life instead." is a life-*loss* conversion (Tainted Remedy) — two
     * models this family cannot build and must not approximate. Every one of the 14 corpus lines
     * `ModifyLifeGain` can hold says "you", so the word is in the template and the day a card prints
     * another gainer for this model it becomes a slot with a real second value in it.
     *
     * ### `ModifyLifeGain(0, 0)` is a spelling this rule never emits
     *
     * "gains no life" is expressible twice in the SDK — as this type with both fields zeroed, and as
     * `PreventLifeGain`, which is what the 8 hand-written cards write. That is the module's
     * two-spellings rule: the grammar prints the majority and reports the other, so the `build`
     * halves below refuse a zero multiplier rather than acquiring a third row that would collide
     * with a `PreventLifeGain` family the day someone writes one.
     *
     * The `match` halves are the family's usual reconstruct-and-compare, so a value carrying a
     * `restrictions` gate — Phial of Galadriel's "while you have 5 or less life" — declines rather
     * than printing a sentence with no room for it.
     */
    private val modifyLifeGain: List<Phrase<ReplacementEffect>> = run {
        fun effectFor(multiplier: Int, modifier: Int): ReplacementEffect = ModifyLifeGain(
            multiplier = multiplier,
            modifier = modifier,
            appliesTo = EventPattern.LifeGainEvent(player = Player.You),
        )
        /** The two fields, or null on any value this family cannot say. */
        fun readFields(effect: ReplacementEffect): Pair<Int, Int>? {
            val gain = effect as? ModifyLifeGain ?: return null
            if (gain.multiplier == 0) return null
            if (gain != effectFor(gain.multiplier, gain.modifier)) return null
            return gain.multiplier to gain.modifier
        }
        val plus = phrase<ReplacementEffect>(
            "if you would gain life, you gain that much life plus {n} instead.",
            name = "gain that much life plus a number instead",
        ) {
            slot("n", Primitives.cardinal)
            build { if (it.int("n") >= 1) effectFor(multiplier = 1, modifier = it.int("n")) else null }
            match { effect ->
                val (multiplier, modifier) = readFields(effect) ?: return@match null
                if (multiplier != 1 || modifier < 1) return@match null
                bind("n" to modifier)
            }
        }
        val twice = constant<ReplacementEffect>(
            "if you would gain life, you gain twice that much life instead.",
            effectFor(multiplier = 2, modifier = 0),
        )
        listOf(plus, twice)
    }

    /**
     * "If ~ would be put into a graveyard from anywhere, exile it instead." — the second line every
     * disturb back face prints (CR 702.146b), and the whole of this rule's corpus: 28 cards, every
     * one of them a MID/VOW transforming card whose back is the disturbed face.
     *
     * A `constant` on the whole value, for [entersTapped]'s reason. `RedirectZoneChange` has six
     * fields beyond the destination and this sentence spells none of them: `linkToSource`,
     * `shuffleIntoLibrary`, `reveal` and a non-`Any` `requiredCause` all belong to other printed
     * lines, and `appliesTo` is pinned by "from anywhere" (no `from`) plus "into a graveyard"
     * (`to = GRAVEYARD`). A rule that let any of them vary would print this sentence for a value
     * that means something else, which is the reversible-but-wrong class exactly.
     *
     * `selfOnly = true` is what "~" buys: the normalizer has already replaced the face's own name,
     * so the subject *is* the source, and the sibling lines that name a filter instead — Rest in
     * Peace's "a card or token", Dryad Militant's "an instant or sorcery card" — are a different
     * value (`selfOnly = false`, a non-`Any` filter) and stay declined here rather than being
     * folded into a slot this sentence would then mis-print.
     */
    private val exileInsteadOfGraveyard: Phrase<ReplacementEffect> = constant(
        "if ${Normalizer.SELF} would be put into a graveyard from anywhere, exile it instead.",
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
            selfOnly = true,
        ),
    )

    /**
     * "If ~ was kicked, it enters with two +1/+1 counters on it." and its keyword-carrying sibling
     * "If ~ was kicked, it enters with two +1/+1 counters on it and with flying." — the kicker
     * creatures of Invasion and Dominaria.
     *
     * ### One sentence, two replacement effects
     *
     * The hand-written cards spell the keyword half as a *second* replacement, an
     * [EntersWithKeywords] beside the [EntersWithCounters], both gated on the same `WasKicked` —
     * the shape `EntersWithKeywords`' own KDoc names with Kavu Titan. So this family yields a list,
     * and the line rule that holds it is the plain replacement line one size up: a sentence is
     * still one line, the model just has two members. The keyword is one [Keywords.keyword] rather
     * than a run because every printed line names exactly one, and a run would have to choose
     * between "and with flying and haste" and "and with flying and with haste" for no card.
     *
     * The condition is a word rather than a [Conditions.condition] slot: the subject is the source
     * itself ("~ was kicked"), which the condition vocabulary spells "it was kicked" from the
     * trigger position, and the only printed customer of this sentence shape is kicker. Both
     * halves are reconstruct-and-compare, so a value with another condition, a non-default
     * `appliesTo` or `selfOnly = false` refuses to print rather than claiming this sentence.
     */
    private val kickedEntry: List<Phrase<List<ReplacementEffect>>> = run {
        fun counters(kind: CounterType, count: Int) = EntersWithCounters(
            counterType = kind,
            count = count,
            selfOnly = true,
            condition = SdkConditions.WasKicked,
        )
        fun keywords(keyword: Keyword) = EntersWithKeywords(
            keywords = listOf(keyword),
            selfOnly = true,
            condition = SdkConditions.WasKicked,
        )
        fun rule(quantity: Phrase<*>?, withKeyword: Boolean): Phrase<List<ReplacementEffect>> {
            val counted = if (quantity == null) "{kind} counter" else "{n} {kind} counters"
            val tail = if (withKeyword) " and with {keyword}" else ""
            return phrase(
                "if {self} was kicked, it enters with $counted on it$tail.",
                name = "kicked entry with " + (if (quantity == null) "a counter" else "counters") +
                    (if (withKeyword) " and a keyword" else ""),
            ) {
                slot("self", Primitives.self)
                slot("kind", if (quantity == null) Primitives.singularCounterKind else Primitives.counterKind)
                if (quantity != null) slot("n", quantity)
                if (withKeyword) slot("keyword", Keywords.keyword)
                build {
                    val first = counters(it.value("kind"), if (quantity == null) 1 else it.int("n"))
                    if (withKeyword) listOf(first, keywords(it.value("keyword"))) else listOf(first)
                }
                match { effects ->
                    if (effects.size != (if (withKeyword) 2 else 1)) return@match null
                    val enters = effects[0] as? EntersWithCounters ?: return@match null
                    if (quantity == null && enters.count != 1) return@match null
                    if (quantity != null && !(enters.count >= 2 && Cardinals.spellable(enters.count))) {
                        return@match null
                    }
                    if (enters != counters(enters.counterType, enters.count)) return@match null
                    val keyword = if (withKeyword) {
                        val granted = effects[1] as? EntersWithKeywords ?: return@match null
                        val only = granted.keywords.singleOrNull() ?: return@match null
                        if (granted != keywords(only)) return@match null
                        only
                    } else null
                    bind("self" to Unit, "kind" to enters.counterType, "n" to enters.count, "keyword" to keyword)
                }
            }
        }
        listOf(null, Cardinals.word).flatMap { quantity -> listOf(false, true).map { rule(quantity, it) } }
    }

    val replacement: Phrase<ReplacementEffect> = oneOf(
        "a replacement effect",
        listOf(
            entersTapped,
            entersTappedUnless,
            shockLand,
            entersWithChosenNumber,
            entersWithLookedUpCardName,
            exileInsteadOfGraveyard,
        ) + choiceNouns.map { (noun, value) -> entersWithChoice(noun, value) } +
            entersWithCounters + entersWithDynamicCounters + entersWithCountersPerCount +
            modifyLifeGain,
    )

    /** One replacement, lifted to the list a line holds. */
    private val singleReplacement: Phrase<List<ReplacementEffect>> =
        phrase("{one}", name = "one replacement effect") {
            slot("one", replacement)
            build { listOf(it.value<ReplacementEffect>("one")) }
            match { it.singleOrNull()?.let { only -> bind("one" to only) } }
        }

    /**
     * The replacement effects one line spells — usually one, two when a sentence's halves are
     * modelled as separate effects ([kickedEntry]).
     */
    val replacements: Phrase<List<ReplacementEffect>> =
        oneOf("the replacement effects of a line", listOf(singleReplacement) + kickedEntry)
}
