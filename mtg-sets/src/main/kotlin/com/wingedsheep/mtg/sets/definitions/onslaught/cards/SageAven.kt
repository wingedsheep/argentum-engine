package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LookAtTopAndReorderEffect

/**
 * Sage Aven
 * {3}{U}
 * Creature — Bird Wizard
 * 1/3
 * Flying
 * When Sage Aven enters the battlefield, look at the top four cards of your
 * library, then put them back in any order.
 */
val SageAven = card("Sage Aven") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Bird Wizard"
    power = 1
    toughness = 3
    oracleText = "Flying\nWhen Sage Aven enters the battlefield, look at the top four cards of your library, then put them back in any order."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LookAtTopAndReorderEffect(4)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Randy Gallegos"
        flavorText = "From their mountain aeries, aven scholars see far more than the distant horizon."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c03afc5-7ca3-4ac6-a06e-091e2cce13a0.jpg?1562912764"
    }
}
