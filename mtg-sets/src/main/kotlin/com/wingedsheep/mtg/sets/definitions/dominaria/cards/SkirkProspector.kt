package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaEffect

/**
 * Skirk Prospector
 * {R}
 * Creature — Goblin
 * 1/1
 * Sacrifice a Goblin: Add {R}.
 */
val SkirkProspector = card("Skirk Prospector") {
    manaCost = "{R}"
    typeLine = "Creature — Goblin"
    power = 1
    toughness = 1
    oracleText = "Sacrifice a Goblin: Add {R}."

    activatedAbility {
        cost = AbilityCost.Sacrifice(GameObjectFilter.Creature.withSubtype("Goblin"))
        effect = AddManaEffect(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Slawomir Maniak"
        flavorText = "\"Deep beneath the ruined continent of Otaria, there's a mine where goblins still work, ignorant of the destruction above.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/6/1636d138-aa63-476f-a930-41b1be988032.jpg?1562731846"
    }
}
