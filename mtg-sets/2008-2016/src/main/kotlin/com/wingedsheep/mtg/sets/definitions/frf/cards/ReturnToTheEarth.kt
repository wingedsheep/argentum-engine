package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Return to the Earth
 * {3}{G}
 * Instant
 * Destroy target artifact, enchantment, or creature with flying.
 */
val ReturnToTheEarth = card("Return to the Earth") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target artifact, enchantment, or creature with flying."

    spell {
        val target = target(
            TargetFilter(
                GameObjectFilter.Artifact or
                    GameObjectFilter.Enchantment or
                    GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
            ),
        )
        effect = Effects.Destroy(target)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Mark Winters"
        flavorText = "\"Dragons are feared for their breath and their bite, but it is their wings that make them unconquerable. Bring them to earth, and they die like any other beast.\"\n—Kassur, Sultai hunter"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95a53144-2ef3-47d9-a176-73d620202df6.jpg"
    }
}
