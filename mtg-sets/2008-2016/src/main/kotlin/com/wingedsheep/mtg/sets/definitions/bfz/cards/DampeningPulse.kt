package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dampening Pulse
 * {3}{U}
 * Enchantment
 * Creatures your opponents control get -1/-0.
 */
val DampeningPulse = card("Dampening Pulse") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
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
        collectorNumber = "75"
        artist = "Johann Bodin"
        flavorText = "\"Quiet now, you dreadful vermin.\"\n—Noyan Dar, Tazeem roilmage"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a4393f6-54d7-4963-a3c0-b3a083c2440c.jpg?1783938210"
    }
}
