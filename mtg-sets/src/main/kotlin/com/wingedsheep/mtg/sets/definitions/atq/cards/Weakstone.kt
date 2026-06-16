package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Weakstone
 * {4}
 * Artifact
 * Attacking creatures get -1/-0.
 */
val Weakstone = card("Weakstone") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Attacking creatures get -1/-0."

    staticAbility {
        ability = ModifyStats(
            powerBonus = -1,
            toughnessBonus = 0,
            filter = GroupFilter.AttackingCreatures
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Justin Hampton"
        flavorText = "During the brothers' childhood, Tocasia took them to explore the sacred cave of Koilos. There, in the Hall of Tagsin, Mishra discovered the mysterious weakstone."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46adf48f-99d2-440e-9129-794584c1ea21.jpg?1562909619"
    }
}
