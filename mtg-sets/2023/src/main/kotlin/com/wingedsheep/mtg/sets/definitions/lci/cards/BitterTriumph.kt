package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceSlot

/**
 * Bitter Triumph
 * {1}{B}
 * Instant
 *
 * As an additional cost to cast this spell, discard a card or pay 3 life.
 * Destroy target creature or planeswalker.
 */
val BitterTriumph = card("Bitter Triumph") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, discard a card or pay 3 life.\nDestroy target creature or planeswalker."

    additionalCost(Costs.additional.Choice(
        Costs.additional.DiscardCards(1),
        Costs.additional.PayLife(3),
        choiceSlot = ChoiceSlot.ADDITIONAL_COST_BRANCH,
    ))
    spell {
        val creatureOrPlaneswalker = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Destroy(creatureOrPlaneswalker)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Donato Giancola"
        flavorText = "Though he wept at Inti's death, Caparocti's eyes were keen, and his spear pierced cleanly through Clavileño's tainted heart."
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05bdd22c-3e11-4c29-bdfa-d3dfc0e90a9f.jpg?1699044085"
    }
}
