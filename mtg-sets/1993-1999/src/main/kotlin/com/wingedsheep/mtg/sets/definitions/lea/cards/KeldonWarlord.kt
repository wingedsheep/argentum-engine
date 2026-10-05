package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

val KeldonWarlord = card("Keldon Warlord") {
    manaCost = "{2}{R}{R}"
    typeLine = "Creature — Human Barbarian"
    oracleText = "Keldon Warlord's power and toughness are each equal to the number of non-Wall creatures you control."
    colorIdentity = "R"
    dynamicStats(DynamicAmounts.battlefield(
        Player.You, GameObjectFilter.Creature.notSubtype(Subtype.WALL)
    ).count())
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Kev Brockschmidt"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8fe3fd83-969c-4add-888f-86f4306b067c.jpg?1783948685"
        ruling("2008-08-01", "This is a Characteristic-Defining Ability. It checks the number of non-Wall creatures you control continuously, and applies in all zones. It is never “locked in”.")
    }
}
