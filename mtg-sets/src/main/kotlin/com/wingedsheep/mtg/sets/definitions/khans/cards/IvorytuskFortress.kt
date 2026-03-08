package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.UntapFilteredDuringOtherUntapSteps

/**
 * Ivorytusk Fortress
 * {2}{W}{B}{G}
 * Creature — Elephant
 * 5/7
 * Untap each creature you control with a +1/+1 counter on it during each other player's untap step.
 */
val IvorytuskFortress = card("Ivorytusk Fortress") {
    manaCost = "{2}{W}{B}{G}"
    typeLine = "Creature — Elephant"
    power = 5
    toughness = 7
    oracleText = "Untap each creature you control with a +1/+1 counter on it during each other player's untap step."

    staticAbility {
        ability = UntapFilteredDuringOtherUntapSteps(
            filter = Filters.Creature.withCounter("+1/+1")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Jasper Sandner"
        flavorText = "Abzan soldiers march to war confident that their Houses march with them."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75a8df73-0141-4c07-87d8-b1f34a4b374b.jpg?1562788730"
    }
}
