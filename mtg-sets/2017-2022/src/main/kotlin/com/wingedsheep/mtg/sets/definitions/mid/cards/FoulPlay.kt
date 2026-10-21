package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Foul Play
 * {1}{B}
 * Sorcery
 * Destroy target creature with power 2 or less. Investigate. (Create a Clue token. It's an
 * artifact with "{2}, Sacrifice this token: Draw a card.")
 */
val FoulPlay = card("Foul Play") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature with power 2 or less. Investigate. (Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"
    spell {
        val t = target(TargetFilter.Creature.powerAtMost(2))
        effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true) then
            Effects.Investigate()
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Campbell White"
        flavorText = "The evidence was overwhelming. Tovolar's howlpack was getting bolder, smarter, and stronger."
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87e4b75c-e993-4983-8933-977be314bba6.jpg"
    }
}
