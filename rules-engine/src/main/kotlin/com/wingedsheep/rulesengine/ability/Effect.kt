package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.player.PlayerId
import kotlinx.serialization.Serializable

/**
 * Sealed hierarchy of effects.
 * Effects define WHAT happens when an ability resolves.
 */
@Serializable
sealed interface Effect {
    /** Human-readable description of the effect */
    val description: String
}

// =============================================================================
// Life Effects
// =============================================================================

/**
 * Gain life effect.
 * "You gain X life" or "Target player gains X life"
 */
@Serializable
data class GainLifeEffect(
    val amount: Int,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "You gain $amount life"
        EffectTarget.Opponent -> "Target opponent gains $amount life"
        EffectTarget.AnyPlayer -> "Target player gains $amount life"
        else -> "Gain $amount life"
    }
}

/**
 * Lose life effect.
 * "You lose X life" or "Target player loses X life"
 */
@Serializable
data class LoseLifeEffect(
    val amount: Int,
    val target: EffectTarget = EffectTarget.Opponent
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "You lose $amount life"
        EffectTarget.Opponent -> "Target opponent loses $amount life"
        EffectTarget.AnyPlayer -> "Target player loses $amount life"
        else -> "Lose $amount life"
    }
}

// =============================================================================
// Damage Effects
// =============================================================================

/**
 * Deal damage effect.
 * "Deal X damage to target creature/player"
 */
@Serializable
data class DealDamageEffect(
    val amount: Int,
    val target: EffectTarget
) : Effect {
    override val description: String = "Deal $amount damage to ${target.description}"
}

// =============================================================================
// Card Drawing Effects
// =============================================================================

/**
 * Draw cards effect.
 * "Draw X cards" or "Target player draws X cards"
 */
@Serializable
data class DrawCardsEffect(
    val count: Int,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Draw ${if (count == 1) "a card" else "$count cards"}"
        EffectTarget.Opponent -> "Target opponent draws ${if (count == 1) "a card" else "$count cards"}"
        else -> "Target player draws ${if (count == 1) "a card" else "$count cards"}"
    }
}

/**
 * Discard cards effect.
 * "Discard X cards" or "Target player discards X cards"
 */
@Serializable
data class DiscardCardsEffect(
    val count: Int,
    val target: EffectTarget = EffectTarget.Opponent
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Discard ${if (count == 1) "a card" else "$count cards"}"
        EffectTarget.Opponent -> "Target opponent discards ${if (count == 1) "a card" else "$count cards"}"
        else -> "Target player discards ${if (count == 1) "a card" else "$count cards"}"
    }
}

// =============================================================================
// Creature Effects
// =============================================================================

/**
 * Destroy target creature/permanent effect.
 * "Destroy target creature"
 */
@Serializable
data class DestroyEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Destroy ${target.description}"
}

/**
 * Exile target effect.
 * "Exile target creature/permanent"
 */
@Serializable
data class ExileEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Exile ${target.description}"
}

/**
 * Return to hand effect.
 * "Return target creature to its owner's hand"
 */
@Serializable
data class ReturnToHandEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Return ${target.description} to its owner's hand"
}

/**
 * Tap/Untap target effect.
 * "Tap target creature" or "Untap target creature"
 */
@Serializable
data class TapUntapEffect(
    val target: EffectTarget,
    val tap: Boolean = true
) : Effect {
    override val description: String = "${if (tap) "Tap" else "Untap"} ${target.description}"
}

// =============================================================================
// Stat Modification Effects
// =============================================================================

/**
 * Modify power/toughness effect.
 * "Target creature gets +X/+Y until end of turn"
 */
@Serializable
data class ModifyStatsEffect(
    val powerModifier: Int,
    val toughnessModifier: Int,
    val target: EffectTarget,
    val untilEndOfTurn: Boolean = true
) : Effect {
    override val description: String = buildString {
        append("${target.description} gets ")
        append(if (powerModifier >= 0) "+$powerModifier" else "$powerModifier")
        append("/")
        append(if (toughnessModifier >= 0) "+$toughnessModifier" else "$toughnessModifier")
        if (untilEndOfTurn) append(" until end of turn")
    }
}

/**
 * Add counters effect.
 * "Put X +1/+1 counters on target creature"
 */
@Serializable
data class AddCountersEffect(
    val counterType: String,
    val count: Int,
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Put $count $counterType counter${if (count != 1) "s" else ""} on ${target.description}"
}

// =============================================================================
// Mana Effects
// =============================================================================

/**
 * Add mana effect.
 * "Add {G}" or "Add {R}{R}"
 */
@Serializable
data class AddManaEffect(
    val color: Color,
    val amount: Int = 1
) : Effect {
    override val description: String = "Add ${"{${color.symbol}}".repeat(amount)}"
}

/**
 * Add colorless mana effect.
 * "Add {C}{C}"
 */
@Serializable
data class AddColorlessManaEffect(
    val amount: Int
) : Effect {
    override val description: String = "Add ${"{C}".repeat(amount)}"
}

// =============================================================================
// Token Effects
// =============================================================================

/**
 * Create token effect.
 * "Create a 1/1 white Soldier creature token"
 */
@Serializable
data class CreateTokenEffect(
    val count: Int = 1,
    val power: Int,
    val toughness: Int,
    val colors: Set<Color>,
    val creatureTypes: Set<String>,
    val keywords: Set<Keyword> = emptySet()
) : Effect {
    override val description: String = buildString {
        append("Create ")
        append(if (count == 1) "a" else "$count")
        append(" $power/$toughness ")
        append(colors.joinToString(" and ") { it.displayName.lowercase() })
        append(" ")
        append(creatureTypes.joinToString(" "))
        append(" creature token")
        if (count != 1) append("s")
        if (keywords.isNotEmpty()) {
            append(" with ")
            append(keywords.joinToString(", ") { it.name.lowercase() })
        }
    }
}

// =============================================================================
// Composite Effects
// =============================================================================

/**
 * Multiple effects that happen together.
 */
@Serializable
data class CompositeEffect(
    val effects: List<Effect>
) : Effect {
    override val description: String = effects.joinToString(". ") { it.description }
}

// =============================================================================
// Effect Targets
// =============================================================================

/**
 * Defines who/what an effect targets.
 */
@Serializable
sealed interface EffectTarget {
    val description: String

    /** The controller of the source ability */
    @Serializable
    data object Controller : EffectTarget {
        override val description: String = "you"
    }

    /** An opponent of the controller */
    @Serializable
    data object Opponent : EffectTarget {
        override val description: String = "target opponent"
    }

    /** Any player */
    @Serializable
    data object AnyPlayer : EffectTarget {
        override val description: String = "target player"
    }

    /** The source permanent itself */
    @Serializable
    data object Self : EffectTarget {
        override val description: String = "this creature"
    }

    /** Target creature */
    @Serializable
    data object TargetCreature : EffectTarget {
        override val description: String = "target creature"
    }

    /** Target creature an opponent controls */
    @Serializable
    data object TargetOpponentCreature : EffectTarget {
        override val description: String = "target creature an opponent controls"
    }

    /** Target creature you control */
    @Serializable
    data object TargetControlledCreature : EffectTarget {
        override val description: String = "target creature you control"
    }

    /** Target permanent */
    @Serializable
    data object TargetPermanent : EffectTarget {
        override val description: String = "target permanent"
    }

    /** Target nonland permanent */
    @Serializable
    data object TargetNonlandPermanent : EffectTarget {
        override val description: String = "target nonland permanent"
    }

    /** Any target (creature or player) */
    @Serializable
    data object AnyTarget : EffectTarget {
        override val description: String = "any target"
    }

    /** All creatures */
    @Serializable
    data object AllCreatures : EffectTarget {
        override val description: String = "all creatures"
    }

    /** All creatures you control */
    @Serializable
    data object AllControlledCreatures : EffectTarget {
        override val description: String = "creatures you control"
    }

    /** All creatures opponents control */
    @Serializable
    data object AllOpponentCreatures : EffectTarget {
        override val description: String = "creatures your opponents control"
    }

    /** Each opponent */
    @Serializable
    data object EachOpponent : EffectTarget {
        override val description: String = "each opponent"
    }

    /** Each player */
    @Serializable
    data object EachPlayer : EffectTarget {
        override val description: String = "each player"
    }
}
