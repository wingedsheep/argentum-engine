package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Sporeback Wolf
 * {1}{G}
 * Creature — Wolf
 * 2/2
 * During your turn, this creature gets +0/+2.
 */
val SporebackWolf = card("Sporeback Wolf") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    oracleText = "During your turn, this creature gets +0/+2."
    power = 2
    toughness = 2
    staticAbility {
        ability = ConditionalStaticAbility(ability = ModifyStats(0, 2, Filters.Self), condition = Conditions.IsYourTurn)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "223"
        artist = "Nicholas Elias"
        flavorText = "The mushrooms growing in the wolves' fur possess curative properties incredible enough to tempt many alchemists into risking their lives to track one down."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3fb04879-9348-4d4f-9a23-c82fd99d04c6.jpg?1782703036"
    }
}
