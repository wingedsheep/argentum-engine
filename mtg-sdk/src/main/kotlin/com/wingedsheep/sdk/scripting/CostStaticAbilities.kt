package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
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
        val gated = gating != CostGating.None
        val gate = when (gating) {
            is CostGating.NthOfTypePerTurn -> "The ${ordinal(gating.n)} "
            CostGating.None -> ""
        }
        // A gated description ("The Nth ...") refers to a single spell, so phrase it in the singular.
        val noun = if (gated) "spell" else "spells"
        val subject = when (target) {
            SpellCostTarget.SelfCast -> "This spell"
            is SpellCostTarget.YouCast -> "${filterAdjective(target.filter)}$noun you cast"
            is SpellCostTarget.AnyCaster -> "${filterAdjective(target.filter)}$noun"
            is SpellCostTarget.OpponentsCastTargeting ->
                "Spells your opponents cast that target ${target.targetFilter.description}"
            SpellCostTarget.FaceDownYouCast -> "Face-down creature spells you cast"
            SpellCostTarget.MorphActivation -> "All morph costs"
        }
        val verb = when (modification) {
            is CostModification.ReduceGeneric -> "cost {${modification.amount}} less to cast"
            is CostModification.ReduceGenericBy -> "cost {X} less to cast, where X is ${modification.source.description}"
            is CostModification.ReduceColored -> "cost ${modification.symbols} less to cast"
            is CostModification.ReduceColoredPerUnit ->
                "cost ${modification.symbols} less to cast for each ${modification.countSource.description}"
            is CostModification.IncreaseGeneric -> "cost {${modification.amount}} more"
            is CostModification.IncreaseColored -> "cost ${modification.symbols} more to cast"
            is CostModification.IncreaseGenericPerOtherSpellThisTurn ->
                "cost {${modification.amountPerSpell}} more to cast for each other spell that player has cast this turn"
            is CostModification.IncreaseLife -> "cost an additional ${modification.amount} life to cast"
        }
        val perTurn = when (gating) {
            is CostGating.NthOfTypePerTurn -> " each turn"
            CostGating.None -> ""
        }
        // The gated subject is singular, so make the verb agree ("cost" -> "costs").
        val agreedVerb = if (gated) verb.replaceFirst("cost ", "costs ") else verb
        val prefix = if (gate.isNotEmpty()) gate + subject.replaceFirstChar { it.lowercase() } else subject
        return "$prefix $agreedVerb$perTurn"
    }

    // A filter that narrows nothing (e.g. GameObjectFilter.Any) describes itself as "card", which
    // reads wrong as an adjective ("the second card spell you cast"). Emit no adjective in that case
    // so the unconstrained form is just "spell(s)".
    private fun filterAdjective(filter: GameObjectFilter): String {
        val desc = filter.description
        return if (desc.isBlank() || desc == "card") "" else "$desc "
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
    fun applyTextReplacement(replacer: TextReplacer): SpellCostTarget

    /** Self-reduction on the spell card itself — applies when this card is cast. */
    @SerialName("SelfCast")
    @Serializable
    data object SelfCast : SpellCostTarget {
        override fun applyTextReplacement(replacer: TextReplacer): SpellCostTarget = this
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

    /** Face-down (morph) creature spells the source's controller casts. */
    @SerialName("FaceDownYouCast")
    @Serializable
    data object FaceDownYouCast : SpellCostTarget {
        override fun applyTextReplacement(replacer: TextReplacer): SpellCostTarget = this
    }

    /** The morph (turn face-up) activated cost, applied globally. */
    @SerialName("MorphActivation")
    @Serializable
    data object MorphActivation : SpellCostTarget {
        override fun applyTextReplacement(replacer: TextReplacer): SpellCostTarget = this
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
     * Reduces cost by the greatest mana value among permanents the caster controls
     * matching a filter. Used for Sunderflock ("This spell costs {X} less to cast,
     * where X is the greatest mana value among Elementals you control.").
     *
     * Empty matches yield 0 reduction. Mana value is read from the card definition
     * (X-cost permanents contribute X = 0).
     *
     * @property filter The filter that controlled permanents must match
     */
    @SerialName("GreatestManaValueAmongPermanentsYouControl")
    @Serializable
    data class GreatestManaValueAmongPermanentsYouControl(
        val filter: GameObjectFilter
    ) : CostReductionSource {
        override val description: String = "the greatest mana value among ${filter.description} you control"
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
     * Reduces cost by a fixed amount if the given [Condition] evaluates true at cast time.
     * The condition is evaluated with the caster as `controllerId`, so "your turn",
     * "you have the city's blessing", etc. all work out of the box.
     *
     * Used for cards like Mental Modulation
     * (`FixedIfCondition(1, IsYourTurn)` — "costs {1} less to cast during your turn").
     */
    @SerialName("FixedIfCondition")
    @Serializable
    data class FixedIfCondition(val amount: Int, val condition: Condition) : CostReductionSource {
        override val description: String = "$amount ${condition.description}"
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
}
