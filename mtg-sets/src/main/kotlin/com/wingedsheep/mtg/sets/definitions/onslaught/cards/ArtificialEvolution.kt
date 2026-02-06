package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChangeCreatureTypeTextEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.TargetSpellOrPermanent

/**
 * Artificial Evolution
 * {U}
 * Instant
 * Change the text of target spell or permanent by replacing all instances of one
 * creature type with another. The new creature type can't be Wall.
 * (This effect lasts indefinitely.)
 */
val ArtificialEvolution = card("Artificial Evolution") {
    manaCost = "{U}"
    typeLine = "Instant"
    oracleText = "Change the text of target spell or permanent by replacing all instances of one creature type with another. The new creature type can't be Wall. (This effect lasts indefinitely.)"

    spell {
        target = TargetSpellOrPermanent()
        effect = ChangeCreatureTypeTextEffect(
            target = EffectTarget.ContextTarget(0),
            excludedTypes = listOf("Wall")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Daniel Ljunggren"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f46894d1-2503-43fa-938e-7bbf19101d13.jpg?1562952988"
    }
}
