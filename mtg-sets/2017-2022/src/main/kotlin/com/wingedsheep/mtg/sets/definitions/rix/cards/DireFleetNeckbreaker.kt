package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dire Fleet Neckbreaker
 * {2}{B}{R}
 * Creature — Orc Pirate
 * 3/2
 * Attacking Pirates you control get +2/+0.
 *
 * Only creatures can attack, so the "attacking Pirates" are Pirate creatures (including itself).
 */
val DireFleetNeckbreaker = card("Dire Fleet Neckbreaker") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Orc Pirate"
    oracleText = "Attacking Pirates you control get +2/+0."
    power = 3
    toughness = 2

    staticAbility {
        ability = ModifyStats(
            powerBonus = 2,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.PIRATE).attacking().youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "156"
        artist = "Daarken"
        flavorText = "The buccaneers scaled the sides of buildings as easily as they boarded enemy ships."
        imageUri = "https://cards.scryfall.io/normal/front/5/1/5146dd15-f696-42c3-b0e5-6a1b1d7be6b4.jpg?1783935277"
    }
}
