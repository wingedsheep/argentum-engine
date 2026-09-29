package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val GoblinSettler = card("Goblin Settler") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "When this creature enters, destroy target land."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        val land = target(TargetFilter.Land)
        effect = Effects.Destroy(land)
        description = "When this creature enters, destroy target land."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Carl Critchlow"
        flavorText = "Be it ever so crumbled, there's no place like home."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/7525100d-32f9-464b-b0c8-0767c3b730b7.jpg?1783946028"
    }
}
