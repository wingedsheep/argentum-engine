package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Futurist Forge
 * {1}{U}
 * Artifact
 *
 * When this artifact enters, draw a card.
 * {3}{U}, Sacrifice this artifact: Draw two cards.
 */
val FuturistForge = card("Futurist Forge") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, draw a card.\n" +
        "{3}{U}, Sacrifice this artifact: Draw two cards."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{U}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Arthur Yuan"
        flavorText = "\"You saw what I built in a cave. Imagine what I can do here.\"\n—Tony Stark"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a50feebf-c660-43f1-9fae-75294703b346.jpg?1783902959"
    }
}
