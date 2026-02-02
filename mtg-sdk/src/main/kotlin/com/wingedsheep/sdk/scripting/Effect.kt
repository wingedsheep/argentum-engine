package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Sealed hierarchy of effects.
 * Effects define WHAT happens when an ability resolves.
 *
 * Effect implementations are organized across multiple files in the effects/ subdirectory:
 * - LifeEffects.kt - Life gain/loss effects
 * - DamageEffects.kt - Damage dealing effects
 * - DrawingEffects.kt - Card draw/discard effects
 * - RemovalEffects.kt - Destroy/exile/sacrifice effects
 * - PermanentEffects.kt - Stats/counters/keywords/tap effects
 * - ManaEffects.kt - Mana-producing effects
 * - TokenEffects.kt - Token creation effects
 * - LibraryEffects.kt - Library manipulation effects
 * - StackEffects.kt - Counterspell effects
 * - PlayerEffects.kt - Turn/phase manipulation effects
 * - CombatEffects.kt - Combat-specific effects
 * - CompositeEffects.kt - Composite/modal/may effects
 * - TransformEffects.kt - Transform effects
 *
 * Supporting types are organized in subdirectories:
 * - values/ - DynamicAmount, PlayerReference, ZoneReference, EffectVariable
 * - filters/ - CardFilter, CountFilter, CreatureFilters, SpellFilter
 * - targets/ - EffectTarget
 * - costs/ - PayCost
 */
@Serializable
sealed interface Effect {
    /** Human-readable description of the effect */
    val description: String

    /**
     * Operator to chain effects.
     * Allows syntax like: EffectA then EffectB
     */
    infix fun then(next: Effect): CompositeEffect {
        return if (this is CompositeEffect) {
            CompositeEffect(this.effects + next)
        } else {
            CompositeEffect(listOf(this, next))
        }
    }
}
