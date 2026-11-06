package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Filigree Crawler
 * {4}
 * Artifact Creature — Insect
 * 2/2
 * When this creature dies, create a 1/1 colorless Thopter artifact creature token with flying.
 */
val FiligreeCrawler = card("Filigree Crawler") {
    manaCost = "{4}"
    typeLine = "Artifact Creature — Insect"
    power = 2
    toughness = 2
    oracleText = "When this creature dies, create a 1/1 colorless Thopter artifact creature token with flying."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/f/d/fd03a2b9-bfd7-4e1d-b660-e9f945699b78.jpg?1783937238",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "James Paick"
        flavorText = "\"The most harmonious designs are based on relationships found in nature.\"\n—Venand Kapur, the Gearherder"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d8e817c-079e-4e7c-a8ca-54634f30bf36.jpg?1783936729"
    }
}
