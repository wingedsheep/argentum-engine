package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Exultant Cultist (Eldritch Moon #59)
 * {2}{U}
 * Creature — Human Wizard
 * 2/2
 *
 * When this creature dies, draw a card.
 */
val ExultantCultist = card("Exultant Cultist") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "When this creature dies, draw a card."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Joseph Meehan"
        flavorText = "Amid the gibbering chorus that rose from the drownyard temple, Jace recognized " +
            "a single word: \"Emrakul.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2c22e0a-81c5-485f-81f0-3e85397108e0.jpg"
    }
}
