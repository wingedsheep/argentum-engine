package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Tower Worker
 * {3}
 * Artifact Creature — Assembly-Worker
 * 1/3
 * Reach
 * {T}: Add {C}. If you control creatures named Mine Worker and Power Plant Worker, add {C}{C}{C} instead.
 *
 * The creature-world echo of Urza's Tower: the same `If` over two `YouControl` checks, filtered to
 * *creatures* with the companion names.
 */
val TowerWorker = card("Tower Worker") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Assembly-Worker"
    power = 1
    toughness = 3
    oracleText = "Reach\n" +
        "{T}: Add {C}. If you control creatures named Mine Worker and Power Plant Worker, add {C}{C}{C} instead."

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.If(
            condition = Conditions.All(
                Conditions.YouControl(GameObjectFilter.Creature.named("Mine Worker")),
                Conditions.YouControl(GameObjectFilter.Creature.named("Power Plant Worker"))
            ),
            then = Effects.AddColorlessMana(3),
            otherwise = Effects.AddColorlessMana(1)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {C}. If you control creatures named Mine Worker and Power Plant Worker, add {C}{C}{C} instead."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "255"
        artist = "Gaboleps"
        flavorText = "Even Urza's most mundane creations shared his obsession with perfection."
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f31bd134-22c8-4031-8f85-ac0855914d11.jpg?1783920010"
    }
}
