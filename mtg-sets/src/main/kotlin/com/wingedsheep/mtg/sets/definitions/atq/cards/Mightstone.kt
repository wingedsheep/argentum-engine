package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Mightstone
 * {4}
 * Artifact
 * Attacking creatures get +1/+0.
 */
val Mightstone = card("Mightstone") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Attacking creatures get +1/+0."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter.AttackingCreatures
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Pete Venters"
        flavorText = "While exploring the sacred cave of Koilos with his brother Mishra and their master Tocasia, Urza fell behind in the Hall of Tagsin, where he discovered the remarkable Mightstone."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b28ba599-5299-4831-a118-1712ada10ef6.jpg?1562932633"
    }
}
