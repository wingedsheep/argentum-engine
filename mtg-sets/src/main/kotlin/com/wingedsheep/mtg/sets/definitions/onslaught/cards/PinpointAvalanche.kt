package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pinpoint Avalanche
 * {3}{R}{R}
 * Instant
 * Pinpoint Avalanche deals 4 damage to target creature.
 * The damage can't be prevented.
 */
val PinpointAvalanche = card("Pinpoint Avalanche") {
    manaCost = "{3}{R}{R}"
    typeLine = "Instant"
    oracleText = "Pinpoint Avalanche deals 4 damage to target creature. The damage can't be prevented."

    spell {
        target = Targets.Creature
        effect = DealDamageEffect(4, EffectTarget.ContextTarget(0), cantBePrevented = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "221"
        artist = "Darrell Riche"
        flavorText = "\"Some solve problems by thinking and talking. Others use rocks.\" —Toggo, goblin weaponsmith"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5cf8876-4c7d-4779-9363-d0a58bb7d851.jpg?1562945960"
    }
}
