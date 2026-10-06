package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Simulacrum
 * {1}{B}
 * Instant
 * You gain life equal to the damage dealt to you this turn. Simulacrum deals damage to target
 * creature you control equal to the damage dealt to you this turn.
 *
 * Both amounts read the per-turn damage-received tracker (`TurnTracker.DAMAGE_RECEIVED`): actual
 * damage dealt to you, not life lost — prevented damage doesn't count and life payment doesn't
 * either. Gaining life doesn't change the tracker, so both halves see the same number. The
 * single target is a creature you control; if it's illegal on resolution, the whole spell does
 * nothing (no life gain).
 */
val Simulacrum = card("Simulacrum") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "You gain life equal to the damage dealt to you this turn. Simulacrum deals damage to target creature you control equal to the damage dealt to you this turn."

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.GainLife(DynamicAmounts.damageReceivedThisTurn()) then
            Effects.DealDamage(DynamicAmounts.damageReceivedThisTurn(), creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35c3a78d-cc79-4187-929a-8aa1d1469990.jpg?1783948691"
    }
}
