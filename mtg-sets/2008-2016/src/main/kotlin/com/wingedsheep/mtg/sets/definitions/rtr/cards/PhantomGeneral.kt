package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Phantom General
 * {3}{W}
 * Creature — Spirit Soldier
 * 2/3
 * Creature tokens you control get +1/+1.
 */
val PhantomGeneral = card("Phantom General") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit Soldier"
    power = 2
    toughness = 3
    oracleText = "Creature tokens you control get +1/+1."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.token().youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Christopher Moeller"
        flavorText = "After his death, the general was reunited with fallen soldiers from over thirty of his campaigns."
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11f42791-070a-4e3a-91c8-b801980abb76.jpg?1783940375"
    }
}
