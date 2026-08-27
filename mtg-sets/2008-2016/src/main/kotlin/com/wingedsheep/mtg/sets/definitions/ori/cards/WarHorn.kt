package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * War Horn
 * {3}
 * Artifact
 * Attacking creatures you control get +1/+0.
 */
val WarHorn = card("War Horn") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Attacking creatures you control get +1/+0."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.attacking().youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "243"
        artist = "Lars Grant-West"
        flavorText = "The reverberations course through the warriors' veins, beat with their hearts, and pound with their footfalls as they charge into battle."
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45c17671-7c4a-4114-b2b7-d34af4e2f8c1.jpg?1783938307"
    }
}
