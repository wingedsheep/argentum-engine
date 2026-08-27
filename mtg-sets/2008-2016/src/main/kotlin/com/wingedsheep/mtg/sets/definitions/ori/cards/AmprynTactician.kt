package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ampryn Tactician
 * {2}{W}{W}
 * Creature — Human Soldier
 * 3/3
 * When this creature enters, creatures you control get +1/+1 until end of turn.
 */
val AmprynTactician = card("Ampryn Tactician") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, creatures you control get +1/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(1, 1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Cynthia Sheppard"
        flavorText = "\"It's all a game. You shouldn't get too attached to the pieces.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82a2e1d9-6763-4024-a18b-982d96395553.jpg?1783938365"
    }
}
