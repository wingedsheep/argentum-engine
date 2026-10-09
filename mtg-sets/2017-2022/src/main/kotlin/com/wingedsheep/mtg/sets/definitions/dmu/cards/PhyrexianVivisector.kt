package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

val PhyrexianVivisector = card("Phyrexian Vivisector") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Human"
    oracleText = "Whenever a creature you control dies, scry 1. (Look at the top card of your library. You may put that card on the bottom.)"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Irina Nordsol"
        flavorText = "Before he was the Father of Machines, Yawgmoth was a twisted Thran physician, and his legacy of medical atrocities lives on in New Phyrexia."
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65617a8f-4bd8-4edb-b5ec-e20b4482390b.jpg?1783921329"
    }
}
