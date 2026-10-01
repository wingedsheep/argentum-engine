package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hazardous Blast
 * {3}{R}
 * Sorcery
 * Hazardous Blast deals 1 damage to each creature your opponents control.
 * Creatures your opponents control can't block this turn.
 */
val HazardousBlast = card("Hazardous Blast") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Hazardous Blast deals 1 damage to each creature your opponents control. Creatures your opponents control can't block this turn."

    spell {
        effect = Patterns.Group.dealDamageToAll(1, GroupFilter.AllCreaturesOpponentsControl) then
            Effects.CantBlockGroup(GroupFilter.AllCreaturesOpponentsControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Zoltan Boros"
        flavorText = "The Machine Orthodoxy finds strength in numbers. The Quiet Furnace finds that amusing."
        imageUri = "https://cards.scryfall.io/normal/front/0/3/0380a4c9-a5d3-465f-8584-e546b54f91e0.jpg?1783918030"
    }
}
