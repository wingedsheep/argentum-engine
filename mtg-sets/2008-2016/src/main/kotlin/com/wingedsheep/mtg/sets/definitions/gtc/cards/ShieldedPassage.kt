package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shielded Passage
 * {W}
 * Instant
 * Prevent all damage that would be dealt to target creature this turn.
 */
val ShieldedPassage = card("Shielded Passage") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Prevent all damage that would be dealt to target creature this turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.PreventDamage(target = creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Raymond Swanland"
        flavorText = "Izzet delved into the depths of Ravnica's past, and Selesnya followed without fear."
        imageUri = "https://cards.scryfall.io/normal/front/6/5/6546b6c4-73b2-41b3-9ff9-316e9ce916e5.jpg"
    }
}
