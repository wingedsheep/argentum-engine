package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val AvenOfEnduringHope = card("Aven of Enduring Hope") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Cleric"
    oracleText = "Flying\nWhen this creature enters, you gain 3 life."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "James Ryman"
        flavorText = "\"This storm will calm. We must believe that.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02c1310c-1b54-42dd-bf24-889770fa2ded.jpg?1783936066"
    }
}
