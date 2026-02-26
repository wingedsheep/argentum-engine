package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Covert Operative
 * {4}{U}
 * Creature — Human Wizard
 * 3/2
 * Covert Operative can't be blocked.
 */
val CovertOperative = card("Covert Operative") {
    manaCost = "{4}{U}"
    typeLine = "Creature — Human Wizard"
    power = 3
    toughness = 2

    flags(AbilityFlag.CANT_BE_BLOCKED)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Kev Walker"
        flavorText = "Some spies seek clarity. Others seek transparency."
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbda6799-3b55-4714-8305-713e1e198a15.jpg?1562939217"
    }
}
