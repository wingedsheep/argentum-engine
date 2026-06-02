package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.dsl.Costs

/**
 * Vaporous Djinn
 * {2}{U}{U}
 * Creature — Djinn
 * 3/4
 * Flying
 * At the beginning of your upkeep, this creature phases out unless you pay {U}{U}.
 *
 * "Phases out" uses the phasing mechanic (Rule 702.26): if the controller declines to
 * pay, the Djinn is treated as though it doesn't exist until it phases back in before
 * the controller's next untap step.
 */
val VaporousDjinn = card("Vaporous Djinn") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Djinn"
    power = 3
    toughness = 4
    oracleText = "Flying\nAt the beginning of your upkeep, this creature phases out unless you pay {U}{U}."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = PayOrSufferEffect(
            cost = Costs.pay.Mana(ManaCost.parse("{U}{U}")),
            suffer = Effects.PhaseOut()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Adam Rex"
        flavorText = "\"What is taking them so long to fill the waterskins?\"\n—Mwani, Mtenda goatherd"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7ea65e2-68d8-429f-9be7-e6e5e12a2a4d.jpg?1562722388"
    }
}
