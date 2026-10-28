package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Niv-Mizzet, the Firemind
 * {2}{U}{U}{R}{R}
 * Legendary Creature — Dragon Wizard
 * 4/4
 * Flying
 * Whenever you draw a card, Niv-Mizzet deals 1 damage to any target.
 * {T}: Draw a card.
 *
 * `Triggers.you.draws()` fires once per card drawn, so a multi-card draw yields that many pings.
 */
val NivMizzetTheFiremind = card("Niv-Mizzet, the Firemind") {
    manaCost = "{2}{U}{U}{R}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Dragon Wizard"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever you draw a card, Niv-Mizzet deals 1 damage to any target.\n" +
        "{T}: Draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.draws()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Todd Lockwood"
        flavorText = "\"(Z->)90° - (E-N²W)90°t = 1\""
        imageUri = "https://cards.scryfall.io/normal/front/5/9/5994d98a-090b-4203-b44f-b6517b6a83ea.jpg?1783943476"
        ruling("2018-03-16", "If a spell or ability causes you to draw multiple cards, Niv-Mizzet's ability triggers that many times.")
        ruling(
            "2018-03-16",
            "If dealing 1 damage to a permanent or player causes you to draw a card, perhaps because Niv-Mizzet " +
                "is enchanted by Curiosity, you'll continue this loop until the loop is interrupted, perhaps by a " +
                "player losing the game.",
        )
    }
}
