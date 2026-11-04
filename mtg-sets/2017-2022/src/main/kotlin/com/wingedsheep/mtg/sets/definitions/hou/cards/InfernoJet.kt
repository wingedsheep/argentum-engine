package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

val InfernoJet = card("Inferno Jet") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Inferno Jet deals 6 damage to target opponent or planeswalker.\nCycling {2} ({2}, Discard this card: Draw a card.)"

    spell {
        val t = target(Targets.OpponentOrPlaneswalker)
        effect = Effects.DealDamage(6, t)
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "Jakub Kasper"
        flavorText = "\"When the whole world is crazy, it's important to find something you can rely on.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c6a43fe-369d-4943-a825-570eb3cceba4.jpg?1783936026"
    }
}
