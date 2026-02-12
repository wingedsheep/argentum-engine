package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LookAtTopAndReorderEffect

/**
 * Aven Fateshaper
 * {6}{U}
 * Creature — Bird Wizard
 * 4/5
 * Flying
 * When Aven Fateshaper enters the battlefield, look at the top four cards of your
 * library, then put them back in any order.
 * {4}{U}: Look at the top four cards of your library, then put them back in any order.
 */
val AvenFateshaper = card("Aven Fateshaper") {
    manaCost = "{6}{U}"
    typeLine = "Creature — Bird Wizard"
    power = 4
    toughness = 5
    oracleText = "Flying\nWhen Aven Fateshaper enters the battlefield, look at the top four cards of your library, then put them back in any order.\n{4}{U}: Look at the top four cards of your library, then put them back in any order."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LookAtTopAndReorderEffect(4)
    }

    activatedAbility {
        cost = Costs.Mana("{4}{U}")
        effect = LookAtTopAndReorderEffect(4)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a4b41c4-0d14-4b9c-8e0c-a626ba6b104d.jpg?1562923863"
    }
}
