package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Grasping Scoundrel
 * {B}
 * Creature — Human Pirate
 * 1/1
 * This creature gets +1/+0 as long as it's attacking.
 */
val GraspingScoundrel = card("Grasping Scoundrel") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Pirate"
    power = 1
    toughness = 1
    oracleText = "This creature gets +1/+0 as long as it's attacking."

    staticAbility {
        ability = ModifyStats(1, 0, GroupFilter.source())
        condition = Conditions.SourceIsAttacking
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Steve Prescott"
        flavorText = "\"I can hear the plunder calling. It asks for me by name.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f6da2e7-4cf0-49ae-b0ba-3b994ff21587.jpg?1783935310"
    }
}
