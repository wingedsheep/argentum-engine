package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.PhraseBuilder
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.constant
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Clauses whose number is a **count of something**, and the vocabulary that names the count.
 *
 * Oracle spells a variable quantity three ways, and all three are here because they are one family
 * in the model — a [DynamicAmount] in the slot a numeral would otherwise fill:
 *
 * | Surface | Example |
 * |---|---|
 * | a trailing definition of X | "gets +X/+X until end of turn, **where X is the number of Elves on the battlefield**" |
 * | "equal to …" | "deals damage **equal to the number of +1/+1 counters on it**" |
 * | "for each …" | "you lose 1 life **for each +1/+1 counter on it**" |
 *
 * ### Why the amount is a slot and the verb is not
 *
 * [Steps.countedStep] takes a numeral and [Steps.amountStep] takes a *fixed* dynamic amount chosen
 * by the rule, because in those sentences the amount has no printed form of its own — "X" is a
 * literal. Here it does: "the number of Elves on the battlefield" is a whole noun phrase that varies
 * independently of the verb, so it is a phrase ([count]) and the verbs slot it. That is what makes
 * this a family of two dozen sentences rather than a rule per tribe.
 *
 * ### The where-clause is part of the sentence, not of the amount
 *
 * "…gets +X/+X until end of turn, where X is the number of Elves on the battlefield" is *one*
 * sentence: the "X" in the verb phrase and the "X" in the definition are the same value, and the
 * model stores it once. So the rule spans both halves and the literal "X" appears twice in the
 * template — a card that printed the definition without the use, or two different letters, is not
 * this sentence and declines.
 */
object Amounts {

    // ---------------------------------------------------------------------------------------
    // The vocabulary: what a count counts
    // ---------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------------------
    // Where a tally counts — the layer that five families each froze a different row of
    // ---------------------------------------------------------------------------------------

    /**
     * **Where a battlefield tally counts**, as the three clauses English ends the noun phrase on.
     *
     * One layer, and the reason it is published rather than spelled per rule is that it is the same
     * three rows every time: "the number of Elves **on the battlefield**", "~ gets +1/+1 for each
     * artifact **you control**", "you gain 1 life for each attacking creature" — same clause, three
     * heads in front of it. Before this table each family wrote the row it happened to be born for
     * and froze the rest as literal text, and every one of them froze a *different* row: [count]
     * had two, [drawForEach] had only "you control", [Statics.selfPumpPerCount] and
     * [Steps.gainLifeForEach] had only "on the battlefield", and [SpellCosts.perUnitSource] had both
     * of those and not the bare one. A card printing the row its family had not been born with died
     * on the sentence's own full stop, which is why they were all in the `.` decline family.
     *
     * ### The empty row is a row
     *
     * English omits the clause — "for each attacking creature" — and means the whole battlefield, so
     * the bare form is a *spelling* of the "on the battlefield" model rather than a model of its
     * own. It therefore parses and never prints ([canonical] false), and a card printing it comes
     * back as a variant rather than a decline. The alternative — making the bare form canonical —
     * would turn every card that spells the clause out into a variant, which trades the same number
     * the other way and loses the byte-exact readings we already have.
     *
     * ### What each row may print in front of
     *
     * A counted noun phrase says where it counts **once**. So a clause with a surface of its own
     * refuses a filter that already carries a controller, and the empty clause refuses only the
     * *you-control* one — because that is the filter whose words the " you control" row prints, and
     * two rows that could read one text is the ambiguity this grammar never resolves by ordering.
     * "for each creature an opponent controls" has no row of its own, so it goes through the empty
     * clause with the controller inside the noun phrase, which is exactly what the model says.
     */
    data class Scope(val surface: String, val player: Player, val where: String, val canonical: Boolean = true) {

        /** The filter this clause may be printed in front of, or null when the two would say it twice. */
        fun narrowing(filter: GameObjectFilter): GameObjectFilter? = when {
            surface.isNotEmpty() -> filter.takeIf { it.controllerPredicate == null }
            else -> filter.takeIf { it.controllerPredicate != ControllerPredicate.ControlledByYou }
        }
    }

    /** The layer itself. Adding a row here reaches every family that counts. */
    val scopes: List<Scope> = listOf(
        Scope(" on the battlefield", Player.Each, "the whole battlefield"),
        Scope("", Player.Each, "the whole battlefield, unqualified", canonical = false),
        Scope(" you control", Player.You, "your battlefield"),
    )

    /**
     * Build one rule per [scopes] row, marking the non-printing ones — the shape every family that
     * slots this layer takes.
     */
    fun <T> perScope(rule: (Scope) -> Phrase<T>): List<Phrase<T>> =
        scopes.map { scope -> rule(scope).let { if (scope.canonical) it else alternate(it) } }

    /**
     * "the number of Elves on the battlefield", "the number of Zombies you control" — a battlefield
     * tally over a noun phrase.
     *
     * The two surfaces differ only in whose battlefield is scanned, which is the `Player` field of
     * the aggregate, so they are two rows of one shape rather than a rule with a player slot: "on
     * the battlefield" and "you control" are not two values of one word, they are two clauses.
     *
     * **`AggregateBattlefield` is canonical for the battlefield and `Count` is never emitted for
     * it.** The SDK spells one battlefield tally twice — `Count(You, BATTLEFIELD, f)` is the same
     * value as `AggregateBattlefield(You, f)` — and the hand-written corpus writes the aggregate 603
     * times against the `Count` spelling's 49. Registering both would be ambiguity by construction,
     * so the minority spelling declines and this paragraph is the finding. The mirror image holds
     * below: `Count` is canonical off the battlefield, where `AggregateZone`'s default aggregation
     * restates it 17 times against `Count`'s 236.
     */
    private fun battlefieldCount(scope: Scope): Phrase<DynamicAmount> =
        phrase("the number of {filter}${scope.surface}", name = "a count of ${scope.where}") {
            slot("filter", Filters.plural)
            build { bindings ->
                val filter = scope.narrowing(bindings.value("filter")) ?: return@build null
                DynamicAmount.AggregateBattlefield(scope.player, filter)
            }
            match { amount ->
                val aggregate = amount as? DynamicAmount.AggregateBattlefield ?: return@match null
                if (aggregate.player != scope.player) return@match null
                if (amount != DynamicAmount.AggregateBattlefield(scope.player, aggregate.filter)) return@match null
                bind("filter" to (scope.narrowing(aggregate.filter) ?: return@match null))
            }
        }

    /**
     * "the number of creature cards in your graveyard", "the number of instant cards in all
     * graveyards" — a tally of a *zone*, which Oracle spells with the noun **card**.
     *
     * The noun is the whole difference from [battlefieldCount] and it is the rules' own: an object
     * in a graveyard is a card, not a permanent (the same split [Graveyard] states), so the type
     * phrase stays *singular* in front of the inflected "cards" and this rule slots
     * [Filters.filter] where the battlefield rule slots [Filters.plural].
     */
    private fun zoneCardCount(
        surface: String,
        player: Player,
        zone: Zone,
        name: String,
    ): Phrase<DynamicAmount> =
        phrase("the number of {filter} $surface", name = name) {
            slot("filter", Filters.pluralCards)
            // `Count`'s filter defaults to `Any`, so the bare noun this vocabulary can now print is
            // *definitionally* [bareZoneCount]'s model. Two rules for one value is the
            // redundant-readings configuration, so the unqualified count stays that rule's — it
            // covers three surfaces where this one covers two — and this row refuses it.
            build { it.value<GameObjectFilter>("filter").takeIf { f -> f != GameObjectFilter.Any }
                ?.let { f -> DynamicAmount.Count(player, zone, f) } }
            match { amount ->
                val counted = amount as? DynamicAmount.Count ?: return@match null
                if (counted.filter == GameObjectFilter.Any) return@match null
                if (counted != DynamicAmount.Count(player, zone, counted.filter)) return@match null
                bind("filter" to counted.filter)
            }
        }

    /**
     * "the greatest mana value among creatures you control", "the number of colors among permanents
     * you control" — a battlefield read that is **not** a tally.
     *
     * One layer owning one field, in [Filters]' sense: the head phrase is exactly what the
     * `aggregation` (and its `property`) spell, and everything after it is the same noun phrase and
     * controller clause [battlefieldCount] reads. So a new aggregation is a row, not a rule — and
     * the counting head stays out of this list, because `COUNT` is [battlefieldCount] and offering
     * it twice is ambiguity by construction.
     */
    private fun battlefieldAggregate(
        head: String,
        aggregation: Aggregation,
        property: CardNumericProperty?,
        surface: String,
        player: Player,
        name: String,
    ): Phrase<DynamicAmount> =
        phrase("$head {filter} $surface", name = name) {
            slot("filter", Filters.plural)
            build { DynamicAmount.AggregateBattlefield(player, it.value("filter"), aggregation, property) }
            match { amount ->
                val aggregate = amount as? DynamicAmount.AggregateBattlefield ?: return@match null
                val rebuilt =
                    DynamicAmount.AggregateBattlefield(player, aggregate.filter, aggregation, property)
                if (amount != rebuilt) return@match null
                bind("filter" to aggregate.filter)
            }
        }

    /**
     * "the number of cards in your hand" — the same tally with **no** filter, which English spells
     * by dropping the type phrase rather than by writing a word for "any".
     *
     * A row rather than a `Filters.Any` inside [zoneCardCount], for the reason [Filters] gives for
     * having no bare "card" noun: a filter that printed as the empty string would let every other
     * layer print a phrase with a hole in it. The unfiltered count is a different *sentence*, so it
     * is a different rule.
     */
    private fun bareZoneCount(surface: String, player: Player, zone: Zone): Phrase<DynamicAmount> =
        constant("the number of cards $surface", DynamicAmount.Count(player, zone))

    /** "the number of flame counters on ~" — see [plainCount]'s last row for why the kind is a slot. */
    private val counterCount: Phrase<DynamicAmount> =
        phrase("the number of {kind} counters on {self}", name = "a count of the source's counters") {
            slot("kind", Primitives.counterKind)
            slot("self", Primitives.self)
            build { DynamicAmounts.countersOnSelf(Primitives.counterFilter(it.value("kind"))) }
            match { amount ->
                val property = (amount as? DynamicAmount.EntityProperty) ?: return@match null
                val counter = (property.numericProperty as? EntityNumericProperty.CounterCount)
                    ?: return@match null
                val kind = Primitives.counterKindOf(counter.counterType) ?: return@match null
                if (amount != DynamicAmounts.countersOnSelf(counter.counterType)) return@match null
                bind("kind" to kind, "self" to Unit)
            }
        }

    /**
     * Everything a count can be, before the arithmetic layer.
     *
     * The four zones are rows rather than a slot for the same reason the two battlefields are:
     * "in your graveyard" and "in all graveyards" are two clauses, not two values of one word, and
     * the possessive changes with the player. What is shared is the *shape*, which is the point.
     */
    private val plainCount: Phrase<DynamicAmount> = oneOf(
        "a plain count",
        perScope(::battlefieldCount) + listOf(
            zoneCardCount("in your graveyard", Player.You, Zone.GRAVEYARD, "a count of your graveyard"),
            zoneCardCount("in all graveyards", Player.Each, Zone.GRAVEYARD, "a count of every graveyard"),
            bareZoneCount("in your graveyard", Player.You, Zone.GRAVEYARD),
            bareZoneCount("in your hand", Player.You, Zone.HAND),
            // "their" is the triggering player's, which is why this one names a player the others reach
            // through a possessive: the sentence it lives in has already introduced them.
            bareZoneCount("in their hand", Player.TriggeringPlayer, Zone.HAND),
            constant("your life total", DynamicAmount.YourLifeTotal),
            battlefieldAggregate(
                "the greatest mana value among", Aggregation.MAX, CardNumericProperty.MANA_VALUE,
                "you control", Player.You, "the greatest mana value on your battlefield",
            ),
            battlefieldAggregate(
                "the greatest power among", Aggregation.MAX, CardNumericProperty.POWER,
                "you control", Player.You, "the greatest power on your battlefield",
            ),
            battlefieldAggregate(
                "the greatest toughness among", Aggregation.MAX, CardNumericProperty.TOUGHNESS,
                "you control", Player.You, "the greatest toughness on your battlefield",
            ),
            battlefieldAggregate(
                "the number of colors among", Aggregation.DISTINCT_COLORS, null,
                "you control", Player.You, "the colours on your battlefield",
            ),
            // Domain, spelled out. The SDK publishes `DynamicAmounts.domain()` for it, and that factory
            // builds exactly this aggregate over `GameObjectFilter.Land` — so slotting the noun rather
            // than fixing it costs nothing and reads the two cards that count the types among something
            // narrower.
            battlefieldAggregate(
                "the number of basic land types among", Aggregation.DISTINCT_BASIC_LAND_SUBTYPES, null,
                "you control", Player.You, "the basic land types on your battlefield",
            ),
            // Tarmogoyf's count, and the one aggregation the corpus spells over a *zone*. Bare rather
            // than filtered because "card types among cards" is the only noun Oracle writes it with —
            // a filtered version would be a printed form no card uses.
            constant(
                "the number of card types among cards in all graveyards",
                DynamicAmount.AggregateZone(Player.Each, Zone.GRAVEYARD, aggregation = Aggregation.DISTINCT_TYPES),
            ),
            constant(
                "the number of card types among cards in your graveyard",
                DynamicAmount.AggregateZone(Player.You, Zone.GRAVEYARD, aggregation = Aggregation.DISTINCT_TYPES),
            ),
            // "the number of +1/+1 counters on ~" — a tally of the source's own counters, which the SDK
            // reads as a property of an entity rather than as a count of a zone. The kind is a slot for
            // [Primitives.counterFilter]'s reason: `CounterTypeFilter` has dedicated cases for the
            // stat-changing kinds and a `Named` fallback for the rest, and one leaf spells both.
            counterCount,
        ),
    )

    /**
     * The multiplier layer — "**twice** the number of cards in your hand".
     *
     * One layer owning one field, exactly as [Filters]' suffixes each own one predicate: it strips
     * `Multiply`'s factor and hands the amount underneath back inwards. Only the doubling is
     * spelled, because that is the multiplier English has a single word for; "three times …" is a
     * different printed shape and would be a second row here, not a number slot in this one.
     */
    private val doubled: Phrase<DynamicAmount> = phrase("twice {amount}", name = "twice a count") {
        slot("amount", plainCount)
        build { DynamicAmount.Multiply(it.value("amount"), 2) }
        match { amount ->
            val product = amount as? DynamicAmount.Multiply ?: return@match null
            if (product.multiplier != 2) return@match null
            bind("amount" to product.amount)
        }
    }

    /** Everything a "where X is …" clause, or an "equal to …" one, can define. */
    val count: Phrase<DynamicAmount> = oneOf("a count", plainCount, doubled)

    // ---------------------------------------------------------------------------------------
    // A counter count that is not a numeral — the layer the three counter positions share
    // ---------------------------------------------------------------------------------------

    /**
     * **How many counters, when the answer is not a number word.**
     *
     * Oracle spells a counter count three ways and the SDK holds two types for them. A numeral is
     * `AddCountersEffect.count` / `EntersWithCounters.count`, both plain `Int`, and that is the only
     * one the three counter positions — [Steps.putCountersOnTargetPermanent],
     * [SelfSteps.putCounters], [Replacements.entersWithCounters] — could read. The other two are one
     * value behind two clauses:
     *
     * | Surface | Example |
     * |---|---|
     * | `, where X is …` | "~ enters with **X** +1/+1 counters on it**, where X is the number of lands you control**." |
     * | ` equal to …` | "~ enters with **a number of** +1/+1 counters on it **equal to the number of creature cards in all graveyards**." |
     *
     * Those are the same model — `AddDynamicCountersEffect` / `EntersWithDynamicCounters`, whose
     * `amount` is the [DynamicAmount] a numeral cannot hold — so they are one rule with two
     * spellings rather than two rules, which is what [definedByCount] registers. The counter count
     * is [Steps.countedStepPair]'s treatment arriving one family late: `Effects.AddDynamicCounters`
     * has been in the SDK the whole time with no caller here, and the difference between "put two
     * +1/+1 counters" and "put X +1/+1 counters" was never a rule, only an argument.
     *
     * ### `X` on its own is a position, and only one of the three positions has it
     *
     * "~ enters with X +1/+1 counters on it." names no count at all: the X is the one announced for
     * the spell, [DynamicAmount.XValue], and [Targets.upToXTargets] already writes down when that
     * reading is legal — the resolution context has to be live. It is, in the enters-with
     * replacement, and provably: `EntersWithReplacements` builds
     * `EffectContext(xValue = spellComponent.xValue)` on the self path, during the permanent spell's
     * own resolution, and ten hand-written cards with scenario tests asserting the counts read it
     * that way. The `otherOnly` branch of the same effect builds a context **without** `xValue` and
     * needs `CastX`, which is the same three-case rule with a fourth case rather than a new one.
     *
     * A *step*, though, does not know its position: [Triggers] and [Activated] lift these clauses,
     * and "whenever ~ attacks, put X +1/+1 counters on ~" carries no announced X — `XValue` there is
     * silently zero, and there is no `DynamicAmount` at all for the X of an arbitrary activated
     * ability. So the bare row is the enters position's alone, and the two positions that cannot
     * know take only the defined clauses, whose amount is a board tally and therefore reads the same
     * wherever the clause is lifted to. That is a declaration with a criterion, the way
     * [Targets.singularQuantifiers] is, not an omission.
     */
    const val WHERE_X = ", where X is {amount}"

    /** The quantity the [WHERE_X] spelling puts where a number word would go. */
    private const val LETTER = " X {kind} counters"

    /** The same counter phrase with the count named behind the noun instead of in front of it. */
    private const val NOUN_PHRASE = " a number of {kind} counters"

    /** ` equal to …`'s clause, which trails the object rather than following a comma. */
    private const val EQUAL_TO = " equal to {amount}"

    /**
     * The `equal to …` spelling of a [WHERE_X] counter template.
     *
     * Both markers are *required* rather than optional, so a template this does not apply to fails
     * at construction — every rule here is built during object initialization, which makes that the
     * first thing a test run reports. [Durations.fronted] is the same contract.
     */
    fun equalTo(template: String): String {
        require(template.contains(LETTER)) { "\"$template\" has no \"$LETTER\" to move behind the noun" }
        require(template.contains(WHERE_X)) { "\"$template\" has no \"$WHERE_X\" clause to respell" }
        return template.replace(LETTER, NOUN_PHRASE).replace(WHERE_X, EQUAL_TO)
    }

    /**
     * Whether a [DynamicAmount] is one a defined-X clause on a **counter count** may name. Two
     * refusals, for two unrelated reasons.
     *
     * ### The two number-word domains, which this must not overlap
     *
     * The three dynamic counter rules and the two fixed ones read the same sentence position, so
     * they have to partition [DynamicAmount] rather than be tried in order — the split
     * [Steps.countedStepPair] draws, for the reason written there. A `Fixed` amount is the numeral's
     * and [XValue][DynamicAmount.XValue] is the bare row's; everything else is a clause. Refusing
     * the other two here is belt to [count]'s braces, which cannot print either of them, and it is
     * the half that keeps a hand-written `AddDynamicCountersEffect(amount = Fixed(2))` declining
     * instead of coming back as a second reading of "put two +1/+1 counters".
     *
     * ### The source's own counter tally, which is last-known information half the time it is printed
     *
     * [counterCount] reads `EntityProperty(Source, CounterCount)`, and `DynamicAmountEvaluator`
     * resolves that from **live** state: `counterCountOf` looks the entity up and answers 0 when it
     * is not there. So in the position Oracle most often prints this clause — "When ~ dies, put X
     * +1/+1 counters on target creature you control, where X is the number of +1/+1 counters on ~"
     * (Servant of the Scale) — the source is already gone and the amount is silently zero. The SDK
     * has the right reading for that position, `DynamicAmount.LastKnownSourceCounters`, and the card
     * corpus has a third spelling again (`Effects.MoveAllLastKnownCounters`, which moves the pile
     * instead of counting it).
     *
     * Which of the three a line means is decided by the trigger it is lifted into, and a step cannot
     * see that — the same reason the bare `X` row is the enters position's alone. So this refuses the
     * live tally rather than emitting a model that evaluates to zero, and the translation belongs at
     * the lift in [Triggers], the one place the position is known. It is narrow: every other row of
     * [count] reads the board or a zone and means the same wherever the clause lands.
     */
    fun namesX(amount: DynamicAmount): Boolean = when {
        amount is DynamicAmount.Fixed -> false
        amount == DynamicAmount.XValue -> false
        amount == counterCountOfSelf(amount) -> false
        else -> true
    }

    /** [counterCount]'s model, for the [namesX] refusal — the source's own live counter tally. */
    private fun counterCountOfSelf(amount: DynamicAmount): DynamicAmount? {
        val property = amount as? DynamicAmount.EntityProperty ?: return null
        val counter = property.numericProperty as? EntityNumericProperty.CounterCount ?: return null
        return DynamicAmounts.countersOnSelf(counter.counterType).takeIf { it == amount }
    }

    /** "+1/+1 counters on it" / "+1/+1 counter on ~" — a tally of the source's own counters. */
    private val plusOneCounters: DynamicAmount = DynamicAmounts.countersOnSelf(CounterTypeFilter.PlusOnePlusOne)

    // ---------------------------------------------------------------------------------------
    // The clauses
    // ---------------------------------------------------------------------------------------

    /**
     * "Target creature gets +X/+X until end of turn, where X is the number of Elves on the
     * battlefield." — Timberwatch Elf, and Magma Sliver's granted "+X/+0" sibling.
     *
     * The *shape* of the modifier is the rule's parameter rather than a slot, because "+X/+X" and
     * "+X/+0" are two printed forms in which the letter appears a different number of times;
     * [Primitives.statModifiers] reads numerals and cannot spell either.
     */
    private fun pumpTargetByCount(
        modifier: String,
        toughness: (DynamicAmount) -> DynamicAmount,
        name: String,
    ): Phrase<CardScript> {
        fun scriptFor(amount: DynamicAmount, filter: GameObjectFilter) = CardScript(
            spellEffect = Effects.ModifyStats(amount, toughness(amount), Targets.bound()),
            targetRequirements = listOf(Targets.permanent(filter)),
        )
        return phrase("target {filter} gets $modifier until end of turn, where X is {amount}", name = name) {
            slot("filter", Filters.filter)
            slot("amount", count)
            build { scriptFor(it.value("amount"), it.value("filter")) }
            match { script ->
                val stats = script.spellEffect as? ModifyStatsEffect ?: return@match null
                val requirement = script.targetRequirements.singleOrNull() ?: return@match null
                val filter = Targets.permanentFilter(requirement) ?: return@match null
                if (script != scriptFor(stats.powerModifier, filter)) return@match null
                bind("filter" to filter, "amount" to stats.powerModifier)
            }
        }
    }

    /** "It gets +X/+X until end of turn, where X is the number of Clerics on the battlefield." */
    private val pumpSelfByCount: Phrase<CardScript> = run {
        fun scriptFor(amount: DynamicAmount) =
            CardScript(spellEffect = Effects.ModifyStats(amount, amount, EffectTarget.Self))
        phrase(
            "{self} gets +X/+X until end of turn, where X is {amount}",
            name = "the source gets a count",
        ) {
            slot("self", Primitives.self)
            slot("amount", count)
            build { scriptFor(it.value("amount")) }
            match { script ->
                val stats = script.spellEffect as? ModifyStatsEffect ?: return@match null
                if (script != scriptFor(stats.powerModifier)) return@match null
                bind("self" to Unit, "amount" to stats.powerModifier)
            }
        }
    }

    /**
     * "All creatures get -X/-X until end of turn." — Bane of the Living's morph payoff.
     *
     * `X` here is the spell's own `{X}`, not a count, so there is no where-clause and the amount is
     * a constant of the rule. The negation lives in the model as a `Multiply` by −1, which is what
     * makes the printed minus sign recoverable: a card printing "+X/+X" is the same rule with the
     * multiplier absent, and it declines here rather than losing the sign.
     */
    private fun groupPumpByX(prefix: String, name: String): Phrase<CardScript> {
        val amount = DynamicAmount.Multiply(DynamicAmount.XValue, -1)
        fun scriptFor(filter: GameObjectFilter) = CardScript(
            spellEffect = Effects.ForEachInGroup(
                GroupFilter(filter),
                Effects.ModifyStats(amount, amount, EffectTarget.Self),
            )
        )
        return phrase("$prefix{filter} get -X/-X until end of turn", name = name) {
            frontedDuration()
            slot("filter", Filters.plural)
            build { scriptFor(it.value("filter")) }
            match { script ->
                val filter = iteratedGroup(script.spellEffect) ?: return@match null
                if (script != scriptFor(filter)) return@match null
                bind("filter" to filter)
            }
        }
    }

    /**
     * "You draw X cards and you lose X life, where X is the number of Zombies you control." —
     * Graveborn Muse.
     *
     * One where-clause and two uses of it, which is why this is a rule rather than a
     * [Steps.sequence] of two: the sentence defines X once at its end, so neither half is a sentence
     * on its own and the join is "and" rather than a full stop.
     */
    private val drawAndLoseByCount: Phrase<CardScript> = run {
        fun scriptFor(amount: DynamicAmount) = CardScript(
            spellEffect = Effects.Composite(
                listOf(
                    Effects.DrawCards(amount, EffectTarget.Controller),
                    Effects.LoseLife(amount, EffectTarget.Controller),
                )
            )
        )
        phrase(
            "you draw X cards and you lose X life, where X is {amount}",
            name = "draw and lose a count",
        ) {
            slot("amount", count)
            build { scriptFor(it.value("amount")) }
            match { script ->
                val effects = (script.spellEffect as? CompositeEffect)?.effects ?: return@match null
                val amount = (effects.firstOrNull() as? DrawCardsEffect)?.count ?: return@match null
                if (script != scriptFor(amount)) return@match null
                bind("amount" to amount)
            }
        }
    }

    /** "That player mills X cards, where X is the number of cards in their hand." — Dreamborn Muse. */
    private val triggeringPlayerMillsByCount: Phrase<CardScript> = run {
        fun scriptFor(amount: DynamicAmount) = CardScript(
            spellEffect = Patterns.Library.mill(amount, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        )
        phrase("that player mills X cards, where X is {amount}", name = "the triggering player mills a count") {
            slot("amount", count)
            build { scriptFor(it.value("amount")) }
            match { script ->
                val amount = milledCount(script.spellEffect) ?: return@match null
                if (script != scriptFor(amount)) return@match null
                bind("amount" to amount)
            }
        }
    }

    /** "Add X mana of any one color, where X is the number of Elves on the battlefield." */
    private val addManaByCount: Phrase<CardScript> = run {
        fun scriptFor(amount: DynamicAmount) = CardScript(spellEffect = Effects.AddAnyColorMana(amount))
        phrase("add X mana of any one color, where X is {amount}", name = "add a count of mana") {
            slot("amount", count)
            build { scriptFor(it.value("amount")) }
            match { script ->
                val amount = addedManaAmount(script.spellEffect) ?: return@match null
                if (script != scriptFor(amount)) return@match null
                bind("amount" to amount)
            }
        }
    }

    /**
     * "Draw a card for each Wizard you control." — Riptide Director; "Draw a card for each attacking
     * creature." — Keep Watch.
     *
     * The same sentence over each row of [scopes]. It read only the first of them until the layer
     * was published, which is what put Keep Watch in the `.` decline family: its noun phrase ends
     * where this rule expected a clause.
     */
    private fun drawForEach(scope: Scope): Phrase<CardScript> {
        fun scriptFor(filter: GameObjectFilter) = CardScript(
            spellEffect = Effects.DrawCards(DynamicAmount.AggregateBattlefield(scope.player, filter))
        )
        return phrase("draw a card for each {filter}${scope.surface}", name = "draw for each of ${scope.where}") {
            slot("filter", Filters.filter)
            build { bindings ->
                scriptFor(scope.narrowing(bindings.value("filter")) ?: return@build null)
            }
            match { script ->
                val amount = (script.spellEffect as? DrawCardsEffect)?.count
                    as? DynamicAmount.AggregateBattlefield ?: return@match null
                if (amount.player != scope.player) return@match null
                if (script != scriptFor(amount.filter)) return@match null
                bind("filter" to (scope.narrowing(amount.filter) ?: return@match null))
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Counts of the source's own counters
    // ---------------------------------------------------------------------------------------

    /** "Target creature gets +1/+1 until end of turn for each +1/+1 counter on ~." — Canopy Crawler. */
    private val pumpTargetPerOwnCounter: Phrase<CardScript> = run {
        fun scriptFor(filter: GameObjectFilter) = CardScript(
            spellEffect = Effects.ModifyStats(plusOneCounters, plusOneCounters, Targets.bound()),
            targetRequirements = listOf(Targets.permanent(filter)),
        )
        phrase(
            "target {filter} gets +1/+1 until end of turn for each +1/+1 counter on {self}",
            name = "pump a target per counter on the source",
        ) {
            slot("filter", Filters.filter)
            slot("self", Primitives.self)
            build { scriptFor(it.value("filter")) }
            match { script ->
                val requirement = script.targetRequirements.singleOrNull() ?: return@match null
                val filter = Targets.permanentFilter(requirement) ?: return@match null
                if (script != scriptFor(filter)) return@match null
                bind("filter" to filter, "self" to Unit)
            }
        }
    }

    /** "You lose 1 life for each +1/+1 counter on it." — Embalmed Brawler. */
    private val loseLifePerOwnCounter: Phrase<CardScript> = run {
        val script = CardScript(spellEffect = Effects.LoseLife(plusOneCounters, EffectTarget.Controller))
        phrase("you lose 1 life for each +1/+1 counter on it", name = "lose life per counter on the source") {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * "You gain that much life." — Wall of Hope, after a damage trigger.
     *
     * "That much" is the damage the trigger reported, which the SDK names as a context property
     * rather than as a count of anything on the battlefield. It is a clause of its own rather than a
     * member of [count] because the phrase replaces the *whole* amount and has no "the number of…"
     * shape to slot.
     */
    private val gainThatMuchLife: Phrase<CardScript> = run {
        val script = CardScript(
            spellEffect = Effects.GainLife(
                DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
                EffectTarget.Controller,
            )
        )
        phrase("you gain that much life", name = "gain that much life") {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * "It gets +1/+1 until end of turn for each creature blocking it." — Berserk Murlodont.
     *
     * The "it" is the creature the *trigger* named, not the source and not a target: a Beast that
     * became blocked. So the reference is `EffectTarget.TriggeringEntity` and the count is the same
     * entity's blocker tally — one printed pronoun standing for one object in two places, which is
     * why nothing here is a slot.
     */
    private val triggeringGetsPerBlocker: Phrase<CardScript> = run {
        val blockers = DynamicAmounts.numberOfBlockers()
        val script = CardScript(
            spellEffect = Effects.ModifyStats(blockers, blockers, EffectTarget.TriggeringEntity)
        )
        phrase(
            "it gets +1/+1 until end of turn for each creature blocking it",
            name = "the triggering creature gets per blocker",
        ) {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    /**
     * "You may have target player lose life equal to the number of Zombies on the battlefield." —
     * Gempalm Polluter — and Gempalm Incinerator's damage sibling.
     *
     * "You may have <someone> <verb>" is Oracle's causative: the controller chooses, and the thing
     * that happens is aimed at the target. The model is the plain optional wrapper around the verb,
     * so what the phrasing buys is nothing the model needs — which is why the whole sentence is the
     * rule and only the amount and the noun phrase are slots.
     */
    private fun mayHaveTargetSuffer(
        template: String,
        name: String,
        requirement: () -> com.wingedsheep.sdk.scripting.targets.TargetRequirement,
        filtered: Boolean,
        effect: (DynamicAmount) -> Effect,
    ): Phrase<CardScript> {
        fun scriptFor(amount: DynamicAmount, target: com.wingedsheep.sdk.scripting.targets.TargetRequirement) =
            CardScript(
                spellEffect = com.wingedsheep.sdk.scripting.effects.MayEffect(effect(amount)),
                targetRequirements = listOf(target),
            )
        return phrase(template, name = name) {
            if (filtered) slot("filter", Filters.filter)
            slot("amount", count)
            build { bindings ->
                val target = if (filtered) Targets.permanent(bindings.value("filter")) else requirement()
                scriptFor(bindings.value("amount"), target)
            }
            match { script ->
                val gated = script.spellEffect as? com.wingedsheep.sdk.scripting.effects.GatedEffect
                    ?: return@match null
                val amount = amountOf(gated.then) ?: return@match null
                val target = script.targetRequirements.singleOrNull() ?: return@match null
                if (script != scriptFor(amount, target)) return@match null
                if (filtered) {
                    val filter = Targets.permanentFilter(target) ?: return@match null
                    bind("filter" to filter, "amount" to amount)
                } else {
                    if (target != requirement()) return@match null
                    bind("amount" to amount)
                }
            }
        }
    }

    /** The amount a life-loss or damage effect carries, whichever of the two it is. */
    private fun amountOf(effect: Effect): DynamicAmount? = when (effect) {
        is LoseLifeEffect -> effect.amount
        is DealDamageEffect -> effect.amount
        else -> null
    }

    val clauses: List<Phrase<CardScript>> = listOf(
        pumpTargetByCount("+X/+X", { it }, "pump a target by a count"),
        pumpTargetByCount("+X/+0", { DynamicAmount.Fixed(0) }, "pump a target's power by a count"),
        pumpSelfByCount,
        groupPumpByX("", "a group gets minus X"),
        groupPumpByX("all ", "all of a group gets minus X"),
        drawAndLoseByCount,
        triggeringPlayerMillsByCount,
        addManaByCount,
        pumpTargetPerOwnCounter,
        loseLifePerOwnCounter,
        gainThatMuchLife,
        triggeringGetsPerBlocker,
        mayHaveTargetSuffer(
            "you may have target player lose life equal to {amount}",
            "you may have a player lose life",
            requirement = { Targets.player() },
            filtered = false,
        ) { Effects.LoseLife(it, Targets.bound()) },
        mayHaveTargetSuffer(
            "you may have it deal X damage to target {filter}, where X is {amount}",
            "you may have the source deal damage",
            requirement = { Targets.any() },
            filtered = true,
        ) { Effects.DealDamage(it, Targets.bound()) },
    ) + perScope(::drawForEach)

    // ---------------------------------------------------------------------------------------
    // Model helpers
    // ---------------------------------------------------------------------------------------

    private fun iteratedGroup(effect: Effect?): GameObjectFilter? {
        val forEach = effect as? com.wingedsheep.sdk.scripting.effects.ForEachEffect ?: return null
        val space = forEach.space as? com.wingedsheep.sdk.scripting.effects.IterationSpace.Group ?: return null
        return space.filter.baseFilter
    }

    /** The count a `Patterns.Library.mill` pipeline moves, read off its gather step. */
    private fun milledCount(effect: Effect?): DynamicAmount? {
        val gather = (effect as? CompositeEffect)?.effects?.firstOrNull()
            as? com.wingedsheep.sdk.scripting.effects.GatherCardsEffect ?: return null
        return (gather.source as? com.wingedsheep.sdk.scripting.effects.CardSource.TopOfLibrary)?.count
    }

    private fun addedManaAmount(effect: Effect?): DynamicAmount? =
        (effect as? com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect)?.amount
}

/**
 * Registers the `equal to …` spelling of a `, where X is …` counter rule: one line, derived from the
 * rule's own template, parsing to the same model and never printing.
 *
 * Which of the two prints is a corpus count and nothing deeper — 48 printed lines put the clause
 * behind a comma against roughly half that many behind the noun — so the majority spelling is
 * canonical and a card printing the other comes back as a variant rather than a decline. What must
 * not be two is the script, the reader and the fail-closed reconstruction, which is exactly what
 * [com.wingedsheep.assay.syntax.PhraseBuilder.alsoSpelled] shares.
 */
fun PhraseBuilder<*>.definedByCount() {
    alsoSpelled(Amounts.equalTo(template), "${ruleName ?: template} (count behind the noun)")
}
