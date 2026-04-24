package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sealed interface for replacement effects.
 *
 * Replacement effects intercept game events BEFORE they happen and
 * modify or replace them entirely. Unlike triggered abilities, replacement
 * effects do not use the stack.
 *
 * The system is compositional - replacement effects are specified by combining
 * a GameEvent filter with a modification/replacement behavior.
 *
 * Examples:
 * ```kotlin
 * // Doubling Season (tokens)
 * DoubleTokenCreation(
 *     appliesTo = GameEvent.TokenCreationEvent(controller = ControllerFilter.You)
 * )
 *
 * // Hardened Scales
 * ModifyCounterPlacement(
 *     modifier = 1,
 *     appliesTo = GameEvent.CounterPlacementEvent(
 *         counterType = CounterTypeFilter.PlusOnePlusOne,
 *         recipient = RecipientFilter.CreatureYouControl
 *     )
 * )
 *
 * // Rest in Peace
 * RedirectZoneChange(
 *     newDestination = Zone.Exile,
 *     appliesTo = GameEvent.ZoneChangeEvent(to = Zone.Graveyard)
 * )
 *
 * // Prevention shield (combat damage from red sources)
 * PreventDamage(
 *     appliesTo = GameEvent.DamageEvent(
 *         recipient = RecipientFilter.You,
 *         source = SourceFilter.HasColor(Color.RED),
 *         damageType = DamageType.Combat
 *     )
 * )
 * ```
 */
@Serializable
sealed interface ReplacementEffect : TextReplaceable<ReplacementEffect> {
    /** Human-readable description of the replacement effect */
    val description: String

    /** What type of event this replacement intercepts (compositional) */
    val appliesTo: GameEvent
}

// =============================================================================
// Token Replacement Effects
// =============================================================================

/**
 * Double the number of tokens created.
 * Example: Doubling Season, Parallel Lives, Anointed Procession
 */
@SerialName("DoubleTokenCreation")
@Serializable
data class DoubleTokenCreation(
    override val appliesTo: GameEvent = GameEvent.TokenCreationEvent()
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, create twice that many of those tokens instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Modify the number of tokens created by a fixed amount.
 */
@SerialName("ModifyTokenCount")
@Serializable
data class ModifyTokenCount(
    val modifier: Int,
    override val appliesTo: GameEvent = GameEvent.TokenCreationEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, create ")
        if (modifier > 0) append("$modifier more")
        else append("${-modifier} fewer")
        append(" of those tokens instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Counter Replacement Effects
// =============================================================================

/**
 * Double the number of counters placed.
 * Example: Doubling Season (counters), Corpsejack Menace, Innkeeper's Talent Level 3
 *
 * @param placedByYou When true, only applies when the controller of this effect is the
 *                    player putting the counters (e.g., Innkeeper's Talent: "If YOU would
 *                    put one or more counters..."). When false, applies regardless of who
 *                    is placing the counters — the recipient filter on [appliesTo] is the
 *                    sole "you control" gate (e.g., Doubling Season: "on a permanent you
 *                    control").
 */
@SerialName("DoubleCounterPlacement")
@Serializable
data class DoubleCounterPlacement(
    val placedByYou: Boolean = false,
    override val appliesTo: GameEvent = GameEvent.CounterPlacementEvent(
        counterType = CounterTypeFilter.PlusOnePlusOne,
        recipient = RecipientFilter.CreatureYouControl
    )
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, place twice that many counters instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Add additional counters when counters are placed.
 * Example: Hardened Scales (+1), Winding Constrictor (+1), Branching Evolution (double)
 */
@SerialName("ModifyCounterPlacement")
@Serializable
data class ModifyCounterPlacement(
    val modifier: Int = 1,
    override val appliesTo: GameEvent = GameEvent.CounterPlacementEvent(
        counterType = CounterTypeFilter.PlusOnePlusOne,
        recipient = RecipientFilter.CreatureYouControl
    )
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, ")
        if (modifier > 0) {
            append("$modifier additional counter")
            if (modifier > 1) append("s")
            append(" is placed")
        } else {
            append("${-modifier} fewer counter")
            if (-modifier > 1) append("s")
            append(" is placed")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Zone Change Replacement Effects
// =============================================================================

/**
 * Redirect a zone change to a different destination.
 * Example: Rest in Peace (graveyard → exile), Leyline of the Void
 */
@SerialName("RedirectZoneChange")
@Serializable
data class RedirectZoneChange(
    val newDestination: Zone,
    override val appliesTo: GameEvent
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, put it into ${newDestination.displayName} instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Permanent enters the battlefield tapped.
 * Example: Glacial Fortress (conditional), tap lands, Thalia Heretic Cathar, Steam Vents (pay life)
 *
 * @param unlessCondition If non-null, the permanent only enters tapped when this condition is NOT met.
 *                        Used for "check lands" like Sulfur Falls ("enters tapped unless you control an Island or a Mountain").
 * @param payLifeCost If non-null, the player may pay this much life to have the permanent enter untapped.
 *                    Used for "shock lands" like Steam Vents ("you may pay 2 life. If you don't, it enters tapped").
 */
@SerialName("EntersTapped")
@Serializable
data class EntersTapped(
    val unlessCondition: Condition? = null,
    val payLifeCost: Int? = null,
    override val appliesTo: GameEvent = GameEvent.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = when {
        payLifeCost != null -> "As this permanent enters, you may pay $payLifeCost life. If you don't, it enters tapped."
        unlessCondition != null -> "This permanent enters tapped unless ${unlessCondition.description}"
        else -> "If ${appliesTo.description}, it enters tapped"
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Permanent/creature enters with counters.
 * Example: Master Biomancer, Metallic Mimic
 */
@SerialName("EntersWithCounters")
@Serializable
data class EntersWithCounters(
    val counterType: CounterTypeFilter = CounterTypeFilter.PlusOnePlusOne,
    val count: Int,
    val selfOnly: Boolean = false,
    override val appliesTo: GameEvent = GameEvent.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, it enters with $count ${counterType.description} counters"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Permanent/creature enters with a dynamic number of counters.
 * Example: Stag Beetle (enters with X +1/+1 counters where X = number of other creatures)
 *
 * @param otherOnly When true, this effect only applies to OTHER creatures entering
 *                  (not the permanent with this replacement effect). Used for
 *                  Gev, Scaled Scorch: "Other creatures you control enter with additional counters."
 */
@SerialName("EntersWithDynamicCounters")
@Serializable
data class EntersWithDynamicCounters(
    val counterType: CounterTypeFilter = CounterTypeFilter.PlusOnePlusOne,
    val count: DynamicAmount,
    val otherOnly: Boolean = false,
    override val appliesTo: GameEvent = GameEvent.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, it enters with ${count.description} ${counterType.description} counters"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newCount = count.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newCount !== count) copy(appliesTo = newAppliesTo, count = newCount) else this
    }
}

/**
 * Undying - if creature dies without +1/+1 counters, return it with one.
 */
@SerialName("Undying")
@Serializable
data class UndyingEffect(
    override val appliesTo: GameEvent = GameEvent.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        from = Zone.BATTLEFIELD,
        to = Zone.GRAVEYARD
    )
) : ReplacementEffect {
    override val description: String =
        "When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield with a +1/+1 counter"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Persist - if creature dies without -1/-1 counters, return it with one.
 */
@SerialName("Persist")
@Serializable
data class PersistEffect(
    override val appliesTo: GameEvent = GameEvent.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        from = Zone.BATTLEFIELD,
        to = Zone.GRAVEYARD
    )
) : ReplacementEffect {
    override val description: String =
        "When this creature dies, if it had no -1/-1 counters on it, return it to the battlefield with a -1/-1 counter"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Damage Replacement Effects
// =============================================================================

/**
 * Prevent damage.
 * Example: Fog effects, protection, damage shields
 */
@SerialName("PreventDamage")
@Serializable
data class PreventDamage(
    val amount: Int? = null,  // null = prevent all
    override val appliesTo: GameEvent
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, prevent ")
        if (amount == null) {
            append("that damage")
        } else {
            append("$amount of that damage")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Redirect damage to another target.
 * Example: Pariah, Stuffy Doll, Boros Reckoner
 */
@SerialName("RedirectDamage")
@Serializable
data class RedirectDamage(
    val redirectTo: EffectTarget,
    override val appliesTo: GameEvent
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, that damage is dealt to ${redirectTo.description} instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Double damage dealt.
 * Example: Furnace of Rath, Insult // Injury
 */
@SerialName("DoubleDamage")
@Serializable
data class DoubleDamage(
    override val appliesTo: GameEvent
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, it deals double that damage instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Modify damage dealt by a fixed amount.
 * Example: Valley Flamecaller ("If a Lizard, Mouse, Otter, or Raccoon you control would deal
 * damage to a permanent or player, it deals that much damage plus 1 instead.")
 */
@SerialName("ModifyDamageAmount")
@Serializable
data class ModifyDamageAmount(
    val modifier: Int,
    override val appliesTo: GameEvent
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, it deals that much damage plus $modifier instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Draw Replacement Effects
// =============================================================================

/**
 * Replace drawing with another effect.
 * Example: Underrealm Lich (look at 3, put 1 in hand, rest in graveyard)
 */
@SerialName("ReplaceDrawWith")
@Serializable
data class ReplaceDrawWithEffect(
    val replacementEffect: Effect,
    val optional: Boolean = false,
    override val appliesTo: GameEvent = GameEvent.DrawEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, ")
        if (optional) append("you may ")
        append("instead ${replacementEffect.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newReplacementEffect = replacementEffect.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newReplacementEffect !== replacementEffect)
            copy(appliesTo = newAppliesTo, replacementEffect = newReplacementEffect)
        else this
    }
}

/**
 * Prevent drawing (with optional replacement).
 * Example: Spirit of the Labyrinth (second draw), Narset Parter of Veils
 */
@SerialName("PreventDraw")
@Serializable
data class PreventDraw(
    override val appliesTo: GameEvent = GameEvent.DrawEvent()
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, that draw doesn't happen"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Life Replacement Effects
// =============================================================================

/**
 * Prevent life gain.
 * Example: Erebos, Sulfuric Vortex
 */
@SerialName("PreventLifeGain")
@Serializable
data class PreventLifeGain(
    override val appliesTo: GameEvent = GameEvent.LifeGainEvent()
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, that player gains no life instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Damage can't be prevented.
 * Example: Sunspine Lynx, Leyline of Punishment
 *
 * While a permanent with this replacement effect is on the battlefield,
 * all damage is treated as though it can't be prevented (protection,
 * prevention shields, etc. are ignored).
 */
@SerialName("DamageCantBePrevented")
@Serializable
data class DamageCantBePrevented(
    override val appliesTo: GameEvent = GameEvent.DamageEvent()
) : ReplacementEffect {
    override val description: String = "Damage can't be prevented"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Replace life gain with another effect.
 * Example: Tainted Remedy (life gain becomes life loss)
 */
@SerialName("ReplaceLifeGain")
@Serializable
data class ReplaceLifeGain(
    val replacementEffect: Effect,
    override val appliesTo: GameEvent = GameEvent.LifeGainEvent()
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, instead ${replacementEffect.description}"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newReplacementEffect = replacementEffect.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newReplacementEffect !== replacementEffect)
            copy(appliesTo = newAppliesTo, replacementEffect = newReplacementEffect)
        else this
    }
}

/**
 * Modify life gain amount.
 * Example: Alhammarret's Archive (double life gain)
 */
@SerialName("ModifyLifeGain")
@Serializable
data class ModifyLifeGain(
    val multiplier: Int = 2,
    override val appliesTo: GameEvent = GameEvent.LifeGainEvent()
) : ReplacementEffect {
    override val description: String = when (multiplier) {
        2 -> "If ${appliesTo.description}, gain twice that much life instead"
        0 -> "If ${appliesTo.description}, gain no life instead"
        else -> "If ${appliesTo.description}, gain $multiplier times that much life instead"
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Copy Replacement Effects
// =============================================================================

/**
 * Enter the battlefield as a copy of a permanent.
 * Example: Clone ("You may have this creature enter as a copy of any creature on the battlefield")
 * Example: Clever Impersonator ("You may have this creature enter as a copy of any nonland permanent on the battlefield")
 *
 * When this permanent would enter the battlefield, the controller may choose a permanent
 * on the battlefield matching [copyFilter]. If they do, the permanent enters as a copy of that permanent.
 * If they don't (or can't), the permanent enters as itself (typically 0/0 and dies).
 *
 * @param copyFilter Filter for what can be copied. Defaults to creatures only (Clone).
 *                   Use [GameObjectFilter.Companion.NonlandPermanent] for Clever Impersonator.
 * @param filterByTotalManaSpent When true, only creatures with mana value ≤ total mana spent
 *                                to cast this spell are valid copy targets. Used for Mockingbird.
 * @param additionalSubtypes Subtypes to add to the copy (e.g., "Bird" for Mockingbird).
 * @param additionalKeywords Keywords to grant to the copy (e.g., FLYING for Mockingbird).
 */
@SerialName("EntersAsCopy")
@Serializable
data class EntersAsCopy(
    val optional: Boolean = true,
    val copyFilter: GameObjectFilter = GameObjectFilter.Creature,
    val filterByTotalManaSpent: Boolean = false,
    val additionalSubtypes: List<String> = emptyList(),
    val additionalKeywords: List<Keyword> = emptyList(),
    override val appliesTo: GameEvent = GameEvent.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = run {
        val filterDesc = copyFilter.description
        if (optional) {
            "You may have this creature enter as a copy of any $filterDesc on the battlefield"
        } else {
            "This creature enters as a copy of any $filterDesc on the battlefield"
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Enter-With-Choice Replacement Effects
// =============================================================================

/**
 * What the player chooses as the permanent enters.
 */
@Serializable
enum class ChoiceType {
    /** Choose a color (e.g., Riptide Replicator, Ward Sliver) */
    COLOR,
    /** Choose a creature type (e.g., Doom Cannon, Cover of Darkness) */
    CREATURE_TYPE,
    /** Choose another creature you control (e.g., Dauntless Bodyguard) */
    CREATURE_ON_BATTLEFIELD
}

/**
 * As this permanent enters, make a choice. The chosen value is stored on
 * the permanent for use by other abilities.
 *
 * Replaces the former EntersWithColorChoice, EntersWithCreatureTypeChoice,
 * and EntersWithCreatureChoice with a single parameterized type.
 *
 * @param choiceType What kind of choice to present
 * @param chooser Who makes the choice (default: controller)
 *
 * Examples:
 * - Riptide Replicator: `EntersWithChoice(ChoiceType.COLOR)`
 * - Callous Oppressor: `EntersWithChoice(ChoiceType.CREATURE_TYPE, chooser = Player.Opponent)`
 * - Dauntless Bodyguard: `EntersWithChoice(ChoiceType.CREATURE_ON_BATTLEFIELD)`
 */
@SerialName("EntersWithChoice")
@Serializable
data class EntersWithChoice(
    val choiceType: ChoiceType,
    val chooser: Player = Player.You,
    /**
     * When [choiceType] is [ChoiceType.CREATURE_TYPE], restrict the choosable
     * subtypes to this list. `null` means any creature type is allowed (the
     * default, matching cards like Three Tree City). Used by cards that
     * enumerate a specific tribal shortlist such as Eclipsed Realms.
     */
    val allowedCreatureTypes: List<String>? = null,
    override val appliesTo: GameEvent = GameEvent.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = when (choiceType) {
        ChoiceType.COLOR -> if (chooser == Player.Opponent) {
            "As this permanent enters, an opponent chooses a color"
        } else {
            "As this permanent enters, choose a color"
        }
        ChoiceType.CREATURE_TYPE -> if (chooser == Player.Opponent) {
            "As this permanent enters, an opponent chooses a creature type"
        } else {
            "As this permanent enters, choose a creature type"
        }
        ChoiceType.CREATURE_ON_BATTLEFIELD -> "As this creature enters, choose another creature you control"
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Enters-With-Reveal-Counters Replacement Effect
// =============================================================================

/**
 * As this creature enters, you may reveal any number of cards from a zone
 * that match a filter. For each card revealed, put N counters on this creature.
 *
 * Generalizes the Amplify mechanic — the default parameters reproduce Amplify
 * exactly (reveal creatures from hand sharing a type, +1/+1 counters).
 *
 * @param filter Which cards can be revealed (default: creatures sharing a creature type with this)
 * @param revealSource Which zone to reveal from (default: HAND)
 * @param counterType Counter type description (default: "+1/+1")
 * @param countersPerReveal How many counters per revealed card
 *
 * Examples:
 * - Embalmed Brawler (Amplify 1): `EntersWithRevealCounters(countersPerReveal = 1)`
 * - Kilnmouth Dragon (Amplify 3): `EntersWithRevealCounters(countersPerReveal = 3)`
 */
@SerialName("EntersWithRevealCounters")
@Serializable
data class EntersWithRevealCounters(
    val filter: GameObjectFilter = GameObjectFilter(
        cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.SharesCreatureTypeWithSource)
    ),
    val revealSource: Zone = Zone.HAND,
    val counterType: String = "+1/+1",
    val countersPerReveal: Int,
    override val appliesTo: GameEvent = GameEvent.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String =
        "As this creature enters, you may reveal any number of cards from your ${revealSource.name.lowercase()} that match. For each card revealed this way, put $countersPerReveal $counterType counter${if (countersPerReveal > 1) "s" else ""} on it."

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newFilter !== filter || newAppliesTo !== appliesTo) copy(filter = newFilter, appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Damage-to-Counters Replacement Effect
// =============================================================================

/**
 * Replace damage dealt to a player with counters on this permanent.
 * Example: Force Bubble — "If damage would be dealt to you, put that many
 * depletion counters on this enchantment instead."
 *
 * @param counterType The type of counter to add (e.g., "depletion")
 * @param sacrificeThreshold If non-null, sacrifice this permanent when it has
 *        this many or more counters of the specified type (state-triggered ability)
 */
@SerialName("ReplaceDamageWithCounters")
@Serializable
data class ReplaceDamageWithCounters(
    val counterType: String,
    val sacrificeThreshold: Int? = null,
    override val appliesTo: GameEvent = GameEvent.DamageEvent(
        recipient = RecipientFilter.You
    )
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, put that many $counterType counters on this permanent instead")
        if (sacrificeThreshold != null) {
            append(". When there are $sacrificeThreshold or more $counterType counters on this permanent, sacrifice it")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Extra Turn Replacement Effects
// =============================================================================

/**
 * Prevent extra turns from being taken.
 * Example: Ugin's Nexus — "If a player would begin an extra turn, that player
 * skips that turn instead."
 *
 * Checked by TakeExtraTurnExecutor before granting extra turns.
 */
@SerialName("PreventExtraTurns")
@Serializable
data class PreventExtraTurns(
    override val appliesTo: GameEvent = GameEvent.ExtraTurnEvent()
) : ReplacementEffect {
    override val description: String =
        "If a player would begin an extra turn, that player skips that turn instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Redirect a zone change to a different destination AND execute an additional effect.
 * Example: Ugin's Nexus — "If Ugin's Nexus would be put into a graveyard from
 * the battlefield, instead exile it and take an extra turn after this one."
 *
 * Extends RedirectZoneChange with an additional effect that fires when the replacement applies.
 *
 * @param newDestination The zone to redirect to (e.g., Exile)
 * @param additionalEffect The effect to execute when replacement fires (e.g., TakeExtraTurnEffect)
 * @param selfOnly When true, only applies when the entity being moved IS this permanent
 * @param appliesTo The zone change event this replacement intercepts
 */
@SerialName("RedirectZoneChangeWithEffect")
@Serializable
data class RedirectZoneChangeWithEffect(
    val newDestination: Zone,
    val additionalEffect: Effect,
    val selfOnly: Boolean = false,
    override val appliesTo: GameEvent
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, instead put it into ${newDestination.displayName} and ${additionalEffect.description}"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newAdditionalEffect = additionalEffect.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newAdditionalEffect !== additionalEffect)
            copy(appliesTo = newAppliesTo, additionalEffect = newAdditionalEffect)
        else this
    }
}

// =============================================================================
// Token Creation Replacement Effects
// =============================================================================

/**
 * Replace token creation with creating token copies of the equipped creature.
 * Example: Mirrormind Crown — "As long as this Equipment is attached to a creature,
 * the first time you would create one or more tokens each turn, you may instead
 * create that many tokens that are copies of equipped creature."
 *
 * @param optional If true, the player may choose whether to apply the replacement ("you may")
 * @param oncePerTurn If true, only applies to the first token creation each turn
 */
@SerialName("ReplaceTokenCreationWithEquippedCopy")
@Serializable
data class ReplaceTokenCreationWithEquippedCopy(
    val optional: Boolean = true,
    val oncePerTurn: Boolean = true,
    override val appliesTo: GameEvent = GameEvent.TokenCreationEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        append("As long as this Equipment is attached to a creature, ")
        if (oncePerTurn) append("the first time ")
        append("you would create one or more tokens")
        if (oncePerTurn) append(" each turn")
        append(", ")
        if (optional) append("you may instead ")
        else append("instead ")
        append("create that many tokens that are copies of equipped creature")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Generic Replacement Effect
// =============================================================================

/**
 * Generic replacement effect for complex scenarios.
 * Use when no specific replacement effect type fits.
 */
@SerialName("GenericReplacement")
@Serializable
data class GenericReplacementEffect(
    val replacement: Effect?,  // null = prevent entirely
    override val appliesTo: GameEvent,
    override val description: String
) : ReplacementEffect {
    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newReplacement = replacement?.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newReplacement !== replacement)
            copy(appliesTo = newAppliesTo, replacement = newReplacement)
        else this
    }
}
