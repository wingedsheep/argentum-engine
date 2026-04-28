package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

/**
 * Unique identifier for an ability instance.
 */
@JvmInline
@Serializable
value class AbilityId(val value: String) {
    companion object {
        private val counter = AtomicLong(0)

        fun generate(): AbilityId = AbilityId("ability_${counter.incrementAndGet()}")

        /**
         * Create a deterministic AbilityId for a Class level-up ability.
         * Uses a fixed prefix so the engine can match the ability when activated.
         */
        fun classLevelUp(targetLevel: Int): AbilityId = AbilityId("class_level_up_$targetLevel")

        /**
         * Create a deterministic AbilityId for an intrinsic mana ability granted by
         * a basic land subtype (CR 305.7). The engine synthesizes these on the fly
         * from projected basic-land subtypes so the same id resolves for any land
         * with the matching subtype, regardless of card definition.
         */
        fun intrinsicMana(colorSymbol: Char): AbilityId = AbilityId("intrinsic_mana_$colorSymbol")
    }
}
