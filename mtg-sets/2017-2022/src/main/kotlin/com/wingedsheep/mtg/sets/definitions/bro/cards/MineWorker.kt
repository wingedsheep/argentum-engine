package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Mine Worker
 * {2}
 * Artifact Creature — Assembly-Worker
 * 2/1
 * {T}: You gain 1 life. If you control creatures named Power Plant Worker and Tower Worker,
 * you gain 3 life instead.
 */
val MineWorker = card("Mine Worker") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Assembly-Worker"
    power = 2
    toughness = 1
    oracleText = "{T}: You gain 1 life. If you control creatures named Power Plant Worker and Tower Worker, " +
        "you gain 3 life instead."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.If(
            condition = Conditions.All(
                Conditions.YouControl(GameObjectFilter.Creature.named("Power Plant Worker")),
                Conditions.YouControl(GameObjectFilter.Creature.named("Tower Worker"))
            ),
            then = Effects.GainLife(3),
            otherwise = Effects.GainLife(1)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "239"
        artist = "Raoul Vitale"
        flavorText = "Following his hunger for knowledge and power from the past, Urza starved Dominaria's future."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5aa63321-aa28-449d-a31f-869a98c5a6be.jpg?1783920018"
    }
}
