package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Sealed interface for replacement effects.
 *
 * Replacement effects intercept game actions BEFORE they happen and
 * modify or replace them entirely. Unlike triggered abilities, replacement
 * effects do not use the stack.
 *
 * Examples:
 * - Doubling Season: "If an effect would create tokens, it creates twice that many instead"
 * - Rest in Peace: "If a card would be put into a graveyard, exile it instead"
 * - Hardened Scales: "If counters would be placed, place one additional counter"
 *
 * Usage:
 * ```kotlin
 * staticAbility {
 *     replacementEffect = DoubleTokenCreation(
 *         appliesTo = ReplacementAppliesTo.TokensYouCreate
 *     )
 * }
 * ```
 */
@Serializable
sealed interface ReplacementEffect {
    /** Human-readable description of the replacement effect */
    val description: String

    /** What type of event this replacement applies to */
    val appliesTo: ReplacementAppliesTo
}

/**
 * Defines what type of game event a replacement effect intercepts.
 */
@Serializable
sealed interface ReplacementAppliesTo {
    val description: String

    // =========================================================================
    // Token Creation
    // =========================================================================

    /**
     * When an effect would create tokens under your control.
     * Example: Doubling Season
     */
    @Serializable
    data object TokensYouCreate : ReplacementAppliesTo {
        override val description = "an effect would create one or more tokens under your control"
    }

    /**
     * When any effect would create tokens.
     */
    @Serializable
    data object AnyTokenCreation : ReplacementAppliesTo {
        override val description = "an effect would create one or more tokens"
    }

    // =========================================================================
    // Counter Placement
    // =========================================================================

    /**
     * When one or more +1/+1 counters would be placed on a creature you control.
     * Example: Hardened Scales, Doubling Season
     */
    @Serializable
    data object PlusCountersOnYourCreatures : ReplacementAppliesTo {
        override val description = "one or more +1/+1 counters would be placed on a creature you control"
    }

    /**
     * When any counters would be placed on a permanent you control.
     */
    @Serializable
    data class CountersOnYourPermanents(val counterType: String? = null) : ReplacementAppliesTo {
        override val description = buildString {
            append("one or more ")
            if (counterType != null) append("$counterType ")
            append("counters would be placed on a permanent you control")
        }
    }

    // =========================================================================
    // Damage
    // =========================================================================

    /**
     * When damage would be dealt to you.
     * Example: Platinum Emperion (prevents life change)
     */
    @Serializable
    data object DamageToYou : ReplacementAppliesTo {
        override val description = "damage would be dealt to you"
    }

    /**
     * When damage would be dealt to a creature you control.
     * Example: Damage prevention effects
     */
    @Serializable
    data object DamageToYourCreatures : ReplacementAppliesTo {
        override val description = "damage would be dealt to a creature you control"
    }

    /**
     * When combat damage would be dealt.
     */
    @Serializable
    data object CombatDamage : ReplacementAppliesTo {
        override val description = "combat damage would be dealt"
    }

    // =========================================================================
    // Zone Changes
    // =========================================================================

    /**
     * When a card would be put into a graveyard from anywhere.
     * Example: Rest in Peace
     */
    @Serializable
    data object CardsEnteringGraveyard : ReplacementAppliesTo {
        override val description = "a card would be put into a graveyard from anywhere"
    }

    /**
     * When a creature you control would die.
     * Example: Undying, Persist
     */
    @Serializable
    data object YourCreaturesDying : ReplacementAppliesTo {
        override val description = "a creature you control would die"
    }

    /**
     * When a permanent would enter the battlefield.
     * Example: Blood Moon (lands enter as Mountains)
     */
    @Serializable
    data object PermanentsEnteringBattlefield : ReplacementAppliesTo {
        override val description = "a permanent would enter the battlefield"
    }

    /**
     * When a creature would enter the battlefield under your control.
     * Example: Leyline of Singularity
     */
    @Serializable
    data object CreaturesEnteringUnderYourControl : ReplacementAppliesTo {
        override val description = "a creature would enter the battlefield under your control"
    }

    // =========================================================================
    // Card Drawing
    // =========================================================================

    /**
     * When you would draw a card.
     * Example: Dredge, Underrealm Lich
     */
    @Serializable
    data object YouDrawing : ReplacementAppliesTo {
        override val description = "you would draw a card"
    }

    // =========================================================================
    // Life Changes
    // =========================================================================

    /**
     * When you would gain life.
     * Example: Tainted Remedy
     */
    @Serializable
    data object YouGainingLife : ReplacementAppliesTo {
        override val description = "you would gain life"
    }

    /**
     * When you would lose life.
     */
    @Serializable
    data object YouLosingLife : ReplacementAppliesTo {
        override val description = "you would lose life"
    }
}

// =============================================================================
// Token Replacement Effects
// =============================================================================

/**
 * Double the number of tokens created.
 * Example: Doubling Season, Parallel Lives
 */
@Serializable
data class DoubleTokenCreation(
    override val appliesTo: ReplacementAppliesTo = ReplacementAppliesTo.TokensYouCreate
) : ReplacementEffect {
    override val description = "If ${appliesTo.description}, it creates twice that many of those tokens instead"
}

/**
 * Modify the number of tokens created by a fixed amount.
 */
@Serializable
data class ModifyTokenCount(
    val modifier: Int,
    override val appliesTo: ReplacementAppliesTo = ReplacementAppliesTo.TokensYouCreate
) : ReplacementEffect {
    override val description = buildString {
        append("If ${appliesTo.description}, it creates ")
        if (modifier > 0) append("$modifier more")
        else append("${-modifier} fewer")
        append(" of those tokens instead")
    }
}

// =============================================================================
// Counter Replacement Effects
// =============================================================================

/**
 * Double the number of counters placed.
 * Example: Doubling Season
 */
@Serializable
data class DoubleCounterPlacement(
    override val appliesTo: ReplacementAppliesTo = ReplacementAppliesTo.PlusCountersOnYourCreatures
) : ReplacementEffect {
    override val description = "If ${appliesTo.description}, twice that many of those counters are placed instead"
}

/**
 * Add additional counters when counters are placed.
 * Example: Hardened Scales (+1), Winding Constrictor (+1)
 */
@Serializable
data class AdditionalCounters(
    val additionalCount: Int = 1,
    override val appliesTo: ReplacementAppliesTo = ReplacementAppliesTo.PlusCountersOnYourCreatures
) : ReplacementEffect {
    override val description = "If ${appliesTo.description}, $additionalCount additional counter is placed"
}

// =============================================================================
// Enter the Battlefield Replacement Effects
// =============================================================================

/**
 * Permanent enters the battlefield tapped.
 * Example: Glacial Fortress (conditional), basic tap lands
 */
@Serializable
data class EntersTapped(
    val filter: CardFilter? = null,
    override val appliesTo: ReplacementAppliesTo = ReplacementAppliesTo.PermanentsEnteringBattlefield
) : ReplacementEffect {
    override val description = buildString {
        if (filter != null) {
            append("${filter.description} enters the battlefield tapped")
        } else {
            append("This permanent enters the battlefield tapped")
        }
    }
}

/**
 * Creature enters with +1/+1 counters.
 * Example: Master Biomancer
 */
@Serializable
data class EntersWithCounters(
    val counterType: String = "+1/+1",
    val count: Int,
    override val appliesTo: ReplacementAppliesTo = ReplacementAppliesTo.CreaturesEnteringUnderYourControl
) : ReplacementEffect {
    override val description = "If ${appliesTo.description}, it enters with $count additional $counterType counters"
}

// =============================================================================
// Death Replacement Effects
// =============================================================================

/**
 * If a creature would die, return it to the battlefield instead (with modification).
 * Example: Undying (return with +1/+1 counter if it had none)
 */
@Serializable
data class UndyingEffect(
    override val appliesTo: ReplacementAppliesTo = ReplacementAppliesTo.YourCreaturesDying
) : ReplacementEffect {
    override val description = "When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield with a +1/+1 counter"
}

/**
 * If a creature would die, return it to the battlefield instead (with -1/-1 counter).
 * Example: Persist
 */
@Serializable
data class PersistEffect(
    override val appliesTo: ReplacementAppliesTo = ReplacementAppliesTo.YourCreaturesDying
) : ReplacementEffect {
    override val description = "When this creature dies, if it had no -1/-1 counters on it, return it to the battlefield with a -1/-1 counter"
}

/**
 * If a card would be put into a graveyard, exile it instead.
 * Example: Rest in Peace
 */
@Serializable
data class ExileInsteadOfGraveyard(
    override val appliesTo: ReplacementAppliesTo = ReplacementAppliesTo.CardsEnteringGraveyard
) : ReplacementEffect {
    override val description = "If a card would be put into a graveyard from anywhere, exile it instead"
}

// =============================================================================
// Damage Replacement Effects
// =============================================================================

/**
 * Prevent damage.
 * Example: Fog effects, protection
 */
@Serializable
data class PreventDamage(
    val amount: Int? = null,  // null = prevent all
    override val appliesTo: ReplacementAppliesTo
) : ReplacementEffect {
    override val description = buildString {
        append("If ${appliesTo.description}, prevent ")
        if (amount == null) {
            append("that damage")
        } else {
            append("$amount of that damage")
        }
    }
}

/**
 * Redirect damage to another target.
 * Example: Pariah, Stuffy Doll
 */
@Serializable
data class RedirectDamage(
    val redirectTo: EffectTarget,
    override val appliesTo: ReplacementAppliesTo
) : ReplacementEffect {
    override val description = "If ${appliesTo.description}, that damage is dealt to ${redirectTo.description} instead"
}

// =============================================================================
// Draw Replacement Effects
// =============================================================================

/**
 * Replace drawing with another effect.
 * Example: Underrealm Lich (look at 3, put 1 in hand, rest in graveyard)
 */
@Serializable
data class ReplaceDrawWithEffect(
    val replacementEffect: Effect,
    override val appliesTo: ReplacementAppliesTo = ReplacementAppliesTo.YouDrawing
) : ReplacementEffect {
    override val description = "If ${appliesTo.description}, instead ${replacementEffect.description}"
}
