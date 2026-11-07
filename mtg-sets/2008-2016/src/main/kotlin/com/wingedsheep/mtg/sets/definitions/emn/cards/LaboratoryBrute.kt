package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Laboratory Brute (Eldritch Moon #67)
 * {3}{U}
 * Creature — Zombie Horror
 * 3/3
 *
 * When this creature enters, mill four cards.
 */
val LaboratoryBrute = card("Laboratory Brute") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Horror"
    oracleText = "When this creature enters, mill four cards."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(4)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Yeong-Hao Han"
        flavorText = "It keeps intruders out and experiments in."
        imageUri = "https://cards.scryfall.io/normal/front/3/2/325f228e-46ff-43e2-bc1d-c2d2df443c51.jpg"
    }
}
