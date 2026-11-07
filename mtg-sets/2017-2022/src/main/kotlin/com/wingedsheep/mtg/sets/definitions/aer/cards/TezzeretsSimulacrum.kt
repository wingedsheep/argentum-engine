package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Tezzeret's Simulacrum
 * {3}
 * Artifact Creature — Golem
 * 2/3
 * {T}: Target opponent loses 1 life. If you control a Tezzeret planeswalker, that player loses
 * 3 life instead.
 *
 * "Instead" changes the amount, not the event: one life loss whose amount is a
 * [DynamicAmounts.conditional], checked on resolution (the Jace's Triumph shape).
 */
val TezzeretsSimulacrum = card("Tezzeret's Simulacrum") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    oracleText = "{T}: Target opponent loses 1 life. If you control a Tezzeret planeswalker, that player loses 3 life instead."
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.Tap
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(
            DynamicAmounts.conditional(
                condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Tezzeret")),
                ifTrue = 3,
                ifFalse = 1
            ),
            opponent
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "Craig J Spearing"
        flavorText = "Tezzeret's command of living metal has nothing to do with intricate gearcraft or aether supplies."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7eacfeec-a341-45bc-bf95-e3df0094505a.jpg?1783936713"
        inBooster = false
    }
}
