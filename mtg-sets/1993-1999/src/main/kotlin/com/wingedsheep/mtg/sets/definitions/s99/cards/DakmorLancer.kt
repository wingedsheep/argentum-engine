package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val DakmorLancer = card("Dakmor Lancer") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Knight"
    oracleText = "When this creature enters, destroy target nonblack creature."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature.notColor(Color.BLACK))
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Chippy"
        flavorText = "The darkness of his shield reflects the inky blackness of his soul."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d012ddf-abe1-4de9-89cb-78d82afb9e7b.jpg?1783946037"
    }
}
