package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An activated ability is an ability that a player can activate by paying a cost.
 * Format: "[Cost]: [com.wingedsheep.sdk.scripting.effects.Effect]"
 */
@Serializable
data class ActivatedAbility(
    val id: AbilityId = AbilityId.generate(),
    val cost: AbilityCost,
    val effect: Effect,
    val targetRequirements: List<TargetRequirement> = emptyList(),
    val timing: TimingRule = TimingRule.InstantSpeed,
    val restrictions: List<ActivationRestriction> = emptyList(),
    val isManaAbility: Boolean = false,
    val isPlaneswalkerAbility: Boolean = false,
    val activateFromZone: Zone = Zone.BATTLEFIELD,
    val promptOnDraw: Boolean = false,
    val descriptionOverride: String? = null,
    val hasConvoke: Boolean = false,
    /** When true, prevents auto-pass whenever this ability is available.
     *  Used for abilities that interact with transient game state the player would miss,
     *  such as copying a spell on the stack. */
    val holdPriority: Boolean = false,
    /** When non-null, reduces the generic-mana portion of [cost] at activation time by the
     *  amount returned for the source. Used by cards like The Dominion Bracelet whose
     *  granted activated ability "costs {X} less to activate, where X is this creature's power."
     *  Per Scryfall ruling, the reduced cost is locked in before costs are paid. */
    val genericCostReduction: DynamicAmount? = null,
    /**
     * Colors that may be spent on the `{X}` portion of this ability's cost.
     * Empty means no restriction (the default). Used for abilities like Atalya, Samite
     * Master ("Spend only white mana on X"). Honored by the mana solver and the
     * activated-ability payment path.
     */
    val xManaRestriction: Set<Color> = emptySet()
) : TextReplaceable<ActivatedAbility> {
    /** Backward-compatible secondary constructor for single-target abilities. */
    constructor(
        id: AbilityId,
        cost: AbilityCost,
        effect: Effect,
        targetRequirement: TargetRequirement?,
        timing: TimingRule = TimingRule.InstantSpeed,
        restrictions: List<ActivationRestriction> = emptyList(),
        isManaAbility: Boolean = false,
        isPlaneswalkerAbility: Boolean = false,
        activateFromZone: Zone = Zone.BATTLEFIELD,
        promptOnDraw: Boolean = false,
        descriptionOverride: String? = null
    ) : this(
        id = id,
        cost = cost,
        effect = effect,
        targetRequirements = listOfNotNull(targetRequirement),
        timing = timing,
        restrictions = restrictions,
        isManaAbility = isManaAbility,
        isPlaneswalkerAbility = isPlaneswalkerAbility,
        activateFromZone = activateFromZone,
        promptOnDraw = promptOnDraw,
        descriptionOverride = descriptionOverride
    )

    /** Convenience accessor for single-target abilities. */
    val targetRequirement: TargetRequirement?
        get() = targetRequirements.firstOrNull()

    val description: String
        get() = descriptionOverride ?: "${cost.description}: ${effect.description}"

    override fun applyTextReplacement(replacer: TextReplacer): ActivatedAbility {
        val newCost = cost.applyTextReplacement(replacer)
        val newEffect = effect.applyTextReplacement(replacer)
        var trChanged = false
        val newTargetReqs = targetRequirements.map {
            val n = it.applyTextReplacement(replacer)
            if (n !== it) trChanged = true
            n
        }
        return if (newCost !== cost || newEffect !== effect || trChanged)
            copy(cost = newCost, effect = newEffect, targetRequirements = newTargetReqs) else this
    }
}

/**
 * Costs for activated abilities.
 */
@Serializable
sealed interface AbilityCost : TextReplaceable<AbilityCost> {
    val description: String

    /** No cost ({0}) — the ability is free to activate */
    @SerialName("CostFree")
    @Serializable
    data object Free : AbilityCost {
        override val description: String = "{0}"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /** Tap the permanent ({T}) */
    @SerialName("CostTap")
    @Serializable
    data object Tap : AbilityCost {
        override val description: String = "{T}"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /** Untap the permanent ({Q}) */
    @SerialName("CostUntap")
    @Serializable
    data object Untap : AbilityCost {
        override val description: String = "{Q}"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /** Pay mana */
    @SerialName("CostMana")
    @Serializable
    data class Mana(val cost: ManaCost) : AbilityCost {
        override val description: String = cost.toString()
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /** Pay life */
    @SerialName("CostPayLife")
    @Serializable
    data class PayLife(val amount: Int) : AbilityCost {
        override val description: String = "Pay $amount life"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /**
     * Pay X life, where X is the value chosen for the ability's `{X}` mana cost.
     *
     * Mirrors [ExileXFromGraveyard]: an X-linked variable cost that reads the chosen
     * X (`CostPaymentChoices.xValue`) at payment time. Used by abilities like
     * "{X}{B}, {T}, Pay X life: ..." (Krumar Initiate) where the life payment must
     * equal the X paid for the mana cost. The legal-action enumerator caps the
     * affordable X by the controller's life total (you can't pay more life than you
     * have, and must survive at 1+).
     */
    @SerialName("CostPayXLife")
    @Serializable
    data object PayXLife : AbilityCost {
        override val description: String = "Pay X life"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /**
     * Sacrifice a permanent
     *
     * @property filter Which permanents can be sacrificed
     */
    @SerialName("CostSacrifice")
    @Serializable
    data class Sacrifice(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val excludeSelf: Boolean = false,
        val count: Int = 1
    ) : AbilityCost {
        override val description: String = buildString {
            append("Sacrifice ")
            if (count == 1) {
                append(if (excludeSelf) "another " else "a ")
            } else {
                append("$count ")
            }
            append(filter.description)
            if (count > 1) append("s")
        }

        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Discard one or more cards.
     *
     * @property filter Which cards can be discarded
     * @property count How many cards to discard
     * @property atRandom When true, the engine chooses the discarded cards at random
     *   (no player selection); otherwise the player picks which cards to discard.
     */
    @SerialName("CostDiscard")
    @Serializable
    data class Discard(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val count: Int = 1,
        val atRandom: Boolean = false
    ) : AbilityCost {
        override val description: String = buildString {
            append("Discard ")
            if (count == 1) append("a ") else append("$count ")
            append(filter.description)
            if (count != 1) append("s")
            if (atRandom) append(" at random")
        }

        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Exile cards from graveyard
     *
     * @property count Number of cards to exile
     * @property filter Which cards can be exiled
     */
    @SerialName("CostExileFromGraveyard")
    @Serializable
    data class ExileFromGraveyard(
        val count: Int,
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : AbilityCost {
        override val description: String = buildString {
            append("Exile $count ${filter.description}")
            if (count > 1) append("s")
            append(" from your graveyard")
        }

        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Exile X cards from graveyard, where X is the ability's X value.
     * Used for abilities like Necropolis Fiend: "{X}, {T}, Exile X cards from your graveyard"
     *
     * @property filter Which cards can be exiled
     */
    @SerialName("CostExileXFromGraveyard")
    @Serializable
    data class ExileXFromGraveyard(
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : AbilityCost {
        override val description: String = "Exile X ${filter.description}s from your graveyard"

        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /** Discard your entire hand */
    @SerialName("CostDiscardHand")
    @Serializable
    data object DiscardHand : AbilityCost {
        override val description: String = "Discard your hand"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /** Discard self (the card with this ability) - used for cycling */
    @SerialName("CostDiscardSelf")
    @Serializable
    data object DiscardSelf : AbilityCost {
        override val description: String = "Discard this card"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /** Sacrifice self (the permanent with this ability) */
    @SerialName("CostSacrificeSelf")
    @Serializable
    data object SacrificeSelf : AbilityCost {
        override val description: String = "Sacrifice this permanent"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /** Exile self (the permanent with this ability) */
    @SerialName("CostExileSelf")
    @Serializable
    data object ExileSelf : AbilityCost {
        override val description: String = "Exile this creature"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /**
     * Exile the permanent that granted this activated ability to the source.
     * Used by cards like The Dominion Bracelet whose static ability grants an
     * activated ability to the equipped creature with "exile [the equipment]"
     * as part of the cost. The granter is resolved at activation time.
     */
    @SerialName("CostExileGrantingPermanent")
    @Serializable
    data object ExileGrantingPermanent : AbilityCost {
        override val description: String = "Exile the granting permanent"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /**
     * Sacrifice a creature of the type chosen when this permanent entered the battlefield.
     * Used by cards like Doom Cannon that choose a creature type on entry and reference it in costs.
     */
    @SerialName("CostSacrificeChosenCreatureType")
    @Serializable
    data object SacrificeChosenCreatureType : AbilityCost {
        override val description: String = "Sacrifice a creature of the chosen type"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /**
     * Tap permanents you control.
     * Example: "Tap five untapped Clerics you control"
     *
     * @property count Number of permanents to tap
     * @property filter Which permanents can be tapped
     */
    @SerialName("CostTapPermanents")
    @Serializable
    data class TapPermanents(
        val count: Int,
        val filter: GameObjectFilter = GameObjectFilter.Creature,
        val excludeSelf: Boolean = false
    ) : AbilityCost {
        override val description: String = buildString {
            append("Tap ")
            if (count == 1) {
                append(if (excludeSelf) "another untapped " else "an untapped ")
            } else {
                append("$count untapped ")
            }
            append(filter.description)
            if (count != 1) append("s")
            append(" you control")
        }

        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Tap a variable number of permanents you control, where the count equals the ability's X value.
     * Example: "Tap X untapped Knights you control" for Aryel, Knight of Windgrace.
     *
     * @property filter Which permanents can be tapped
     */
    @SerialName("CostTapXPermanents")
    @Serializable
    data class TapXPermanents(
        val filter: GameObjectFilter = GameObjectFilter.Creature
    ) : AbilityCost {
        override val description: String = "Tap X untapped ${filter.description}s you control"

        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /** Multiple costs combined */
    @SerialName("CostComposite")
    @Serializable
    data class Composite(val costs: List<AbilityCost>) : AbilityCost {
        override val description: String = costs.joinToString(", ") { it.description }

        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost {
            var changed = false
            val newCosts = costs.map {
                val n = it.applyTextReplacement(replacer)
                if (n !== it) changed = true
                n
            }
            return if (changed) copy(costs = newCosts) else this
        }
    }

    /** Tap the creature this aura is attached to ({T} enchanted creature) */
    @SerialName("CostTapAttachedCreature")
    @Serializable
    data object TapAttachedCreature : AbilityCost {
        override val description: String = "{T} enchanted creature"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /**
     * Return a permanent you control to its owner's hand.
     * Example: "Return an Elf you control to its owner's hand"
     *
     * @property filter Which permanents can be returned
     */
    @SerialName("CostReturnToHand")
    @Serializable
    data class ReturnToHand(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val count: Int = 1
    ) : AbilityCost {
        override val description: String = if (count == 1) {
            "Return a ${filter.description} you control to its owner's hand"
        } else {
            "Return $count ${filter.description}s you control to their owner's hand"
        }

        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /** Loyalty cost for planeswalker abilities */
    @SerialName("CostLoyalty")
    @Serializable
    data class Loyalty(val change: Int) : AbilityCost {
        override val description: String = if (change >= 0) "+$change" else "$change"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /**
     * Remove X +1/+1 counters from among creatures you control.
     * X is chosen by the player when activating the ability.
     * The engine auto-distributes counter removal across creatures.
     */
    @SerialName("CostRemoveXPlusOnePlusOneCounters")
    @Serializable
    data object RemoveXPlusOnePlusOneCounters : AbilityCost {
        override val description: String = "Remove X +1/+1 counters from among creatures you control"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /**
     * Remove a fixed number of +1/+1 counters from among permanents you control matching
     * a filter. Used for fixed-count costs that aren't creature-only — e.g., Iron Spider,
     * Stark Upgrade's "Remove two +1/+1 counters from among artifacts you control."
     *
     * The player chooses how to distribute the removal across matching permanents. Use
     * [RemoveXPlusOnePlusOneCounters] instead when the count is a player-chosen X.
     */
    @SerialName("CostRemovePlusOnePlusOneCounters")
    @Serializable
    data class RemovePlusOnePlusOneCounters(val filter: GameObjectFilter, val count: Int) : AbilityCost {
        override val description: String =
            "Remove $count +1/+1 counters from among ${filter.description}s you control"

        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Remove one or more counters of the specified type from this permanent.
     * Used for artifacts with charge/gem counters as activation costs.
     *
     * @property counterType The type of counter to remove (e.g., "gem", "charge")
     * @property count Number of counters to remove (defaults to 1)
     */
    @SerialName("CostRemoveCounterFromSelf")
    @Serializable
    data class RemoveCounterFromSelf(val counterType: String, val count: Int = 1) : AbilityCost {
        override val description: String = if (count == 1) {
            "Remove a $counterType counter from this permanent"
        } else {
            "Remove $count $counterType counters from this permanent"
        }
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /**
     * Forage: exile three cards from your graveyard or sacrifice a Food.
     * Used as an activated ability cost for Bloomburrow cards.
     */
    @SerialName("CostForage")
    @Serializable
    data object Forage : AbilityCost {
        override val description: String = "Forage"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /**
     * Blight N: put N -1/-1 counters on a creature you control.
     * Used as an activated ability cost (e.g., Gristle Glutton's "{T}, Blight 1:").
     * Requires at least one creature you control to activate.
     */
    @SerialName("CostBlight")
    @Serializable
    data class Blight(val amount: Int) : AbilityCost {
        override val description: String = "Blight $amount"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost = this
    }

    /**
     * Remove N counters of a specific type from among permanents matching a filter you control.
     * The player distributes which permanents contribute counters toward the total.
     *
     * Example: "Remove two +1/+1 counters from among artifacts you control"
     */
    @SerialName("CostRemoveCountersFromAmongFilteredPermanents")
    @Serializable
    data class RemoveCountersFromAmongFilteredPermanents(
        val counterType: String,
        val count: Int,
        val filter: GameObjectFilter
    ) : AbilityCost {
        override val description: String =
            "Remove $count $counterType counter${if (count == 1) "" else "s"} from among ${filter.description}s you control"
        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }
}
