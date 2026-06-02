package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.dsl.Costs

/**
 * Junún Efreet
 * {1}{B}{B}
 * Creature — Efreet
 * 3/3
 * Flying
 * At the beginning of your upkeep, sacrifice this creature unless you pay {B}{B}.
 */
val JununEfreet = card("Junún Efreet") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Efreet"
    power = 3
    toughness = 3
    oracleText = "Flying\nAt the beginning of your upkeep, sacrifice this creature unless you pay {B}{B}."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = PayOrSufferEffect(
            cost = Costs.pay.Mana(ManaCost.parse("{B}{B}")),
            suffer = SacrificeSelfEffect,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f46783a-b91e-4829-a173-5515b09ca615.jpg?1562912566"
    }
}
