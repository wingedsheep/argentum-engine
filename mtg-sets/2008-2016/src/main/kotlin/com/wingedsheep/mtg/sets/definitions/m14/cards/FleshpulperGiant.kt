package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fleshpulper Giant
 * {5}{R}{R}
 * Creature — Giant
 * 4/4
 * When this creature enters, you may destroy target creature with toughness 2 or less.
 */
val FleshpulperGiant = card("Fleshpulper Giant") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, you may destroy target creature with toughness 2 or less."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter(GameObjectFilter.Creature.toughnessAtMost(2)))
        effect = Effects.May(Effects.Destroy(creature))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "140"
        artist = "Alex Horley-Orlandelli"
        flavorText = "\"He doesn't hate small folk. He just likes the squishing sound they make.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2726d3c-c182-4d8a-a723-0de2c5c4b152.jpg?1783939913"
    }
}
