package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Haunter of Nightveil
 * {3}{U}{B}
 * Creature — Spirit
 * 3/4
 * Creatures your opponents control get -1/-0.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val HaunterOfNightveil = card("Haunter of Nightveil") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 4
    oracleText = "Creatures your opponents control get -1/-0."

    staticAbility {
        ability = ModifyStats(
            powerBonus = -1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.opponentControls()),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Igor Kieryluk"
        flavorText = "The Dimir employ the spirits of Ravnica's ancient, nameless dead, guiding their malevolence toward selected victims."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/438683f5-adfa-42ae-a6fb-c4649a8a30ab.jpg"
    }
}
