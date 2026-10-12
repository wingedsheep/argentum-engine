package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Argothian Opportunist
 * {2}{G}
 * Creature — Human Scout
 * 3/2
 * When this creature enters, create a tapped Powerstone token.
 */
val ArgothianOpportunist = card("Argothian Opportunist") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Scout"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreatePowerstone(tapped = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Ina Wong"
        flavorText = "\"Nature adapts. So should we.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b22586ce-efcc-4b20-83f9-c23b8eeaa88c.jpg?1783920053"
    }
}
