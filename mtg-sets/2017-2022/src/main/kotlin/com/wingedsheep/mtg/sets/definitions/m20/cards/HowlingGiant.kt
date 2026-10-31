package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Howling Giant (M20 #177)
 * {5}{G}{G}  Creature — Giant Druid  5/5
 *
 * Reach
 * When this creature enters, create two 2/2 green Wolf creature tokens.
 */
val HowlingGiant = card("Howling Giant") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Giant Druid"
    oracleText = "Reach (This creature can block creatures with flying.)\n" +
        "When this creature enters, create two 2/2 green Wolf creature tokens."
    power = 5
    toughness = 5

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
            count = 2,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Grzegorz Rutkowski"
        flavorText = "He and his pack sing the same mournful song to the moon."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb8434fd-5ee5-4be9-a28d-9e04b1b94327.jpg"
    }
}
