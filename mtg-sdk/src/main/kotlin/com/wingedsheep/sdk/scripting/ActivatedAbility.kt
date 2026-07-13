package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.wingedsheep.sdk.dsl.craft

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
    /**
     * True for the equip ability synthesized by [com.wingedsheep.sdk.dsl.CardBuilder.equipAbility].
     * Lets the engine recognise equip activations for permissions that key off them — e.g.
     * "you may activate equip abilities any time you could cast an instant" (instant-speed equip)
     * and "the first equip ability you activate each turn costs {0}" (Forge Anew).
     */
    val isEquipAbility: Boolean = false,
    val activateFromZone: Zone = Zone.BATTLEFIELD,
    val promptOnDraw: Boolean = false,
    val descriptionOverride: String? = null,
    val hasConvoke: Boolean = false,
    /**
     * When true, this ability's mana [cost] is a *waterbend* cost (Avatar: The Last Airbender):
     * while paying it, the controller may tap untapped artifacts and/or creatures they control,
     * each paying for {1} of the generic mana in the cost. Generic-only — a tapped permanent never
     * covers a colored pip. Mirrors [hasConvoke] but the eligible-permanent set is widened to
     * artifacts and payment is restricted to generic. The number of permanents that may be tapped
     * is bounded by the generic mana in the cost (CR: "for each generic mana in that cost").
     */
    val hasWaterbend: Boolean = false,
    /**
     * True for an *exhaust* ability (Avatar: The Last Airbender, returning from Edge of Eternities;
     * CR 702.177). "Exhaust — [cost]: [effect]" means "[cost]: [effect]. Activate only once."
     *
     * This flag is purely the keyword marker: it drives the "Exhaust — " prefix in [description]
     * and lets tooling recognise an exhaust ability. The *rules* meaning — once per the lifetime of
     * this object — is carried by an [ActivationRestriction.Once] in [restrictions], which the
     * `activatedAbility { isExhaust = true }` DSL adds automatically. Per CR 400.7 / 403.4 a
     * permanent that leaves and re-enters the battlefield is a new object whose exhaust abilities may
     * be activated again — exactly what the per-object [ActivationRestriction.Once] tracker provides
     * (it lives on the permanent entity and resets on a new entity), so no game-scoped tracking is
     * needed.
     */
    val isExhaust: Boolean = false,
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
    val xManaRestriction: Set<Color> = emptySet(),
    /**
     * Minimum legal value for the `{X}` in this ability's cost (CR 601.2b analogue for activated
     * abilities). Defaults to 0. Set to 1 for "X can't be 0" abilities (Gogo, Master of Mimicry:
     * "{X}{X}, {T}: … X can't be 0."). The activated-ability X-choice decision clamps its lower
     * bound to this value.
     */
    val minimumXValue: Int = 0,
    /**
     * When true, this activated ability can't be copied by effects that copy abilities (CR 707.10e).
     * The engine tags the ability instance on the stack with a can't-be-copied marker so a
     * copy-ability effect (e.g. another Gogo, Master of Mimicry) produces no copy of it. Models
     * "This ability can't be copied."
     */
    val cantBeCopied: Boolean = false
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
        get() = descriptionOverride ?: describeWithCost(cost)

    /**
     * Render this ability's menu text against a (possibly cost-reduced) [effectiveCost], applying
     * the keyword-action prefixes in printed order: "Exhaust — Waterbend {N}: ...". The legal-action
     * enumerator calls this when a cost reduction means the printed [cost] no longer matches what the
     * player will pay, so the rebuilt label keeps the same prefixes the [description] getter shows.
     */
    fun describeWithCost(effectiveCost: AbilityCost): String {
        // A waterbend cost renders as "Waterbend {N}" (the keyword action precedes the cost).
        val base = effectiveCost.description.ifEmpty { "{0}" }
        val costText = if (hasWaterbend) "Waterbend $base" else base
        // An exhaust ability prefixes "Exhaust — " before the (already waterbend-prefixed) cost.
        val prefixed = if (isExhaust) "Exhaust — $costText" else costText
        return "$prefixed: ${effect.description}"
    }

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

    /**
     * A single shared payable thing — see [CostAtom]. Carries the payable concepts that mean the
     * same in any cost context (pay mana, pay life, sacrifice/tap/return permanents, discard cards,
     * exile from a zone). The activated-ability-specific costs — [Free], [Tap]/[Untap], the
     * X-variable costs, the self-referential sacrifice/exile costs, counter removal, [Loyalty],
     * [Composite], and named mechanics ([Forage], [Blight], [Craft]) — stay as their own subtypes.
     * Ability-cost text leads a clause, so the description is the atom's phrase capitalized.
     */
    @SerialName("CostAtomWrapper")
    @Serializable
    data class Atom(val atom: CostAtom) : AbilityCost {
        override val description: String get() = atom.description.replaceFirstChar { it.uppercase() }

        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost {
            val newAtom = atom.applyTextReplacement(replacer)
            return if (newAtom !== atom) copy(atom = newAtom) else this
        }
    }

    /** No cost ({0}) — the ability is free to activate */
    @SerialName("CostFree")
    @Serializable
    data object Free : AbilityCost {
        override val description: String = "{0}"
    }

    /** Tap the permanent ({T}) */
    @SerialName("CostTap")
    @Serializable
    data object Tap : AbilityCost {
        override val description: String = "{T}"
    }

    /** Untap the permanent ({Q}) */
    @SerialName("CostUntap")
    @Serializable
    data object Untap : AbilityCost {
        override val description: String = "{Q}"
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
    }

    /** Discard self (the card with this ability) - used for cycling */
    @SerialName("CostDiscardSelf")
    @Serializable
    data object DiscardSelf : AbilityCost {
        override val description: String = "Discard this card"
    }

    /**
     * Discard the specific card the controller drew most recently this turn (CR 121
     * "draws"). The engine tracks the per-player most-recently-drawn card on
     * `GameState.lastCardDrawnThisTurnByPlayer`, updated whenever a `CardsDrawnEvent`
     * fires during a turn and cleared at every turn boundary. Used by Jandor's Ring
     * ("{2}, {T}, Discard the last card you drew this turn: Draw a card.") and any
     * future card that scopes a discard cost to the same notion.
     *
     * The cost is unpayable when (a) the controller has not drawn a card this turn,
     * or (b) the tracked card has since left their hand. Per Scryfall ruling on
     * Jandor's Ring: "If you draw more than one card due to a spell or ability, you
     * must discard the last one of those drawn." — the tracker takes the *last* id
     * in a multi-card `CardsDrawnEvent`, then is overwritten by any later draw event.
     */
    @SerialName("CostDiscardLastDrawnThisTurn")
    @Serializable
    data object DiscardLastDrawnThisTurn : AbilityCost {
        override val description: String = "Discard the last card you drew this turn"
    }

    /** Sacrifice self (the permanent with this ability) */
    @SerialName("CostSacrificeSelf")
    @Serializable
    data object SacrificeSelf : AbilityCost {
        override val description: String = "Sacrifice this permanent"
    }

    /** Exile self (the permanent with this ability) */
    @SerialName("CostExileSelf")
    @Serializable
    data object ExileSelf : AbilityCost {
        override val description: String = "Exile this creature"
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
    }

    /**
     * Sacrifice a creature of the type chosen when this permanent entered the battlefield.
     * Used by cards like Doom Cannon that choose a creature type on entry and reference it in costs.
     */
    @SerialName("CostSacrificeChosenCreatureType")
    @Serializable
    data object SacrificeChosenCreatureType : AbilityCost {
        override val description: String = "Sacrifice a creature of the chosen type"
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
    }

    /** Loyalty cost for planeswalker abilities */
    @SerialName("CostLoyalty")
    @Serializable
    data class Loyalty(val change: Int) : AbilityCost {
        override val description: String = if (change >= 0) "+$change" else "$change"
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
    }

    /**
     * Forage: exile three cards from your graveyard or sacrifice a Food.
     * Used as an activated ability cost for Bloomburrow cards.
     */
    @SerialName("CostForage")
    @Serializable
    data object Forage : AbilityCost {
        override val description: String = "Forage"
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

    /**
     * Craft materials (CR 702.167a). The combined "Exile this permanent, Exile [filter] from
     * among permanents you control and/or [filter] cards from your graveyard" portion of the
     * Craft activated ability.
     *
     * Atomic because the self-exile and the materials-exile are a single named cost shape in
     * the Comprehensive Rules — pulling them apart would let a card pay one half without the
     * other. The mana portion stays as a separate [Mana] sub-cost combined via [Composite].
     *
     * Payment selects [minCount]+ materials from a **single combined pool** spanning the
     * activator's controlled battlefield permanents and graveyard cards matching [filter]
     * (CR 702.167b). The exiled materials are recorded on the source's
     * [com.wingedsheep.engine.state.components.battlefield.CraftedFromExiledComponent] so the
     * back face's CDA (Mastercraft Raptor's "power = total power of the exiled cards") can read
     * them after the source returns to the battlefield transformed.
     *
     * Pair with [com.wingedsheep.sdk.scripting.effects.ReturnSelfFromExileTransformedEffect]
     * as the ability effect and `timing = TimingRule.SorcerySpeed`.
     *
     * **Composition note.** The legal-action enumerator surfaces a Craft sub-cost as the sole
     * payload on [com.wingedsheep.engine.legalactions.AdditionalCostData]: any sibling
     * AdditionalCost-bearing sub-cost inside the same `Composite` (tap, sacrifice, discard,
     * counter-removal, …) will be dropped from the legal-action DTO. In practice the
     * `card { craft(...) }` helper only pairs Craft with `Mana`, which travels through the
     * separate mana-payment channel, so this is exhaustive. If you ever compose Craft with a
     * second AdditionalCost-bearing sub-cost, generalize `ActivatedAbilityEnumerator.buildAdditionalCostData`
     * to merge both payloads first.
     *
     * @property filter Material filter (typically the same one used in the Craft display text,
     *   e.g. `Filters.Dinosaur` for Saheeli's Lattice).
     * @property minCount Minimum number of materials to exile (1 for "one or more").
     * @property maxCount Maximum number of materials, or `null` for unbounded ("... or more").
     *   Most Craft costs name an exact count — "Craft with artifact" exiles exactly one
     *   artifact, "Craft with two creatures" exactly two — so those set `maxCount == minCount`;
     *   only "one or more" / "N or more" wordings leave it `null` (CR 702.167a).
     */
    @SerialName("CostCraft")
    @Serializable
    data class Craft(
        val filter: GameObjectFilter,
        val minCount: Int = 1,
        val maxCount: Int? = null,
    ) : AbilityCost {
        override val description: String = buildString {
            val exactlyOne = maxCount == 1
            val article = if (filter.description.firstOrNull()?.lowercaseChar() in listOf('a', 'e', 'i', 'o', 'u')) "an" else "a"
            append("Exile this permanent, Exile ")
            when {
                maxCount == null && minCount == 1 -> append("one or more ")
                maxCount == null -> append("$minCount or more ")
                exactlyOne -> append("$article ")
                else -> append("$minCount ")
            }
            append(filter.description)
            if (!exactlyOne) append("s")
            append(" you control and/or ")
            if (exactlyOne) append("$article ")
            append(filter.description)
            append(if (exactlyOne) " card from your graveyard" else " cards from your graveyard")
        }

        override fun applyTextReplacement(replacer: TextReplacer): AbilityCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }
}
