package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ModifyStatsEffect

/**
 * Goblin Sledder
 * {R}
 * Creature — Goblin
 * 1/1
 * Sacrifice a Goblin: Target creature gets +1/+1 until end of turn.
 */
val GoblinSledder = card("Goblin Sledder") {
    manaCost = "{R}"
    typeLine = "Creature — Goblin"
    power = 1
    toughness = 1

    activatedAbility {
        cost = AbilityCost.Sacrifice(CardFilter.HasSubtype("Goblin"))
        target = Targets.Creature
        effect = ModifyStatsEffect(
            powerModifier = 1,
            toughnessModifier = 1,
            target = EffectTarget.ContextTarget(0),
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "209"
        artist = "Ron Spencer"
        flavorText = "\"Let's play 'sled.' Here's how it works: you're the sled.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12af10e9-19b7-4177-b556-a446f2788da7.jpg"
    }
}
