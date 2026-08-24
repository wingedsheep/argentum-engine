package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.constant
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
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
     * ### The counter kind is a [CounterTypeFilter] here and a `String` on every effect
     *
     * Two SDK types for one concept, and `CounterTypeFilter.Named` can hold the same string the
     * dedicated cases do — so the grammar emits exactly one of the two spellings and reports the
     * other. [Primitives.counterFilter] and its inverse own that choice; the note is there.
     */
    private val entersWithCounters: List<Phrase<ReplacementEffect>> = run {
        fun effectFor(kind: String, count: Int): ReplacementEffect = EntersWithCounters(
            counterType = Primitives.counterFilter(kind),
            count = count,
            selfOnly = true,
        )
        fun rule(template: String, name: String, quantity: Phrase<*>?) =
            phrase(template, name = name) {
                slot("self", Primitives.self)
                slot("kind", if (quantity == null) Primitives.singularCounterKind else Primitives.counterKind)
                if (quantity != null) slot("n", quantity)
                build { effectFor(it.value("kind"), if (quantity == null) 1 else it.int("n")) }
                match { effect ->
                    val enters = effect as? EntersWithCounters ?: return@match null
                    val kind = Primitives.counterKindOf(enters.counterType) ?: return@match null
                    if (quantity == null && enters.count != 1) return@match null
                    if (quantity != null && !(enters.count >= 2 && Cardinals.spellable(enters.count))) {
                        return@match null
                    }
                    if (enters != effectFor(kind, enters.count)) return@match null
                    bind("self" to Unit, "kind" to kind, "n" to enters.count)
                }
            }
        listOf(
            rule("{self} enters with {kind} counter on it.", "enters with a counter", null),
            rule("{self} enters with {n} {kind} counters on it.", "enters with counters", Cardinals.word),
        )
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
        fun effectFor(kind: String, amount: DynamicAmount): ReplacementEffect = EntersWithDynamicCounters(
            counterType = Primitives.counterFilter(kind),
            count = amount,
        )
        /** The shared reader: the kind and the amount, or null on any value this family cannot say. */
        fun readAmount(effect: ReplacementEffect, allows: (DynamicAmount) -> Boolean): Pair<String, DynamicAmount>? {
            val enters = effect as? EntersWithDynamicCounters ?: return null
            val kind = Primitives.counterKindOf(enters.counterType) ?: return null
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

    val replacement: Phrase<ReplacementEffect> = oneOf(
        "a replacement effect",
        listOf(
            entersTapped,
            entersTappedUnless,
            shockLand,
            entersWithChosenNumber,
            entersWithLookedUpCardName,
        ) + choiceNouns.map { (noun, value) -> entersWithChoice(noun, value) } +
            entersWithCounters + entersWithDynamicCounters,
    )
}
