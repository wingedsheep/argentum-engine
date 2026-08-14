package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Unified static ability that modifies spell or morph-activation costs.
 *
 * Replaces the old per-shape classes (`ReduceSpellCostBySubtype`,
 * `ReduceSpellColoredCostBySubtype`, `ReduceSpellCostByFilter`, `SpellCostReduction`,
 * `FaceDownSpellCostReduction`, `ReduceFirstSpellOfTypeColoredCost`,
 * `IncreaseSpellCostByFilter`, `IncreaseSpellCostByPlayerSpellsCast`,
 * `IncreaseMorphCost`).
 *
 * @property target Which spells/costs the modifier applies to.
 * @property modification How the cost is changed (reduce/increase, generic/colored, fixed/dynamic).
 * @property gating Optional extra restriction (e.g. only the first matching spell each turn).
 */
@SerialName("ModifySpellCost")
@Serializable
data class ModifySpellCost(
    val target: SpellCostTarget,
    val modification: CostModification,
    val gating: CostGating = CostGating.None,
) : StaticAbility {
    override val description: String = buildDescription()

    private fun buildDescription(): String {
        // Only the Nth-of-type gate rephrases the subject as a single spell ("The second spell ...").
        // A conditional (OnlyIf) gate leaves the subject plural and tacks the condition on at the end.
        val nthGating = gating as? CostGating.NthOfTypePerTurn
        val gate = if (nthGating != null) "The ${ordinal(nthGating.n)} " else ""
        // A gated description ("The Nth ...") refers to a single spell, so phrase it in the singular.
        val noun = if (nthGating != null) "spell" else "spells"
        val subject = when (target) {
            SpellCostTarget.SelfCast -> "This spell"
            is SpellCostTarget.YouCast -> "${filterAdjective(target.filter)}$noun you cast"
            is SpellCostTarget.AnyCaster -> "${filterAdjective(target.filter)}$noun"
            is SpellCostTarget.OpponentsCastTargeting ->
                "Spells your opponents cast that target ${target.targetFilter.description}"
            is SpellCostTarget.OpponentsCastFromZones ->
                "Spells your opponents cast from ${describeZones(target.zones)}"
            is SpellCostTarget.YouCastFromZones ->
                "Spells you cast from ${describeZones(target.zones)}"
            SpellCostTarget.FaceDownYouCast -> "Face-down creature spells you cast"
            SpellCostTarget.MorphActivation -> "All morph costs"
        }
        val verb = when (modification) {
            is CostModification.ReduceGeneric -> "cost {${modification.amount}} less to cast"
            is CostModification.ReduceGenericBy -> "cost {X} less to cast, where X is ${modification.source.description}"
            is CostModification.ReduceColored -> "cost ${modification.symbols} less to cast"
            is CostModification.ReduceColoredPerUnit ->
                "cost ${modification.symbols} less to cast for each ${modification.countSource.description}"
            is CostModification.ReduceColoredIfAnyTargetMatches ->
                "cost ${modification.symbols} less to cast if it targets ${modification.filter.description}"
            is CostModification.IncreaseGeneric -> "cost {${modification.amount}} more"
            is CostModification.IncreaseColored -> "cost ${modification.symbols} more to cast"
            is CostModification.IncreaseGenericPerOtherSpellThisTurn ->
                "cost {${modification.amountPerSpell}} more to cast for each other spell that player has cast this turn"
            is CostModification.IncreaseGenericIfAnyTargetMatches ->
                "cost {${modification.amount}} more to cast if it targets ${modification.filter.description}"
            is CostModification.IncreaseLife -> "cost an additional ${modification.amount} life to cast"
        }
        val perTurn = if (nthGating != null) " each turn" else ""
        // The gated subject is singular, so make the verb agree ("cost" -> "costs").
        val agreedVerb = if (nthGating != null) verb.replaceFirst("cost ", "costs ") else verb
        val prefix = if (gate.isNotEmpty()) gate + subject.replaceFirstChar { it.lowercase() } else subject
        val conditionSuffix = when (val g = gating) {
            is CostGating.OnlyIf -> " ${g.condition.description}"
            else -> ""
        }
        return "$prefix $agreedVerb$perTurn$conditionSuffix"
    }

    // A filter that narrows nothing (e.g. GameObjectFilter.Any) describes itself as "card", which
    // reads wrong as an adjective ("the second card spell you cast"). Emit no adjective in that case
    // so the unconstrained form is just "spell(s)".
    private fun filterAdjective(filter: GameObjectFilter): String {
        val desc = filter.description
        return if (desc.isBlank() || desc == "card") "" else "$desc "
    }

    // Phrase a set of cast-from zones the way the oracle text reads — "graveyards or from exile".
    private fun describeZones(zones: Set<com.wingedsheep.sdk.core.Zone>): String {
        val parts = zones.toList().sortedBy { it.ordinal }.map { zone ->
            when (zone) {
                com.wingedsheep.sdk.core.Zone.GRAVEYARD -> "graveyards"
                com.wingedsheep.sdk.core.Zone.EXILE -> "exile"
                else -> zone.displayName
            }
        }
        return when (parts.size) {
            0 -> ""
            1 -> parts[0]
            else -> parts.dropLast(1).joinToString(", ") + " or from " + parts.last()
        }
    }

    private fun ordinal(n: Int): String = when (n) {
        1 -> "first"
        2 -> "second"
        3 -> "third"
        4 -> "fourth"
        5 -> "fifth"
        else -> {
            val suffix = when {
                n % 100 in 11..13 -> "th"
                n % 10 == 1 -> "st"
                n % 10 == 2 -> "nd"
                n % 10 == 3 -> "rd"
                else -> "th"
            }
            "$n$suffix"
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newTarget = target.applyTextReplacement(replacer)
        val newModification = modification.applyTextReplacement(replacer)
        return if (newTarget !== target || newModification !== modification) {
            copy(target = newTarget, modification = newModification)
        } else this
    }
}

/**
 * What the [ModifySpellCost] applies to.
 */
@Serializable
sealed interface SpellCostTarget {
    /** Identity by default; cases holding a filter override to recurse. */
    fun applyTextReplacement(replacer: TextReplacer): SpellCostTarget = this

    /** Self-reduction on the spell card itself — applies when this card is cast. */
    @SerialName("SelfCast")
    @Serializable
    data object SelfCast : SpellCostTarget {
    }

    /** Spells the source's controller casts that match the filter. */
    @SerialName("YouCast")
    @Serializable
    data class YouCast(val filter: GameObjectFilter) : SpellCostTarget {
        override fun applyTextReplacement(replacer: TextReplacer): SpellCostTarget {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /** Spells matching the filter cast by any player (global tax effect). */
    @SerialName("AnyCaster")
    @Serializable
    data class AnyCaster(val filter: GameObjectFilter) : SpellCostTarget {
        override fun applyTextReplacement(replacer: TextReplacer): SpellCostTarget {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Spells the source's controller casts **from one of [zones]**, matching [filter]
     * (default: any spell). The you-cast analogue of [OpponentsCastFromZones].
     *
     * Used for Doc Aurlock, Grizzled Genius ("Spells you cast from your graveyard or from
     * exile cost {2} less to cast") via
     * `YouCastFromZones(setOf(Zone.GRAVEYARD, Zone.EXILE))` paired with
     * [CostModification.ReduceGeneric]. The cost calculator matches it only when the casting
     * player controls the source and the spell is being cast from one of the named zones.
     */
    @SerialName("YouCastFromZones")
    @Serializable
    data class YouCastFromZones(
        val zones: Set<com.wingedsheep.sdk.core.Zone>,
        val filter: GameObjectFilter = GameObjectFilter.Any,
    ) : SpellCostTarget {
        override fun applyTextReplacement(replacer: TextReplacer): SpellCostTarget {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Spells opponents of the source's controller cast that target one or more
     * permanents matching [targetFilter] relative to the source.
     *
     * The filter is a [GroupFilter] so callers can express:
     *   - `GroupFilter.source()` — "this permanent" (Terror of the Peaks, Sphinx of New Prahv, ...)
     *   - `GroupFilter.AllCreaturesYouControl` — "creatures you control" (Kasmina, Kopala, ...)
     *   - `GroupFilter(GameObjectFilter.Creature.youControl().withKeyword(Keyword.FLYING))`
     *     — "flying creatures you control" (Jubilant Skybonder)
     *
     * Pair with [CostModification.IncreaseGeneric] for "{N} more" cards (Sphinx of New Prahv,
     * Boreal Elemental, Charix, ...) or [CostModification.IncreaseLife] for "N life to cast"
     * (Terror of the Peaks).
     */
    @SerialName("OpponentsCastTargeting")
    @Serializable
    data class OpponentsCastTargeting(val targetFilter: GroupFilter) : SpellCostTarget {
        override fun applyTextReplacement(replacer: TextReplacer): SpellCostTarget {
            val newFilter = targetFilter.applyTextReplacement(replacer)
            return if (newFilter !== targetFilter) copy(targetFilter = newFilter) else this
        }
    }

    /**
     * Spells opponents of the source's controller cast **from one of [zones]**, matching [filter]
     * (default: any spell). The zone-of-cast analogue of [OpponentsCastTargeting].
     *
     * Used for Aven Interrupter ("Spells your opponents cast from graveyards or from exile cost
     * {2} more to cast") via `OpponentsCastFromZones(setOf(Zone.GRAVEYARD, Zone.EXILE))` paired
     * with [CostModification.IncreaseGeneric]. The cost calculator matches it only when the
     * casting player is an opponent of the source's controller and the spell is being cast from
     * one of the named zones.
     */
    @SerialName("OpponentsCastFromZones")
    @Serializable
    data class OpponentsCastFromZones(
        val zones: Set<com.wingedsheep.sdk.core.Zone>,
        val filter: GameObjectFilter = GameObjectFilter.Any,
    ) : SpellCostTarget {
        override fun applyTextReplacement(replacer: TextReplacer): SpellCostTarget {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /** Face-down (morph) creature spells the source's controller casts. */
    @SerialName("FaceDownYouCast")
    @Serializable
    data object FaceDownYouCast : SpellCostTarget {
    }

    /** The morph (turn face-up) activated cost, applied globally. */
    @SerialName("MorphActivation")
    @Serializable
    data object MorphActivation : SpellCostTarget {
    }
}

/**
 * How a spell's cost is modified.
 */
@Serializable
sealed interface CostModification {
    fun applyTextReplacement(replacer: TextReplacer): CostModification = this

    /** Reduce generic mana by a fixed amount. */
    @SerialName("ReduceGeneric")
    @Serializable
    data class ReduceGeneric(val amount: Int) : CostModification

    /** Reduce generic mana by a dynamic amount sourced from the game state. */
    @SerialName("ReduceGenericBy")
    @Serializable
    data class ReduceGenericBy(val source: CostReductionSource) : CostModification

    /**
     * Remove specific colored mana symbols from the cost (e.g. `"{W}{B}"`).
     * Excess that cannot match is silently dropped (does NOT overflow to generic).
     */
    @SerialName("ReduceColored")
    @Serializable
    data class ReduceColored(val symbols: String) : CostModification

    /**
     * Remove `symbols` per unit of [countSource]. Excess overflows to generic reduction
     * (e.g. Eluge: "{U} less for each flood-counter land you control").
     */
    @SerialName("ReduceColoredPerUnit")
    @Serializable
    data class ReduceColoredPerUnit(
        val symbols: String,
        val countSource: CostReductionSource,
    ) : CostModification

    /**
     * Remove specific colored mana [symbols] from the cost if the spell targets any object
     * matching [filter]. The colored analogue of
     * [CostReductionSource.FixedIfAnyTargetMatches] (which only reduces generic), and the
     * reduction counterpart of [IncreaseGenericIfAnyTargetMatches].
     *
     * Used for cards like Brush Off ("This spell costs {1}{U} less to cast if it targets an
     * instant or sorcery spell") — pair this `{U}` reduction with a
     * `ReduceGenericBy(FixedIfAnyTargetMatches(1, filter))` for the `{1}` so both halves apply
     * together once a matching target is chosen.
     *
     * At cast resolution, the reduction applies if any of the spell's chosen targets match.
     * Like [CostReductionSource.FixedIfAnyTargetMatches], excess that cannot match is silently
     * dropped (does NOT overflow to generic).
     */
    @SerialName("ReduceColoredIfAnyTargetMatches")
    @Serializable
    data class ReduceColoredIfAnyTargetMatches(
        val symbols: String,
        val filter: GameObjectFilter,
    ) : CostModification {
        override fun applyTextReplacement(replacer: TextReplacer): CostModification {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /** Increase generic mana by a fixed amount (tax effect). */
    @SerialName("IncreaseGeneric")
    @Serializable
    data class IncreaseGeneric(val amount: Int) : CostModification

    /**
     * Add specific colored mana symbols to the cost (e.g. `"{W}"`), a colored tax effect.
     * Used for the Invasion "Leech" creatures ("White spells you cast cost {W} more to cast").
     */
    @SerialName("IncreaseColored")
    @Serializable
    data class IncreaseColored(val symbols: String) : CostModification

    /**
     * Damping-Sphere-style scaling tax: increase by `amountPerSpell` for each spell
     * the casting player has already cast this turn.
     */
    @SerialName("IncreaseGenericPerOtherSpellThisTurn")
    @Serializable
    data class IncreaseGenericPerOtherSpellThisTurn(
        val amountPerSpell: Int = 1,
    ) : CostModification

    /**
     * Increase generic mana by a fixed amount if the spell targets any object matching
     * the filter. The increase analogue of
     * [CostReductionSource.FixedIfAnyTargetMatches]; used for cards like Dragon's Prey
     * ("This spell costs {2} more to cast if it targets a Dragon").
     *
     * At cast resolution, the increase applies if any of the spell's chosen targets match.
     * For affordability enumeration (before targets are chosen) the increase is treated as
     * NOT applying, since the minimum possible cost is achieved by targeting something the
     * filter doesn't match.
     */
    @SerialName("IncreaseGenericIfAnyTargetMatches")
    @Serializable
    data class IncreaseGenericIfAnyTargetMatches(
        val amount: Int,
        val filter: GameObjectFilter,
    ) : CostModification {
        override fun applyTextReplacement(replacer: TextReplacer): CostModification {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Pay [amount] additional life as part of the casting cost (CR 601.2f).
     * Validated at cast time; the spell never reaches the stack if the caster
     * cannot pay. Currently only meaningful when paired with
     * [SpellCostTarget.OpponentsCastTargeting] (Terror of the Peaks).
     */
    @SerialName("IncreaseLife")
    @Serializable
    data class IncreaseLife(val amount: Int) : CostModification
}

/**
 * Optional gating restriction layered on top of [SpellCostTarget].
 */
@Serializable
sealed interface CostGating {
    /** No extra restriction — the modifier applies to every matching cast. */
    @SerialName("None")
    @Serializable
    data object None : CostGating

    /**
     * Modifier applies only when the matching spell being cast is the Nth such spell the casting
     * player has cast this turn (1-indexed; counts itself). Use `n = 1` for "the first ... each
     * turn" (e.g. Eluge) and `n = 2` for Uthros Psionicist's "the second spell you cast each turn
     * costs {2} less to cast".
     */
    @SerialName("NthOfTypePerTurn")
    @Serializable
    data class NthOfTypePerTurn(val n: Int) : CostGating

    /**
     * Modifier applies only while [condition] holds at cast time. The condition is evaluated with
     * the casting player as `controllerId`, so player-scoped conditions ("during your turn",
     * "if you've cast another spell this turn", "if your opponents control three or more creatures")
     * all work out of the box.
     *
     * Gates the *entire* modification, so it composes with any [CostModification] — including the
     * dynamic per-unit reductions ([CostModification.ReduceGenericBy]) that a fixed-amount source
     * cannot express. This is the home for "During your turn, ..." cost effects such as
     * Temur Battlecrier; for a fixed conditional reduction pair it with [CostModification.ReduceGeneric]
     * (e.g. Mental Modulation: `ReduceGeneric(1)` gated by `OnlyIf(IsYourTurn)`).
     */
    @SerialName("OnlyIf")
    @Serializable
    data class OnlyIf(val condition: Condition) : CostGating
}

/**
 * Sources for cost reduction amounts.
 */
@Serializable
sealed interface CostReductionSource {
    val description: String

    /**
     * Vivid - reduces cost by number of colors among permanents you control.
     */
    @SerialName("ColorsAmongPermanentsYouControl")
    @Serializable
    data object ColorsAmongPermanentsYouControl : CostReductionSource {
        override val description: String = "the number of colors among permanents you control"
    }

    /**
     * Reduces cost by a fixed amount.
     */
    @SerialName("Fixed")
    @Serializable
    data class Fixed(val amount: Int) : CostReductionSource {
        override val description: String = "$amount"
    }

    /**
     * Reduces cost by your speed, 0–4 (CR 702.179) — "Noncreature spells you cast cost {X} less to
     * cast, where X is your speed" (Samut, the Driving Force).
     *
     * "No speed" reads as 0 (CR 702.179f), so there is no has-speed distinction to make here: a
     * player who never started their engines simply gets no reduction. Speed is per-player and never
     * pooled in team games, so this always reads the *casting* player's speed.
     */
    @SerialName("YourSpeed")
    @Serializable
    data object YourSpeed : CostReductionSource {
        override val description: String = "your speed"
    }

    /**
     * Reduces cost by number of creatures you control.
     */
    @SerialName("CreaturesYouControl")
    @Serializable
    data object CreaturesYouControl : CostReductionSource {
        override val description: String = "the number of creatures you control"
    }

    /**
     * Reduces cost by total power of creatures you control.
     * Used for Ghalta, Primal Hunger.
     */
    @SerialName("TotalPowerYouControl")
    @Serializable
    data object TotalPowerYouControl : CostReductionSource {
        override val description: String = "the total power of creatures you control"
    }

    /**
     * Reduces cost by number of artifacts you control.
     * Used for Affinity for artifacts.
     */
    @SerialName("ArtifactsYouControl")
    @Serializable
    data object ArtifactsYouControl : CostReductionSource {
        override val description: String = "the number of artifacts you control"
    }

    /**
     * Reduces cost by a fixed amount if you control a permanent matching the filter.
     * Used for cards like Academy Journeymage ("This spell costs {1} less to cast if you control a Wizard").
     * Returns the fixed amount if any controlled permanent matches, otherwise 0.
     */
    @SerialName("FixedIfControlFilter")
    @Serializable
    data class FixedIfControlFilter(val amount: Int, val filter: GameObjectFilter) : CostReductionSource {
        override val description: String = "$amount if you control a permanent matching ${filter.description}"
    }

    /**
     * Reduces cost by 1 for each card in your graveyard matching the filter.
     * Used for Eddymurk Crab ("This spell costs {1} less to cast for each instant and sorcery card in your graveyard").
     *
     * @property filter The filter that graveyard cards must match to count toward the reduction
     * @property amountPerCard The amount of generic mana reduced per matching card (typically 1)
     */
    @SerialName("CardsInGraveyardMatchingFilter")
    @Serializable
    data class CardsInGraveyardMatchingFilter(
        val filter: GameObjectFilter,
        val amountPerCard: Int = 1
    ) : CostReductionSource {
        override val description: String = "the number of ${filter.description} cards in your graveyard"
    }

    /**
     * Reduces cost by 1 for each card you own in exile and in your graveyard matching the filter.
     * Used for Huskburster Swarm ("costs {1} less for each creature card you own in exile and in your graveyard").
     *
     * @property filter The filter that cards must match to count toward the reduction
     * @property amountPerCard The amount of generic mana reduced per matching card (typically 1)
     */
    @SerialName("CardsInGraveyardAndExileMatchingFilter")
    @Serializable
    data class CardsInGraveyardAndExileMatchingFilter(
        val filter: GameObjectFilter,
        val amountPerCard: Int = 1
    ) : CostReductionSource {
        override val description: String = "the number of ${filter.description} cards you own in exile and in your graveyard"
    }

    /**
     * Reduces cost by number of permanents you control matching a filter that have a specific counter.
     * Used for Eluge: "costs {U} less for each land you control with a flood counter on it."
     *
     * @property filter The filter permanents must match (e.g., Land)
     * @property counterType The counter type permanents must have (e.g., "flood")
     */
    @SerialName("PermanentsWithCounterYouControl")
    @Serializable
    data class PermanentsWithCounterYouControl(
        val filter: GameObjectFilter,
        val counterType: String
    ) : CostReductionSource {
        override val description: String = "${filter.description} you control with a $counterType counter on it"
    }

    /**
     * Reduces cost by a fixed amount if the spell targets any object matching the filter.
     * Used for cards like Dire Downdraft ("This spell costs {1} less to cast if it targets
     * an attacking or tapped creature").
     *
     * At cast resolution, the reduction applies if any of the spell's chosen targets match.
     * For affordability enumeration (before targets are chosen), the reduction is treated as
     * applicable if at least one legal target exists on the battlefield.
     */
    @SerialName("FixedIfAnyTargetMatches")
    @Serializable
    data class FixedIfAnyTargetMatches(
        val amount: Int,
        val filter: GameObjectFilter
    ) : CostReductionSource {
        override val description: String = "$amount if it targets ${filter.description}"
    }

    /**
     * Reduces cost by the greatest value of a numeric [property] among permanents the caster
     * controls matching a [filter] — "{X} less, where X is the greatest <property> among
     * <filter> you control". Empty matches yield 0 reduction.
     *
     * Parameterized over the property so one source shape covers the whole family instead of a
     * type-per-property:
     *  - `GreatestPropertyAmongPermanentsYouControl(EntityNumericProperty.Power, Filters.Creature)`
     *    — The Skullspore Nexus ("the greatest power among creatures you control").
     *  - `GreatestPropertyAmongPermanentsYouControl(EntityNumericProperty.ManaValue, <Elementals>)`
     *    — Sunderflock ("the greatest mana value among Elementals you control").
     *
     * Power/toughness are read from projected state (CR 613), so counters and continuous buffs
     * count; mana value is read from the card definition (X-cost permanents contribute X = 0).
     *
     * @property property Which numeric characteristic to take the greatest of
     * @property filter The filter that controlled permanents must match
     */
    @SerialName("GreatestPropertyAmongPermanentsYouControl")
    @Serializable
    data class GreatestPropertyAmongPermanentsYouControl(
        val property: EntityNumericProperty,
        val filter: GameObjectFilter
    ) : CostReductionSource {
        override val description: String = "the greatest ${property.description} among ${filter.description} you control"
    }

    /**
     * Reduces cost by the *sum* of a numeric [property] over the permanents the caster controls
     * matching a [filter] — "{X} less, where X is the total <property> of <filter> you control".
     * Empty matches yield 0 reduction.
     *
     * The sum twin of [GreatestPropertyAmongPermanentsYouControl], sharing its `(property, filter)`
     * axes and its read rules: power/toughness come from projected state (CR 613) so counters and
     * lords count, and mana value comes from the card definition (X-cost permanents contribute
     * X = 0). Negative power subtracts from the total, per Ghalta's ruling on the same "total
     * power" wording; only the finished sum is floored at 0.
     *
     * The filtered generalization of [TotalPowerYouControl], the same way
     * [PermanentsYouControlMatching] generalizes [CreaturesYouControl]:
     *  - `TotalPropertyAmongPermanentsYouControl(EntityNumericProperty.Power, Filters.Creature.withKeyword(FLYING))`
     *    — The Lord of the Eagles ("the total power of creatures you control with flying").
     *
     * @property property Which numeric characteristic to sum
     * @property filter The filter that controlled permanents must match
     */
    @SerialName("TotalPropertyAmongPermanentsYouControl")
    @Serializable
    data class TotalPropertyAmongPermanentsYouControl(
        val property: EntityNumericProperty,
        val filter: GameObjectFilter
    ) : CostReductionSource {
        override val description: String = "the total ${property.description} of ${filter.description} you control"
    }

    /**
     * Reduces cost by a numeric [property] of the permanent the *reducing ability's own source* is
     * attached to — "{X} less to cast, where X is equipped creature's power" (Glamdring,
     * Foe-hammer).
     *
     * The odd one out in this family: every other source aggregates over a group the caster
     * controls, while this one reads a single permanent found by walking the Equipment's/Aura's
     * attachment. It is therefore only meaningful on a [ModifySpellCost] printed on a permanent
     * that can be attached; an unattached (or non-attaching) source contributes 0, which is exactly
     * what an Equipment sitting on the battlefield with nothing equipped should do.
     *
     * Shares the read rules of [GreatestPropertyAmongPermanentsYouControl] and
     * [TotalPropertyAmongPermanentsYouControl]: power/toughness come from projected state (CR 613),
     * so counters, anthems, and the Equipment's own bonus all count; mana value comes from the card
     * definition. Negative power floors at 0 — a cost can't be *increased* by a reduction clause.
     *
     * @property property Which numeric characteristic of the attached permanent to read
     */
    @SerialName("AttachedPermanentProperty")
    @Serializable
    data class AttachedPermanentProperty(
        val property: EntityNumericProperty
    ) : CostReductionSource {
        override val description: String = "the attached permanent's ${property.description}"
    }

    /**
     * Reduces cost by a fixed amount if a creature is currently attacking the caster.
     * Used for cards like Swat Away ("This spell costs {2} less to cast if a creature
     * is attacking you").
     *
     * Evaluated at cast time against the live combat state — any creature on the
     * battlefield whose attack is declared against the casting player (directly or
     * against one of their planeswalkers) satisfies the condition. During the
     * declare-attackers step and combat damage step, this exposes the reduction
     * defensively; outside combat, the reduction does not apply.
     */
    @SerialName("FixedIfCreatureAttackingYou")
    @Serializable
    data class FixedIfCreatureAttackingYou(val amount: Int) : CostReductionSource {
        override val description: String = "$amount if a creature is attacking you"
    }

    /**
     * Reduces cost by a fixed amount if a creature died this turn under *any* player's control —
     * the morbid family's cost-reduction shape ("This spell costs {3} less to cast if a creature
     * died this turn", Dreaded Bat-Cloud).
     *
     * Reads the same per-player died-this-turn tally as
     * [com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition], summed across the
     * table, so an opponent's creature dying counts. Turn history, not a graveyard scan: a creature
     * that died and was then exiled or reanimated still counts.
     */
    @SerialName("FixedIfCreatureDiedThisTurn")
    @Serializable
    data class FixedIfCreatureDiedThisTurn(val amount: Int) : CostReductionSource {
        override val description: String = "$amount if a creature died this turn"
    }

    /**
     * Reduces cost by a fixed amount if the Void condition is met this turn — i.e.,
     * a nonland permanent left the battlefield this turn or a spell was warped this turn.
     * Used for Edge of Eternities cards like Temporal Intervention
     * ("This spell costs {2} less to cast if a nonland permanent left the battlefield
     * this turn or a spell was warped this turn").
     */
    @SerialName("FixedIfVoid")
    @Serializable
    data class FixedIfVoid(val amount: Int) : CostReductionSource {
        override val description: String =
            "$amount if a nonland permanent left the battlefield this turn or a spell was warped this turn"
    }

    /**
     * Reduces cost by the number of permanents the caster controls matching a filter.
     * The "you control" analogue of [PermanentsOnBattlefieldMatching]; the filter carries the
     * narrowing (type, power, subtype, ...). Power/type checks honor projected state, so buffs
     * and lords count (e.g. Temur Battlecrier: "for each creature you control with power 4 or
     * greater" via `PermanentsYouControlMatching(Creature.powerAtLeast(4))`).
     *
     * Generalizes the fixed [CreaturesYouControl] / [ArtifactsYouControl] shorthands to an
     * arbitrary filter.
     */
    @SerialName("PermanentsYouControlMatching")
    @Serializable
    data class PermanentsYouControlMatching(
        val filter: GameObjectFilter
    ) : CostReductionSource {
        override val description: String =
            "the number of ${filter.description} you control"
    }

    /**
     * Reduces cost by the number of differently named permanents the caster controls
     * matching a filter. Used for Fungal Colossus ("This spell costs {X} less to cast,
     * where X is the number of differently named lands you control") via
     * `DifferentlyNamedPermanentsYouControl(Filters.Land)`.
     */
    @SerialName("DifferentlyNamedPermanentsYouControl")
    @Serializable
    data class DifferentlyNamedPermanentsYouControl(
        val filter: GameObjectFilter
    ) : CostReductionSource {
        override val description: String =
            "the number of differently named ${filter.description} you control"
    }

    /**
     * Reduces cost by the number of permanents on the battlefield matching a filter,
     * regardless of who controls them. Used for cards like Blasphemous Act
     * ("This spell costs {1} less to cast for each creature on the battlefield") via
     * `PermanentsOnBattlefieldMatching(Filters.Creature)`.
     */
    @SerialName("PermanentsOnBattlefieldMatching")
    @Serializable
    data class PermanentsOnBattlefieldMatching(
        val filter: GameObjectFilter
    ) : CostReductionSource {
        override val description: String =
            "the number of ${filter.description} on the battlefield"
    }

    /**
     * Reduces cost by [amountPerPermanent] for each permanent sacrificed this turn —
     * by ANY player, not just the caster (the wording is "for each permanent sacrificed
     * this turn", which is not controller-scoped). Reads the turn-scoped
     * `GameState.permanentsSacrificedThisTurn` counter, which the central sacrifice hook
     * (`ZoneTransitionService.trackPermanentSacrifice`) increments on every sacrifice and
     * `TurnManager.startTurn` resets to 0 at each new turn.
     *
     * Used for The Balrog, Durin's Bane ("This spell costs {1} less to cast for each
     * permanent sacrificed this turn") via `PermanentsSacrificedThisTurn()`.
     */
    @SerialName("PermanentsSacrificedThisTurn")
    @Serializable
    data class PermanentsSacrificedThisTurn(
        val amountPerPermanent: Int = 1
    ) : CostReductionSource {
        override val description: String = "the number of permanents sacrificed this turn"
    }

    /**
     * Reduces cost by [amountPerCreature] for each creature that was declared as an attacker this
     * turn — by ANY player, not just the caster ("for each creature that attacked this turn" is
     * not controller-scoped), and counting every combat phase this turn.
     *
     * The turn-history sibling of [PermanentsSacrificedThisTurn], and deliberately *not*
     * `PermanentsOnBattlefieldMatching(Creature.attackedThisTurn())`: that reads the live
     * battlefield, so an attacker that died in combat would stop counting. The count here is
     * the union of every player's `PlayerAttackersThisTurnComponent.attackerIds` — the same set
     * the engine already maintains for raid — which survives the attacker leaving the
     * battlefield, so a trick cast after combat damage still sees the creatures that traded.
     *
     * Used for Witchstalker Frenzy ("This spell costs {1} less to cast for each creature that
     * attacked this turn"). Per its ruling the reduction never touches the colored part of the
     * cost, so it can't reduce below `{R}` — that clamping is the generic-reduction rail's, not
     * this source's.
     */
    @SerialName("CreaturesThatAttackedThisTurn")
    @Serializable
    data class CreaturesThatAttackedThisTurn(
        val amountPerCreature: Int = 1
    ) : CostReductionSource {
        override val description: String = "the number of creatures that attacked this turn"
    }

    /**
     * Reduces cost by [amountPerType] for each *card type* among the cards in the caster's
     * graveyard — Emrakul, the Promised End ("This spell costs {1} less to cast for each card type
     * among cards in your graveyard").
     *
     * Counts distinct card types (CR 205.2a: artifact, battle, creature, enchantment, instant,
     * kindred, land, planeswalker, sorcery), never supertypes or subtypes. A single card with
     * several types (an artifact creature) contributes each of its types once, and the same type
     * across many cards still counts once — so the cap is the number of card types in the game,
     * not the graveyard's size.
     *
     * The counting sibling of [CardsInGraveyardMatchingFilter], which totals *cards*: nine
     * creature cards reduce by nine there and by one here. Uses the same aggregation as
     * `Conditions.Delirium` (`Aggregation.DISTINCT_TYPES` over the graveyard), so a card that
     * satisfies delirium sees a matching reduction.
     */
    @SerialName("CardTypesInYourGraveyard")
    @Serializable
    data class CardTypesInYourGraveyard(
        val amountPerType: Int = 1
    ) : CostReductionSource {
        override val description: String = "the number of card types among cards in your graveyard"
    }
}

/**
 * Static ability that modifies the cost of the **Plot** special action (CR 718).
 *
 * Plot is not a spell, so [ModifySpellCost] does not touch it; this is its dedicated cost
 * modifier. The engine's `PlotCostReducer` scans the battlefield for these and reduces the
 * plot cost paid by [PlotEnumerator]/`PlotCardHandler`.
 *
 * Used for Doc Aurlock, Grizzled Genius ("Plotting cards from your hand costs {2} less") via
 * `ModifyPlotCost(PlotCostTarget.YouPlotFromHand, CostModification.ReduceGeneric(2))`.
 *
 * @property target Which plots the modifier applies to.
 * @property modification How the cost is changed (reuses the spell-cost [CostModification]
 *           vocabulary; only generic reductions are currently meaningful for plot).
 */
@SerialName("ModifyPlotCost")
@Serializable
data class ModifyPlotCost(
    val target: PlotCostTarget,
    val modification: CostModification,
) : StaticAbility {
    override val description: String = buildString {
        append(
            when (target) {
                PlotCostTarget.YouPlotFromHand -> "Plotting cards from your hand"
            }
        )
        append(
            when (val m = modification) {
                is CostModification.ReduceGeneric -> " costs {${m.amount}} less"
                is CostModification.IncreaseGeneric -> " costs {${m.amount}} more"
                else -> " has a modified cost"
            }
        )
    }

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newModification = modification.applyTextReplacement(replacer)
        return if (newModification !== modification) copy(modification = newModification) else this
    }
}

/**
 * What a [ModifyPlotCost] applies to. Modeled as a sealed interface so future "plot from the top
 * of your library" reductions (Fblthp-shaped) slot in as a new variant without changing the
 * static-ability shape.
 */
@Serializable
sealed interface PlotCostTarget {
    /** Cards the source's controller plots from their hand (the printed Plot keyword cost). */
    @SerialName("YouPlotFromHand")
    @Serializable
    data object YouPlotFromHand : PlotCostTarget
}

/**
 * Static ability that modifies the cost of the **door-unlock** special action (CR 709.5e).
 *
 * Unlocking a door is not a spell or the Plot action, so neither [ModifySpellCost] nor
 * [ModifyPlotCost] touches it; this is its dedicated cost modifier. The engine's
 * `UnlockCostReducer` scans the battlefield for these and reduces the unlock cost paid by
 * `UnlockRoomDoorEnumerator` (affordability) and `UnlockRoomDoorHandler` (validate + pay),
 * keeping the two in lockstep — mirroring the [ModifyPlotCost]/`PlotCostReducer` pair.
 *
 * Used for Inquisitive Glimmer ("Unlock costs you pay cost {1} less") via
 * `ModifyUnlockCost(UnlockCostTarget.YouUnlock, CostModification.ReduceGeneric(1))`. Only
 * generic [CostModification] reductions/increases are meaningful — the printed unlock cost is
 * a flat mana cost.
 *
 * @property target Whose unlock actions the modifier applies to.
 * @property modification How the cost is changed (reuses the spell-cost [CostModification]
 *           vocabulary; only generic adjustments apply to unlocking).
 */
@SerialName("ModifyUnlockCost")
@Serializable
data class ModifyUnlockCost(
    val target: UnlockCostTarget,
    val modification: CostModification,
) : StaticAbility {
    override val description: String = buildString {
        append(
            when (target) {
                UnlockCostTarget.YouUnlock -> "Unlock costs you pay"
            }
        )
        append(
            when (val m = modification) {
                is CostModification.ReduceGeneric -> " cost {${m.amount}} less"
                is CostModification.IncreaseGeneric -> " cost {${m.amount}} more"
                else -> " have a modified cost"
            }
        )
    }

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newModification = modification.applyTextReplacement(replacer)
        return if (newModification !== modification) copy(modification = newModification) else this
    }
}

/**
 * Whose door-unlock actions a [ModifyUnlockCost] applies to. Modeled as a sealed interface so a
 * future "opponents' unlock costs" tax slots in as a new variant without changing the
 * static-ability shape.
 */
@Serializable
sealed interface UnlockCostTarget {
    /** Door-unlock special actions performed by the source's controller ("unlock costs you pay"). */
    @SerialName("YouUnlock")
    @Serializable
    data object YouUnlock : UnlockCostTarget
}

/**
 * The activated abilities of permanents matching [filter] cost [amount] generic mana less to
 * activate, with the mana in each cost floored at [manaFloor] total mana (generic + colored).
 *
 * The activated-ability sibling of [ModifySpellCost] / [ModifyPlotCost] / [ReduceEquipCost],
 * generalized so one type covers the family:
 *  - **Power Artifact** ("Enchanted artifact's activated abilities cost {2} less to activate. This
 *    effect can't reduce the mana in that cost to less than one mana.") →
 *    `ReduceActivatedAbilityCost(GroupFilter.attachedCreature(), amount = DynamicAmount.Fixed(2), manaFloor = 1)`. The
 *    `attachedCreature()` scope resolves to the enchanted permanent, so the reduction keys to the
 *    artifact this Aura is attached to.
 *  - A "your creatures' activated abilities cost {X} less, where X is this creature's power" lord →
 *    `ReduceActivatedAbilityCost(GroupFilter(GameObjectFilter.Creature.youControl()),
 *    amount = DynamicAmount.EntityProperty(EntityReference.Source, EntityNumericProperty.Power))`.
 *
 * Only the **generic** portion of the ability's mana cost is reduced; colored/hybrid/Phyrexian pips
 * are untouched (CR 118.7). [manaFloor] is the minimum *total* mana the cost may be reduced to: with
 * `manaFloor = 1`, a `{1}` ability stays `{1}` and a `{2}` ability becomes `{1}` (not `{0}`), while
 * a `{2}{U}` ability becomes `{U}` (already 1 mana). `manaFloor = 0` (default) floors at `{0}` like
 * an ordinary reduction. Non-mana costs (`{T}`, sacrifice) and abilities with no mana cost are
 * unaffected. Multiple sources stack additively before the floor is applied.
 *
 * [exhaustOnly] narrows the reduction to abilities marked `isExhaust` (CR 702.177) — Boom Scholar's
 * "Exhaust abilities of other permanents you control cost {2} less to activate." It gates on the
 * *ability*, not the permanent, so a matching permanent's ordinary activated abilities cost full
 * price while its exhaust ability is discounted. [powerUpOnly] is its sibling for power-up
 * (CR 702.193) — Hulk, Gamma Goliath's "Power-up abilities of other creatures you control cost {3}
 * less to activate."
 *
 * @property filter Which permanents' activated abilities are cheaper (matched via projected state;
 *   use [GroupFilter.attachedCreature] for an Aura's enchanted permanent, [GroupFilter.source] for
 *   "this permanent's abilities", or a battlefield filter for a group).
 * @property amount Dynamic generic-mana reduction applied to each matching ability's cost.
 * @property manaFloor Minimum total mana the cost may be reduced to (default 0).
 * @property exhaustOnly When true, only exhaust abilities (`ActivatedAbility.isExhaust`) are reduced.
 * @property powerUpOnly When true, only power-up abilities (`ActivatedAbility.isPowerUp`) are reduced.
 */
@SerialName("ReduceActivatedAbilityCost")
@Serializable
data class ReduceActivatedAbilityCost(
    val filter: GroupFilter,
    val amount: DynamicAmount,
    val manaFloor: Int = 0,
    val exhaustOnly: Boolean = false,
    val powerUpOnly: Boolean = false
) : StaticAbility {
    override val description: String = buildString {
        val abilities = when {
            exhaustOnly -> "exhaust abilities"
            powerUpOnly -> "power-up abilities"
            else -> "activated abilities"
        }
        append(filter.description.replaceFirstChar { it.uppercase() })
        when (amount) {
            is DynamicAmount.Fixed -> append("'s $abilities cost {${amount.amount}} less to activate")
            else -> append("'s $abilities cost {X} less to activate, where X is ${amount.description}")
        }
        if (manaFloor > 0) {
            append(". This effect can't reduce the mana in that cost to less than ")
            append(if (manaFloor == 1) "one mana" else "$manaFloor mana")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newFilter !== filter || newAmount !== amount) copy(filter = newFilter, amount = newAmount) else this
    }
}

/**
 * The activated abilities of sources matching [filter] cost [amount] generic mana **more** to
 * activate — the taxing mirror of [ReduceActivatedAbilityCost], summed against it into a single
 * net delta so a reduction and an increase on the same ability cancel (CR 601.2f: increases and
 * reductions are applied to the mana part of the cost, and the two are netted before payment).
 *
 * Unlike a reduction, an increase applies even when the ability has *no* mana in its cost — a bare
 * `{T}:` ability taxed by {2} becomes `{2}, {T}:`. There is no floor parameter: costs only grow.
 *
 * Used by Skyseer's Chariot — "Activated abilities of sources with the chosen name cost {2} more to
 * activate", where [filter] is the bare chosen-name predicate
 * ([com.wingedsheep.sdk.scripting.GameObjectFilter.namedFromChosenComponent]) so the tax keys off
 * the Vehicle's durable as-enters card-name choice.
 *
 * @property filter Which sources' activated abilities are taxed (matched via projected state; use
 *   [com.wingedsheep.sdk.scripting.filters.unified.GroupFilter.source] for "this permanent's
 *   abilities" or a battlefield filter for a group).
 * @property amount Dynamic generic-mana increase applied to each matching ability's cost.
 */
@SerialName("IncreaseActivatedAbilityCost")
@Serializable
data class IncreaseActivatedAbilityCost(
    val filter: GroupFilter,
    val amount: DynamicAmount
) : StaticAbility {
    override val description: String = buildString {
        append(filter.description.replaceFirstChar { it.uppercase() })
        when (amount) {
            is DynamicAmount.Fixed -> append("'s activated abilities cost {${amount.amount}} more to activate")
            else -> append("'s activated abilities cost {X} more to activate, where X is ${amount.description}")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newFilter !== filter || newAmount !== amount) copy(filter = newFilter, amount = newAmount) else this
    }
}
