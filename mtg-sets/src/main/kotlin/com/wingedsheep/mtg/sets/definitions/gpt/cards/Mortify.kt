package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mortify
 * {1}{W}{B}
 * Instant
 * Destroy target creature or enchantment.
 */
val Mortify = card("Mortify") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Instant"
    oracleText = "Destroy target creature or enchantment."

    spell {
        val t = target("creature or enchantment", Targets.CreatureOrEnchantment)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Glen Angus"
        flavorText = "The eyes let flow with tears, then blood, then the very soul—the whole wrung inside out, dripping down into the blackened puddle of the past."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b2c5187-71c7-4801-8a76-339c67322d35.jpg?1593272729"
    }
}
