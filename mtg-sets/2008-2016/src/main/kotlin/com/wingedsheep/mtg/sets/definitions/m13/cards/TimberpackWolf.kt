package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Timberpack Wolf
 * {1}{G}
 * Creature — Wolf
 * 2/2
 * This creature gets +1/+1 for each other creature you control named Timberpack Wolf.
 *
 * Seven Dwarves' shape: a self-only dynamic buff counting the other same-named creatures you control.
 */
val TimberpackWolf = card("Timberpack Wolf") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    power = 2
    toughness = 2
    oracleText = "This creature gets +1/+1 for each other creature you control named Timberpack Wolf."

    staticAbility {
        val otherWolves = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Creature.named("Timberpack Wolf"),
            excludeSelf = true
        ).count()
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = otherWolves,
            toughnessBonus = otherWolves
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "194"
        artist = "John Avon"
        flavorText = "\"The need for the pack is bred in their bones. Their true strength will not show itself for any other purpose.\"\n—Garruk Wildspeaker"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d16928c9-0470-46ec-b92d-0d6ff9f23ef7.jpg?1783940464"
        ruling("2018-03-16", "Because damage remains marked on a creature until it's removed as the turn ends, nonlethal damage dealt to Timberpack Wolf may become lethal if other Timberpack Wolves leave the battlefield during that turn.")
    }
}
