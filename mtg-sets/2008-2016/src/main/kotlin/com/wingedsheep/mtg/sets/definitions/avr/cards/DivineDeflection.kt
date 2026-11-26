package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Divine Deflection
 * {X}{W}
 * Instant
 * Prevent the next X damage that would be dealt to you and/or permanents you control this turn. If
 * damage is prevented this way, Divine Deflection deals that much damage to any target.
 *
 * One X-point pool shared by you and your permanents: simultaneous damage to several of them is
 * split by you as it would be dealt. The reaction is part of the prevention (no stack) and hits the
 * target chosen as the spell was cast, once per damage event for what the pool prevented there; a
 * target that has gone since takes nothing, but the damage is still prevented.
 */
val DivineDeflection = card("Divine Deflection") {
    manaCost = "{X}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Prevent the next X damage that would be dealt to you and/or permanents you control this turn. " +
        "If damage is prevented this way, Divine Deflection deals that much damage to any target."

    spell {
        val victim = target(Targets.Any)
        effect = Effects.PreventDamage(
            amount = DynamicAmounts.xValue(),
            toGroup = GameObjectFilter.Permanent.youControl(),
            alsoToYou = true,
            onPrevented = Effects.DealDamage(DynamicAmounts.preventedDamage(), victim),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec06e42f-e294-44ef-8fa8-1d1a4c2090d8.jpg?1783940736"
    }
}
