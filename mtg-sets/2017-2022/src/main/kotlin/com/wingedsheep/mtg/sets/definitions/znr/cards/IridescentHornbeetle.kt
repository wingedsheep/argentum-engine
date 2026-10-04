package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Iridescent Hornbeetle
 * {4}{G}
 * Creature — Insect
 * 3/4
 * At the beginning of your end step, create a 1/1 green Insect creature token for each +1/+1
 * counter you've put on creatures under your control this turn.
 *
 * The count is turn history ([DynamicAmounts.plusOneCountersPutOnYourCreaturesThisTurn]): counters
 * put before the Hornbeetle arrived, counters a creature entered with, and counters on creatures
 * that have since left all count.
 */
val IridescentHornbeetle = card("Iridescent Hornbeetle") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    power = 3
    toughness = 4
    oracleText = "At the beginning of your end step, create a 1/1 green Insect creature token for " +
        "each +1/+1 counter you've put on creatures under your control this turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.CreateToken(
            count = DynamicAmounts.plusOneCountersPutOnYourCreaturesThisTurn(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect")
        )
        description = "At the beginning of your end step, create a 1/1 green Insect creature token " +
            "for each +1/+1 counter you've put on creatures under your control this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Simon Dominic"
        flavorText = "The hatchlings will fill the jungles of Murasa in a glittering tide."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/214ef641-b08c-42d0-94a5-3054fa7fcebc.jpg?1783929341"
    }
}
