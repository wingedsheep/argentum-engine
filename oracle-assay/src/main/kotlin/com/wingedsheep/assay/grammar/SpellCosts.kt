package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.constant
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions as SdkConditions
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * What a spell costs, and what changes it — the [ModifySpellCost] family.
 *
 * One printed sentence, three independent axes, and that is the whole reason this is a file rather
 * than more rows in [Statics]:
 *
 * ```
 *   <subject> cost(s) <amount> less|more to cast <clause>.
 *      │                │           │              └── the clause: nothing, a condition, a count,
 *      │                │           │                   a target test, or a named variable
 *      │                │           └── the direction, which the SDK spells as two families
 *      │                └── generic or coloured, which the SDK also spells as two families
 *      └── whose spells: this one, yours, everyone's
 * ```
 *
 * `ModifySpellCost(target, modification, gating)` has exactly those three fields, so the grammar is
 * the product of three small vocabularies rather than one rule per printed sentence — the same move
 * the cost band made when it read `CostAtom` as "the one cost language" instead of writing a second
 * one. The subject is a *slot* ([subject]) shared by every sentence below, which is why adding
 * "Creature spells you cast cost {1} less to cast." cost one row rather than a parallel set.
 *
 * ### Where the corpus spells one thing two ways
 *
 * `CostReductionSource` carries five `FixedIf…` cases — [CostReductionSource.FixedIfControlFilter],
 * `FixedIfAnyTargetMatches`, `FixedIfCreatureAttackingYou`, `FixedIfCreatureDiedThisTurn`,
 * `FixedIfVoid` — plus a plain [CostReductionSource.Fixed]. Every one of them says "reduce by *n*
 * when *P* holds", which is also exactly what `ReduceGeneric(n)` under
 * [CostGating.OnlyIf] says, and the hand-written corpus is split between the two spellings: 21 cards
 * write Bolt Bend's sentence as `Fixed…` sources and 21 write the same shape as an `OnlyIf` gate.
 * That is genuine ambiguity — one meaning, two models — so the grammar emits exactly one of them and
 * the other is reported rather than routed around.
 *
 * **The gate is canonical**, for a reason that is about reach rather than about counting cards: its
 * condition slot is the whole of [Conditions], so one rule reads *every* "… if <condition>" sentence
 * the grammar will ever know, while the `FixedIf…` cases are five sentences that will never be six
 * without an SDK change. The exception is [CostReductionSource.FixedIfAnyTargetMatches], which stays
 * canonical for "if it targets …" because the thing it tests — the spell's own target list — is not
 * a `Condition` at all and has no gate spelling.
 *
 * So `Fixed`, `FixedIfControlFilter`, `FixedIfCreatureAttackingYou`, `FixedIfCreatureDiedThisTurn`
 * and `FixedIfVoid` are values this grammar never produces, and a golden that carries one diverges
 * on purpose. That divergence is the finding: **the `FixedIf…` family duplicates `OnlyIf` and should
 * fold into it**, and until it does, the split means two cards printing the same sentence can carry
 * different models. Declining the minority spelling also polices its membership, exactly as
 * [Mana]'s omission of `ManaColorSet.Specific` does.
 *
 * The same argument runs one level down. [CostModification.ReduceColoredPerUnit] and
 * [CostModification.ReduceColoredIfAnyTargetMatches] restate "for each …" and "if it targets …" for
 * a *coloured* reduction, and the corpus uses them twice against forty times for the
 * `ReduceGenericBy` spelling. The generic spelling is canonical here too; a genuinely coloured
 * reduction with one of those clauses declines and is counted.
 *
 * ### Why "for each …" and ", where X is …" are one modification and two sentences
 *
 * Both build [CostModification.ReduceGenericBy], and printing is still determined because the two
 * clause vocabularies take **disjoint `CostReductionSource` cases**: [perUnitSource] emits only the
 * three counting sources, [namedVariableSource] only the three aggregating ones. A source registered
 * in both would give one model two printed forms — which is why
 * [CostReductionSource.PermanentsYouControlMatching] is *not* a `, where X is the number of …` row
 * even though its own `description` reads like one.
 */
object SpellCosts {

    /** The `{X}` a named-variable reduction spells where the other sentences spell a number. */
    private val X_COST: ManaCost = ManaCost.parse("{X}")

    // -------------------------------------------------------------------------------------------
    // The subject — whose spells the sentence is about
    // -------------------------------------------------------------------------------------------

    /**
     * The candidate filter a subject narrows by, for the reconstruction in [filteredSubject] to
     * check. `SelfCast` and the zone-scoped subjects have none and decline here.
     */
    private fun subjectFilter(target: SpellCostTarget): GameObjectFilter? = when (target) {
        is SpellCostTarget.YouCast -> target.filter
        is SpellCostTarget.AnyCaster -> target.filter
        else -> null
    }

    /**
     * "Creature spells you cast cost …", "Noncreature spells cost …".
     *
     * The unfiltered spellings are separate [constant] rows rather than this rule with an empty
     * filter, and the guard against [GameObjectFilter.Any] here is what keeps them one printed form
     * each: without it, `YouCast(Any)` could print either "spells you cast" or whatever the noun
     * phrase spells `Any` as, and the printer would choose.
     */
    private fun filteredSubject(
        template: String,
        name: String,
        wrap: (GameObjectFilter) -> SpellCostTarget,
    ): Phrase<SpellCostTarget> = phrase(template, name = name) {
        slot("filter", Filters.spellQuality)
        build { bindings ->
            val filter = bindings.value<GameObjectFilter>("filter")
            if (!spellQuality(filter)) return@build null
            wrap(filter)
        }
        match { value ->
            val filter = subjectFilter(value) ?: return@match null
            if (!spellQuality(filter) || value != wrap(filter)) return@match null
            bind("filter" to filter)
        }
    }

    /**
     * What a filter may say about a **spell**, which is narrower than what it may say about a
     * permanent in two ways the model does not mark.
     *
     * A `StatePredicate` — tapped, attacking, face-down — is a fact about a permanent *on the
     * battlefield*, and a spell on the stack has none of those states. `GameObjectFilter.Any` is
     * excluded for the printing reason instead: the unfiltered subjects are their own [constant]
     * rows ("spells you cast"), so admitting `Any` here would give one value two printed forms.
     *
     * The state guard is what makes Dream Chisel decline rather than round-trip wrongly. Its
     * "Face-down creature spells you cast cost {1} less to cast." has a dedicated
     * `SpellCostTarget.FaceDownYouCast`, and reading it as a creature filter with `IsFaceDown` on
     * it is a *different* value that prints back byte-identically — the reversible-but-wrong class,
     * caught by the differential and closed here rather than folded.
     */
    private fun spellQuality(filter: GameObjectFilter): Boolean =
        filter != GameObjectFilter.Any && filter.statePredicates.isEmpty()

    /**
     * The subject *and its verb*, because English conjugates it: "This spell **costs**" against
     * "Creature spells you cast **cost**". The verb is not in the model and there is nowhere to put
     * it, so it belongs to whichever rule spells the noun — the same reasoning that keeps the
     * article inside [Filters.indefinite].
     */
    private val subject: Phrase<SpellCostTarget> = oneOf(
        "whose spells cost",
        constant<SpellCostTarget>("this spell costs", SpellCostTarget.SelfCast),
        constant<SpellCostTarget>("spells you cast cost", SpellCostTarget.YouCast(GameObjectFilter.Any)),
        constant<SpellCostTarget>("spells cost", SpellCostTarget.AnyCaster(GameObjectFilter.Any)),
        filteredSubject("{filter} spells you cast cost", "a spell type you cast") {
            SpellCostTarget.YouCast(it)
        },
        filteredSubject("{filter} spells cost", "a spell type anyone casts") {
            SpellCostTarget.AnyCaster(it)
        },
    )

    // -------------------------------------------------------------------------------------------
    // The counted noun phrases
    // -------------------------------------------------------------------------------------------

    /**
     * "You control" is a **layer of the noun phrase in the text and a property of the source type in
     * the model**, and these two functions are the one place that difference is crossed.
     *
     * Every golden agrees on it: Dross Golem's "for each Swamp you control" is
     * `PermanentsYouControlMatching(land Swamp)` — the scope in the source's *name*, the filter
     * carrying no controller predicate at all. But the printed noun phrase carries it, because
     * [Filters] owns "you control" as its outermost layer and there is exactly one rule that may
     * print that field.
     *
     * So the "you control" rules below consume the whole noun phrase and **strip** the predicate on
     * the way in, restoring it on the way out — the same one-layer-owns-one-field discipline
     * [Filters] itself uses, applied across the boundary between a filter and the type that holds
     * it. Spelling " you control" as a trailing literal instead would print "for each Swamp you
     * control you control" for one value and re-parse it as a different filter: reversible, and
     * wrong.
     */
    private fun controlledScope(printed: GameObjectFilter): GameObjectFilter? =
        printed.takeIf { it.controllerPredicate == ControllerPredicate.ControlledByYou }
            ?.copy(controllerPredicate = null)

    /** [controlledScope]'s inverse — the noun phrase a "you control" source prints. */
    private fun controlledPrinted(model: GameObjectFilter): GameObjectFilter? =
        model.takeIf { it.controllerPredicate == null }
            ?.copy(controllerPredicate = ControllerPredicate.ControlledByYou)

    /** A noun phrase whose clause says nothing about a controller must not carry one either. */
    private fun scopeFree(filter: GameObjectFilter): Boolean = filter.controllerPredicate == null

    /**
     * The sources a "for each …" clause can name, **always spelled with a per-unit amount of one**.
     *
     * The amount is not in this phrase because it is not in this phrase's text: the sentence spells
     * it once, before "less to cast", where [perUnit] owns it. So the slot's value space is exactly
     * the one-per sources, and [perUnit] re-scales the parsed value with [withPerUnitAmount] and
     * normalizes the printed one back down. Two slots, one number, and the reconstruction in
     * [perUnit] is what proves the two agree.
     */
    private val perUnitSource: Phrase<CostReductionSource> = oneOf(
        "something to count",
        countedRule("{filter}", "permanents you control", controlled = true) { filter ->
            CostReductionSource.PermanentsYouControlMatching(filter)
        },
        countedRule("{filter} on the battlefield", "permanents on the battlefield") { filter ->
            CostReductionSource.PermanentsOnBattlefieldMatching(filter)
        },
        // "This spell costs {1} less to cast for each attacking creature." — Stone Idol Trap. The
        // same source with the clause left off, which is [Amounts.Scope]'s empty row: English omits
        // "on the battlefield" and means it, so this spelling parses and the row above prints. It is
        // kept apart from the "you control" row above by the noun phrase alone — that one requires a
        // controller predicate and `countedRule`'s `scopeFree` here refuses one — so one text still
        // has one reading.
        alternate(
            countedRule("{filter}", "permanents on the battlefield (unqualified)") { filter ->
                CostReductionSource.PermanentsOnBattlefieldMatching(filter)
            }
        ),
        countedRule(
            "{filter} in your graveyard",
            "cards in your graveyard",
            noun = Filters.cardNoun,
        ) { filter ->
            CostReductionSource.CardsInGraveyardMatchingFilter(filter, 1)
        },
        countedRule(
            "{filter} in your graveyard and in exile",
            "cards in your graveyard and in exile",
            noun = Filters.cardNoun,
        ) { filter ->
            CostReductionSource.CardsInGraveyardAndExileMatchingFilter(filter, 1)
        },
    )

    /**
     * The per-card amount a counting source carries — 1 for the two that have nowhere to hold one.
     * `null` where the value is not a counting source at all.
     */
    private fun perUnitAmount(source: CostReductionSource): Int? = when (source) {
        is CostReductionSource.PermanentsYouControlMatching -> 1
        is CostReductionSource.PermanentsOnBattlefieldMatching -> 1
        is CostReductionSource.CardsInGraveyardMatchingFilter -> source.amountPerCard
        is CostReductionSource.CardsInGraveyardAndExileMatchingFilter -> source.amountPerCard
        else -> null
    }

    /**
     * [perUnitAmount]'s inverse: the same source scaled to [amount] per unit, or null where the
     * source cannot hold one — "{2} less for each Swamp you control" has nowhere to put the 2, so it
     * declines rather than being read as {1}.
     */
    private fun withPerUnitAmount(source: CostReductionSource, amount: Int): CostReductionSource? =
        when (source) {
            is CostReductionSource.CardsInGraveyardMatchingFilter -> source.copy(amountPerCard = amount)
            is CostReductionSource.CardsInGraveyardAndExileMatchingFilter ->
                source.copy(amountPerCard = amount)

            else -> source.takeIf { amount == 1 && perUnitAmount(it) != null }
        }

    /** One row of [perUnitSource]: a noun phrase and the source it counts, checked by rebuilding. */
    private fun countedRule(
        template: String,
        name: String,
        controlled: Boolean = false,
        // The noun the row counts: permanents for the battlefield rows, *cards* for the graveyard
        // ones, which is a different noun phrase and not the same one with a word after it.
        noun: Phrase<GameObjectFilter> = Filters.filter,
        source: (GameObjectFilter) -> CostReductionSource?,
    ): Phrase<CostReductionSource> = phrase(template, name = name) {
        slot("filter", noun)
        build { bindings ->
            val printed = bindings.value<GameObjectFilter>("filter")
            val filter = if (controlled) controlledScope(printed) else printed.takeIf { scopeFree(it) }
            source(filter ?: return@build null)
        }
        match { value ->
            val filter = countedFilter(value) ?: return@match null
            if (value != source(filter)) return@match null
            val printed = if (controlled) controlledPrinted(filter) else filter.takeIf { scopeFree(it) }
            bind("filter" to (printed ?: return@match null))
        }
    }

    /** The filter a counting source narrows by — a candidate; [countedRule] decides. */
    private fun countedFilter(source: CostReductionSource): GameObjectFilter? = when (source) {
        is CostReductionSource.PermanentsYouControlMatching -> source.filter
        is CostReductionSource.PermanentsOnBattlefieldMatching -> source.filter
        is CostReductionSource.CardsInGraveyardMatchingFilter -> source.filter
        is CostReductionSource.CardsInGraveyardAndExileMatchingFilter -> source.filter
        else -> null
    }

    /** "power", "toughness", "mana value" — the characteristics an aggregate clause names. */
    private val property: Phrase<EntityNumericProperty> = oneOf(
        "a numeric characteristic",
        constant("power", EntityNumericProperty.Power),
        constant("toughness", EntityNumericProperty.Toughness),
        constant("mana value", EntityNumericProperty.ManaValue),
    )

    /**
     * What `X` stands for in ", where X is …" — the aggregating sources, disjoint from
     * [perUnitSource]'s counting ones so that one `ReduceGenericBy` value has one printed sentence.
     */
    private val namedVariableSource: Phrase<CostReductionSource> = oneOf(
        "a named variable",
        aggregateRule("the greatest {prop} among {filter}", "the greatest of a property") { prop, filter ->
            CostReductionSource.GreatestPropertyAmongPermanentsYouControl(prop, filter)
        },
        aggregateRule("the total {prop} of {filter}", "the total of a property") { prop, filter ->
            CostReductionSource.TotalPropertyAmongPermanentsYouControl(prop, filter)
        },
        phrase("the number of differently named {filter}", name = "differently named permanents") {
            slot("filter", Filters.plural)
            build { bindings ->
                val filter = controlledScope(bindings.value("filter")) ?: return@build null
                CostReductionSource.DifferentlyNamedPermanentsYouControl(filter)
            }
            match { value ->
                val named = value as? CostReductionSource.DifferentlyNamedPermanentsYouControl
                    ?: return@match null
                if (value != CostReductionSource.DifferentlyNamedPermanentsYouControl(named.filter)) {
                    return@match null
                }
                bind("filter" to (controlledPrinted(named.filter) ?: return@match null))
            }
        },
    )

    /** One row of [namedVariableSource] over the shared `(property, filter)` axes. */
    private fun aggregateRule(
        template: String,
        name: String,
        source: (EntityNumericProperty, GameObjectFilter) -> CostReductionSource,
    ): Phrase<CostReductionSource> = phrase(template, name = name) {
        slot("prop", property)
        slot("filter", Filters.plural)
        build { bindings ->
            val filter = controlledScope(bindings.value("filter")) ?: return@build null
            source(bindings.value("prop"), filter)
        }
        match { value ->
            val (prop, filter) = aggregateParts(value) ?: return@match null
            if (value != source(prop, filter)) return@match null
            bind("prop" to prop, "filter" to (controlledPrinted(filter) ?: return@match null))
        }
    }

    /** The `(property, filter)` an aggregating source carries — a candidate; [aggregateRule] decides. */
    private fun aggregateParts(
        source: CostReductionSource,
    ): Pair<EntityNumericProperty, GameObjectFilter>? = when (source) {
        is CostReductionSource.GreatestPropertyAmongPermanentsYouControl -> source.property to source.filter
        is CostReductionSource.TotalPropertyAmongPermanentsYouControl -> source.property to source.filter
        else -> null
    }

    // -------------------------------------------------------------------------------------------
    // The sentences
    // -------------------------------------------------------------------------------------------

    /**
     * Which way the cost moves. Two words in the text and two *families* in the model, because the
     * SDK names the reduce and increase cases separately rather than signing an amount.
     */
    private enum class Direction(val word: String) { LESS("less"), MORE("more") }

    /**
     * "Noncreature spells cost {1} more to cast." — Glowrider — and "Cleric spells you cast cost
     * {W}{B} less to cast." — Edgewalker.
     *
     * Generic and coloured are one rule and two models, split on the *printed cost* rather than on
     * the sentence: `{1}` is `ReduceGeneric(1)` and `{W}{B}` is `ReduceColored("{W}{B}")`, which are
     * disjoint domains and therefore leave the printer nothing to choose. A `ReduceColored("{1}")`
     * — a coloured modification holding a cost with no colour in it — refuses to print, because the
     * reconstruction below rebuilds a generic modification from that cost and finds it unequal.
     */
    private fun fixedAmount(direction: Direction): Phrase<StaticAbility> = phrase(
        "{subject} {cost} ${direction.word} to cast.",
        name = "spells cost a fixed amount ${direction.word}",
    ) {
        slot("subject", subject)
        slot("cost", Primitives.manaCost)
        build { bindings ->
            val cost = bindings.value<ManaCost>("cost")
            val modification = fixedModification(direction, cost) ?: return@build null
            ModifySpellCost(bindings.value("subject"), modification)
        }
        match { value ->
            val modify = value as? ModifySpellCost ?: return@match null
            val cost = fixedCost(direction, modify.modification) ?: return@match null
            val modification = fixedModification(direction, cost) ?: return@match null
            if (value != ModifySpellCost(modify.target, modification)) return@match null
            bind("subject" to modify.target, "cost" to cost)
        }
    }

    /** The modification a printed cost denotes, generic where it can be and coloured otherwise. */
    private fun fixedModification(direction: Direction, cost: ManaCost): CostModification? {
        val generic = Primitives.genericAmount(cost)
        return when {
            generic != null && direction == Direction.LESS -> CostModification.ReduceGeneric(generic)
            generic != null -> CostModification.IncreaseGeneric(generic)
            cost == X_COST -> null
            direction == Direction.LESS -> CostModification.ReduceColored(cost.toString())
            else -> CostModification.IncreaseColored(cost.toString())
        }
    }

    /** [fixedModification]'s inverse — a candidate cost, which the rule then rebuilds and compares. */
    private fun fixedCost(direction: Direction, modification: CostModification): ManaCost? = when {
        direction == Direction.LESS && modification is CostModification.ReduceGeneric ->
            modification.amount.takeIf { it >= 0 }?.let { ManaCost.parse("{$it}") }

        direction == Direction.MORE && modification is CostModification.IncreaseGeneric ->
            modification.amount.takeIf { it >= 0 }?.let { ManaCost.parse("{$it}") }

        direction == Direction.LESS && modification is CostModification.ReduceColored ->
            runCatching { ManaCost.parse(modification.symbols) }.getOrNull()

        direction == Direction.MORE && modification is CostModification.IncreaseColored ->
            runCatching { ManaCost.parse(modification.symbols) }.getOrNull()

        else -> null
    }

    /**
     * "This spell costs {2} less to cast if you control a Dragon." — the whole of [Conditions] in a
     * cost sentence, and the reason the gate rather than `FixedIfControlFilter` is canonical.
     *
     * Only the reducing direction exists: no printed card raises its own cost under a condition, and
     * a rule with no card behind it is a rule with nothing to prove.
     */
    private val conditional: Phrase<StaticAbility> = phrase(
        "{subject} {cost} less to cast if {cond}.",
        name = "spells cost less under a condition",
    ) {
        slot("subject", subject)
        slot("cost", Primitives.manaCost)
        slot("cond", Conditions.condition)
        build { bindings ->
            val generic = Primitives.genericAmount(bindings.value("cost")) ?: return@build null
            ModifySpellCost(
                target = bindings.value("subject"),
                modification = CostModification.ReduceGeneric(generic),
                gating = CostGating.OnlyIf(bindings.value("cond")),
            )
        }
        match { value ->
            val modify = value as? ModifySpellCost ?: return@match null
            val reduce = modify.modification as? CostModification.ReduceGeneric ?: return@match null
            if (reduce.amount < 0) return@match null
            val gate = modify.gating as? CostGating.OnlyIf ?: return@match null
            if (value != ModifySpellCost(modify.target, reduce, gate)) return@match null
            bind(
                "subject" to modify.target,
                "cost" to ManaCost.parse("{${reduce.amount}}"),
                "cond" to gate.condition,
            )
        }
    }

    /**
     * "This spell costs {3} less to cast if it targets a tapped creature." — Grounded for Life, and
     * forty-six more lines.
     *
     * The one clause whose test is *not* a `Condition`: it reads the spell's own target list, which
     * `CostGating.OnlyIf` has no way to reach. So this is the one `FixedIf…` source the grammar
     * keeps, and the argument that folds the other four does not apply to it.
     */
    private fun targetMatching(direction: Direction): Phrase<StaticAbility> = phrase(
        "{subject} {cost} ${direction.word} to cast if it targets {filter}.",
        name = "spells cost ${direction.word} when they target something",
    ) {
        slot("subject", subject)
        slot("cost", Primitives.manaCost)
        slot("filter", Filters.indefinite)
        build { bindings ->
            val generic = Primitives.genericAmount(bindings.value("cost")) ?: return@build null
            ModifySpellCost(
                target = bindings.value("subject"),
                modification = targetModification(direction, generic, bindings.value("filter")),
            )
        }
        match { value ->
            val modify = value as? ModifySpellCost ?: return@match null
            val parts = targetParts(direction, modify.modification) ?: return@match null
            val (amount, filter) = parts
            if (amount < 0) return@match null
            if (value != ModifySpellCost(modify.target, targetModification(direction, amount, filter))) {
                return@match null
            }
            bind(
                "subject" to modify.target,
                "cost" to ManaCost.parse("{$amount}"),
                "filter" to filter,
            )
        }
    }

    private fun targetModification(
        direction: Direction,
        amount: Int,
        filter: GameObjectFilter,
    ): CostModification = when (direction) {
        Direction.LESS ->
            CostModification.ReduceGenericBy(CostReductionSource.FixedIfAnyTargetMatches(amount, filter))

        Direction.MORE -> CostModification.IncreaseGenericIfAnyTargetMatches(amount, filter)
    }

    /** [targetModification]'s inverse — a candidate pair, which [targetMatching] rebuilds and compares. */
    private fun targetParts(
        direction: Direction,
        modification: CostModification,
    ): Pair<Int, GameObjectFilter>? = when {
        direction == Direction.LESS && modification is CostModification.ReduceGenericBy -> {
            val source = modification.source as? CostReductionSource.FixedIfAnyTargetMatches
            source?.let { it.amount to it.filter }
        }

        direction == Direction.MORE && modification is CostModification.IncreaseGenericIfAnyTargetMatches ->
            modification.amount to modification.filter

        else -> null
    }

    /**
     * "This spell costs {1} less to cast for each creature on the battlefield." — Vanquish the
     * Horde, and eighty-five more lines.
     *
     * The reduction's amount reaches [perUnitSource] as an argument rather than as a second slot,
     * because only one of the counting sources can hold it. That is the [Keywords.keywordRun]
     * lesson applied to a different family: the number belongs to the *slot* that can carry it, and
     * a family with nowhere to put it declines rather than dropping it.
     */
    private val perUnit: Phrase<StaticAbility> = phrase(
        "{subject} {cost} less to cast for each {counted}.",
        name = "spells cost less for each of something",
    ) {
        slot("subject", subject)
        slot("cost", Primitives.manaCost)
        slot("counted", perUnitSource)
        build { bindings ->
            val generic = Primitives.genericAmount(bindings.value("cost")) ?: return@build null
            val source = withPerUnitAmount(bindings.value("counted"), generic) ?: return@build null
            ModifySpellCost(
                target = bindings.value("subject"),
                modification = CostModification.ReduceGenericBy(source),
            )
        }
        match { value ->
            val modify = value as? ModifySpellCost ?: return@match null
            val reduce = modify.modification as? CostModification.ReduceGenericBy ?: return@match null
            val amount = perUnitAmount(reduce.source)?.takeIf { it >= 0 } ?: return@match null
            val counted = withPerUnitAmount(reduce.source, 1) ?: return@match null
            if (value != ModifySpellCost(
                    modify.target,
                    CostModification.ReduceGenericBy(withPerUnitAmount(counted, amount) ?: return@match null),
                )
            ) {
                return@match null
            }
            bind(
                "subject" to modify.target,
                "cost" to ManaCost.parse("{$amount}"),
                "counted" to counted,
            )
        }
    }

    /**
     * "This spell costs {X} less to cast, where X is the total power of creatures you control." —
     * The Lord of the Eagles.
     *
     * The printed cost is the literal symbol `{X}`, read through [Primitives.manaCost] like every
     * other cost and then required to *be* `{X}`, rather than spelled into the template — a template
     * cannot hold a literal brace, and a rule that special-cased the lexer here would be a second
     * way to read a mana symbol.
     */
    private val namedVariable: Phrase<StaticAbility> = phrase(
        "{subject} {cost} less to cast, where X is {source}.",
        name = "spells cost less by a named variable",
    ) {
        slot("subject", subject)
        slot("cost", Primitives.manaCost)
        slot("source", namedVariableSource)
        build { bindings ->
            if (bindings.value<ManaCost>("cost") != X_COST) return@build null
            ModifySpellCost(
                target = bindings.value("subject"),
                modification = CostModification.ReduceGenericBy(bindings.value("source")),
            )
        }
        match { value ->
            val modify = value as? ModifySpellCost ?: return@match null
            val reduce = modify.modification as? CostModification.ReduceGenericBy ?: return@match null
            if (aggregateParts(reduce.source) == null &&
                reduce.source !is CostReductionSource.DifferentlyNamedPermanentsYouControl
            ) {
                return@match null
            }
            if (value != ModifySpellCost(modify.target, CostModification.ReduceGenericBy(reduce.source))) {
                return@match null
            }
            bind("subject" to modify.target, "cost" to X_COST, "source" to reduce.source)
        }
    }

    // -------------------------------------------------------------------------------------------
    // The leading gate
    // -------------------------------------------------------------------------------------------

    /**
     * "During your turn, spells you cast cost {1} less to cast for each creature you control with
     * power 4 or greater." — Temur Battlecrier — and Geyser Drake's mirror image.
     *
     * A *wrapper* over every ungated sentence rather than a variant of each, which is what keeps the
     * two turn conditions from multiplying the seven sentences into fourteen: the inner rule prints
     * the sentence and this one prints the clause in front of it. Reachable only where the inner
     * value's gating is [CostGating.None], so nothing here can silently replace a condition a
     * sentence already carries.
     *
     * These two conditions are deliberately **not** rows in [Conditions]. If they were, [conditional]
     * would print `OnlyIf(IsYourTurn)` as "… if it's your turn." and this rule would print the same
     * model as "During your turn, …" — two printed forms for one value, which is the ambiguity the
     * module's second invariant forbids. The turn clause lives in exactly one sentence position.
     */
    private fun leadingGate(template: String, name: String, condition: Condition): Phrase<StaticAbility> =
        phrase(template, name = name) {
            slot("inner", ungated)
            build { bindings ->
                val inner = bindings.value<StaticAbility>("inner") as? ModifySpellCost ?: return@build null
                if (inner.gating != CostGating.None) return@build null
                inner.copy(gating = CostGating.OnlyIf(condition))
            }
            match { value ->
                val modify = value as? ModifySpellCost ?: return@match null
                if (modify.gating != CostGating.OnlyIf(condition)) return@match null
                bind("inner" to modify.copy(gating = CostGating.None))
            }
        }

    /** Every sentence that carries no gating of its own — what [leadingGate] may wrap. */
    private val ungatedSentences: List<Phrase<StaticAbility>> = listOf(
        fixedAmount(Direction.LESS),
        fixedAmount(Direction.MORE),
        targetMatching(Direction.LESS),
        targetMatching(Direction.MORE),
        perUnit,
        namedVariable,
    )

    private val ungated: Phrase<StaticAbility> = oneOf("a spell cost modification", ungatedSentences)

    val all: List<Phrase<StaticAbility>> = ungatedSentences + listOf(
        conditional,
        leadingGate("during your turn, {inner}", "a spell cost modification on your turn", SdkConditions.IsYourTurn),
        leadingGate(
            "during turns other than yours, {inner}",
            "a spell cost modification off your turn",
            SdkConditions.IsNotYourTurn,
        ),
    )
}
