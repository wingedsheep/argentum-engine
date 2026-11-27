package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Headless Skaab
 * {2}{U}
 * Creature — Zombie Warrior
 * 3/6
 * As an additional cost to cast this spell, exile a creature card from your graveyard.
 * This creature enters tapped.
 */
val HeadlessSkaab = card("Headless Skaab") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Warrior"
    power = 3
    toughness = 6
    oracleText = "As an additional cost to cast this spell, exile a creature card from your graveyard.\n" +
        "This creature enters tapped."

    additionalCost(Costs.additional.ExileCards(count = 1, filter = GameObjectFilter.Creature))

    replacementEffect(EntersTapped())

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Johann Bodin"
        flavorText = "A head is a needless encumbrance when muscle is all that's required."
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca63f9a2-381e-4c84-b0bb-3acd9445b4db.jpg"
    }
}
