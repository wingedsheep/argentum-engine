package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

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
        collectorNumber = "230"
        artist = "Doug Chaffee"
        flavorText = "\"I like goblins. They make funny little popping sounds when they die.\"\n—Braids, dementia summoner"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb545dcd-3a7a-46a7-9c35-d28faebc6d17.jpg?1562951033"
    }
}
