package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Aviation Pioneer
 * {2}{U}
 * Creature — Human Artificer
 * 1/2
 * When this creature enters, create a 1/1 colorless Thopter artifact creature token with flying.
 */
val AviationPioneer = card("Aviation Pioneer") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Artificer"
    oracleText = "When this creature enters, create a 1/1 colorless Thopter artifact creature token with flying."
    power = 1
    toughness = 2
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/d/b/db0041d8-cacd-4057-8f99-37810edb4b7e.jpg?1783916667",
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Lake Hurwitz"
        flavorText = "\"They say perfection is unattainable, but they said that about flight too.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6966738-b4fc-4854-81b0-09de305854f2.jpg"
    }
}
