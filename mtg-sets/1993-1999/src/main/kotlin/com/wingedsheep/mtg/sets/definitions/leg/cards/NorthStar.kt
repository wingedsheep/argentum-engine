package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * North Star
 * {4}
 * Artifact
 *
 * {4}, {T}: For one spell this turn, you may spend mana as though it were mana of any type to pay
 * that spell's mana cost. (Additional costs are still paid normally.)
 *
 * The rider waits on the state until the first spell this turn whose mana cost can't be paid
 * without it, so a spell paid with the right types first doesn't use it up.
 */
val NorthStar = card("North Star") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "{4}, {T}: For one spell this turn, you may spend mana as though it were mana of any " +
        "type to pay that spell's mana cost. (Additional costs are still paid normally.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.SpendAnyManaTypeForOneSpell()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "288"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daac2a6b-27c8-4567-9e0c-7b262628d331.jpg?1783948026"
    }
}
