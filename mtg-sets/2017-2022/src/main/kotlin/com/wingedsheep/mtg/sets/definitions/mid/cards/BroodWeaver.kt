package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Brood Weaver
 * {3}{G}
 * Creature — Spider
 * 2/4
 * Reach
 * When this creature dies, create a 1/2 green Spider creature token with reach.
 */
val BroodWeaver = card("Brood Weaver") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    oracleText = "Reach\n" +
        "When this creature dies, create a 1/2 green Spider creature token with reach."
    power = 2
    toughness = 4

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Spider"),
            keywords = setOf(Keyword.REACH)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "173"
        artist = "Slawomir Maniak"
        flavorText = "Travelers in the Somberwald scan the ground for werewolves and other threats, unaware of what lurks above."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e24e2f49-70f9-445f-af03-2ef43798004a.jpg"
    }
}
