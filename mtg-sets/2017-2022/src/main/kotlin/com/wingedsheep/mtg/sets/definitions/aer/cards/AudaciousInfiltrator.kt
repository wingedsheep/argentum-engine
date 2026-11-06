package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Audacious Infiltrator
 * {1}{W}
 * Creature — Dwarf Rogue
 * 3/1
 * This creature can't be blocked by artifact creatures.
 */
val AudaciousInfiltrator = card("Audacious Infiltrator") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Rogue"
    power = 3
    toughness = 1
    oracleText = "This creature can't be blocked by artifact creatures."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.ArtifactCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Jakub Kasper"
        flavorText = "\"This little critter can turn all of you to scrap in under thirty seconds! Come on, who wants to be the first to go? Try and stop me!\""
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36a348bb-cdc5-4e2a-933f-21f91faab891.jpg?1783936783"
    }
}
