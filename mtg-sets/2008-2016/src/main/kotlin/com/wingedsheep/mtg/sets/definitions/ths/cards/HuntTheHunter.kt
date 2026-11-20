package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hunt the Hunter
 * {G}
 * Sorcery
 * Target green creature you control gets +2/+2 until end of turn. It fights target green
 * creature an opponent controls.
 */
val HuntTheHunter = card("Hunt the Hunter") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target green creature you control gets +2/+2 until end of turn. " +
        "It fights target green creature an opponent controls."

    spell {
        val mine = target(TargetFilter(GameObjectFilter.Creature.withColor(Color.GREEN).youControl()))
        val theirs = target(TargetFilter(GameObjectFilter.Creature.withColor(Color.GREEN).opponentControls()))
        effect = Effects.ModifyStats(2, 2, mine) then
            Effects.Fight(mine, theirs)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Ryan Barger"
        flavorText = "\"A grudge is a tattoo worn on the inside.\"\n—Setessan warrior saying"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61d4899f-11a4-4a2c-a499-3a447b792f86.jpg?1783939746"
    }
}
