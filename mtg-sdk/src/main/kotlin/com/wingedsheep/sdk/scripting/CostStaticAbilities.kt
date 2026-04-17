package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Reduces the cost of spells with a matching subtype cast by the controller of this permanent.
 * Used for "Goblin spells you cast cost {1} less" (Goblin Warchief),
 * "Zombie spells you cast cost {1} less" (Undead Warchief),
 * "Dragon spells you cast cost {2} less" (Dragonspeaker Shaman), etc.
 *
 * This is a battlefield-based static ability — the permanent with this ability
 * must be on the battlefield to provide the reduction.
 *
 * @property subtype The creature subtype that spells must have to benefit from the reduction
 * @property amount The amount of generic mana to reduce
 */
@SerialName("ReduceSpellCostBySubtype")
@Serializable
data class ReduceSpellCostBySubtype(
    val subtype: String,
    val amount: Int
) : StaticAbility {
    override val description: String = "$subtype spells you cast cost {$amount} less to cast"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newSubtype = replacer.replaceCreatureType(subtype)
        return if (newSubtype != subtype) copy(subtype = newSubtype) else this
    }
}

/**
 * Reduces the colored mana cost of spells with a matching subtype cast by the controller of this permanent.
 * Unlike ReduceSpellCostBySubtype (which reduces generic mana), this removes specific colored mana symbols.
 * Used for "Cleric spells you cast cost {W}{B} less to cast" (Edgewalker).
 *
 * The manaReduction string specifies which colored symbols to remove (e.g., "{W}{B}").
 * This effect reduces only colored mana, never generic mana.
 *
 * @property subtype The creature subtype that spells must have to benefit from the reduction
 * @property manaReduction The colored mana symbols to remove, as a mana cost string (e.g., "{W}{B}")
 */
@SerialName("ReduceSpellColoredCostBySubtype")
@Serializable
data class ReduceSpellColoredCostBySubtype(
    val subtype: String,
    val manaReduction: String
) : StaticAbility {
    override val description: String = "$subtype spells you cast cost $manaReduction less to cast"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newSubtype = replacer.replaceCreatureType(subtype)
        return if (newSubtype != subtype) copy(subtype = newSubtype) else this
    }
}

/**
 * Reduces the cost of spells matching a filter cast by the controller of this permanent.
 * A general-purpose cost reduction that uses GameObjectFilter's card predicates to match spells.
 *
 * Examples:
 * - "Creature spells with MV 6+ cost {2} less" → ReduceSpellCostByFilter(Creature.manaValueAtLeast(6), 2)
 * - "Red spells cost {1} less" → ReduceSpellCostByFilter(Any.withColor(Color.RED), 1)
 * - "Dragon spells cost {2} less" → ReduceSpellCostByFilter(Any.withSubtype("Dragon"), 2)
 *
 * This is a battlefield-based static ability — the permanent with this ability
 * must be on the battlefield to provide the reduction.
 *
 * @property filter The filter that spells must match to benefit from the reduction (card predicates only)
 * @property amount The amount of generic mana to reduce
 */
@SerialName("ReduceSpellCostByFilter")
@Serializable
data class ReduceSpellCostByFilter(
    val filter: GameObjectFilter,
    val amount: Int
) : StaticAbility {
    override val description: String = "${filter.description} spells you cast cost {$amount} less to cast"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Reduces the cost to cast this spell.
 * Used for Vivid and similar cost-reduction mechanics.
 *
 * Note: This is a static ability that affects casting cost.
 * Full implementation requires cost calculation during spell casting.
 *
 * @property reductionSource How the reduction amount is determined
 */
@SerialName("SpellCostReduction")
@Serializable
data class SpellCostReduction(
    val reductionSource: CostReductionSource
) : StaticAbility {
    override val description: String = "This spell costs {X} less to cast, where X is ${reductionSource.description}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Reduces the cost to cast face-down creature spells (morph).
 * Used for Dream Chisel: "Face-down creature spells you cast cost {1} less to cast."
 *
 * This is a static ability on a permanent that reduces the morph casting cost
 * for its controller. The engine scans battlefield permanents for this ability
 * when calculating face-down spell costs.
 *
 * @property reductionSource How the reduction amount is determined
 */
@SerialName("FaceDownSpellCostReduction")
@Serializable
data class FaceDownSpellCostReduction(
    val reductionSource: CostReductionSource
) : StaticAbility {
    override val description: String = "Face-down creature spells you cast cost {${reductionSource.description}} less to cast"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
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
}

/**
 * Reduces the colored mana cost of the first spell matching a filter you cast each turn,
 * by a per-unit mana symbol multiplied by a dynamic count.
 * Used for Eluge: "The first instant or sorcery spell you cast each turn costs {U} less
 * for each land you control with a flood counter on it."
 *
 * This is a battlefield-based static ability. The engine checks the player's spell records
 * to determine "first of type" eligibility, then evaluates the count source
 * and applies colored mana reduction.
 *
 * @property spellFilter The filter that spells must match (card predicates only)
 * @property manaReductionPerUnit The colored mana symbol to remove per unit (e.g., "{U}")
 * @property countSource How the number of reduction units is determined
 */
@SerialName("ReduceFirstSpellOfTypeColoredCost")
@Serializable
data class ReduceFirstSpellOfTypeColoredCost(
    val spellFilter: GameObjectFilter,
    val manaReductionPerUnit: String,
    val countSource: CostReductionSource
) : StaticAbility {
    override val description: String = "The first ${spellFilter.description} spell you cast each turn costs $manaReductionPerUnit less to cast for each ${countSource.description}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = spellFilter.applyTextReplacement(replacer)
        return if (newFilter !== spellFilter) copy(spellFilter = newFilter) else this
    }
}

/**
 * Reduces the cost of face-down creature spells you cast.
 * Used for Dream Chisel: "Face-down creature spells you cast cost {1} less to cast."
 *
 * This is a battlefield-based static ability — the permanent with this ability
 * must be on the battlefield to provide the reduction.
 *
 * @property amount The amount of generic mana to reduce
 */
@SerialName("ReduceFaceDownCastingCost")
@Serializable
data class ReduceFaceDownCastingCost(
    val amount: Int
) : StaticAbility {
    override val description: String = "Face-down creature spells you cast cost {$amount} less to cast"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * All morph costs cost more to pay (turning face-down creatures face up).
 * Used for Exiled Doomsayer: "All morph costs cost {2} more."
 *
 * This affects all players' morph (turn face-up) costs globally.
 * The engine scans all battlefield permanents for this ability when calculating
 * the effective cost to turn a face-down creature face up.
 * Does not affect the cost to cast creature spells face down.
 *
 * @property amount The amount of additional generic mana required
 */
@SerialName("IncreaseMorphCost")
@Serializable
data class IncreaseMorphCost(
    val amount: Int
) : StaticAbility {
    override val description: String = "All morph costs cost {$amount} more"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Increases the cost of spells matching a filter for ALL players.
 * Used for tax effects like Glowrider: "Noncreature spells cost {1} more to cast."
 *
 * This is a global effect — it applies to all players, not just the controller.
 * The engine scans all battlefield permanents for this ability when calculating
 * effective spell costs.
 *
 * @property filter The filter that spells must match to be taxed (card predicates only)
 * @property amount The amount of generic mana to increase
 */
@SerialName("IncreaseSpellCostByFilter")
@Serializable
data class IncreaseSpellCostByFilter(
    val filter: GameObjectFilter,
    val amount: Int
) : StaticAbility {
    override val description: String = "${filter.description} spells cost {$amount} more to cast"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Increases the cost of each spell a player casts by {1} for each other spell
 * that player has already cast this turn.
 * Used for Damping Sphere: "Each spell a player casts costs {1} more to cast
 * for each other spell that player has cast this turn."
 *
 * This is a global effect — it applies to all players.
 * The engine uses the per-player spell count from GameState to determine the increase.
 *
 * @property amountPerSpell The amount of generic mana added per previously-cast spell (typically 1)
 */
@SerialName("IncreaseSpellCostByPlayerSpellsCast")
@Serializable
data class IncreaseSpellCostByPlayerSpellsCast(
    val amountPerSpell: Int = 1
) : StaticAbility {
    override val description: String = "Each spell a player casts costs {$amountPerSpell} more to cast for each other spell that player has cast this turn"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}
