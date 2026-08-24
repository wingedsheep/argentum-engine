package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Hand of Justice
 * {5}{W}
 * Creature — Avatar
 * 2/6
 * {T}, Tap three untapped white creatures you control: Destroy target creature.
 *
 * The three tapped creatures exclude Hand of Justice itself: the {T} symbol in the same cost
 * already taps it, and an already-tapped permanent can't be tapped again to pay a cost (CR 107.5).
 */
val HandOfJustice = card("Hand of Justice") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Avatar"
    oracleText = "{T}, Tap three untapped white creatures you control: Destroy target creature."
    power = 2
    toughness = 6

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapPermanents(
                count = 3,
                filter = GameObjectFilter.Creature.withColor(Color.WHITE).untapped().youControl(),
                excludeSelf = true
            )
        )
        val t = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.Destroy(t)
        description = "{T}, Tap three untapped white creatures you control: Destroy target creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "5"
        artist = "Melissa A. Benson"
        flavorText = "\"The Hand of Justice will come to cleanse the world if we are true.\"\n—Oliver Farrel"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a899b2d-825c-4929-a769-f4df70bf6a17.jpg?1783947920"
    }
}
