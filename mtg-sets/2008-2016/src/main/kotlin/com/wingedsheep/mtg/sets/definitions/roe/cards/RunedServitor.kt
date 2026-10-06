package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

val RunedServitor = card("Runed Servitor") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    oracleText = "When this creature dies, each player draws a card."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.ForEachPlayer(Player.ActivePlayerFirst, Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "224"
        artist = "Mike Bierek"
        flavorText = "Scholars had puzzled for centuries over the ruins at Tal Terig. Its secrets had always lived within one rune-carved head."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9dbbbbf6-e3ed-4a36-bedf-11c3514ff965.jpg?1783941955"
    }
}
