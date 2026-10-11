package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Niv-Mizzet, Parun
 * {U}{U}{U}{R}{R}{R}
 * Legendary Creature — Dragon Wizard
 * 5/5
 * This spell can't be countered.
 * Flying
 * Whenever you draw a card, Niv-Mizzet deals 1 damage to any target.
 * Whenever a player casts an instant or sorcery spell, you draw a card.
 *
 * `Triggers.you.draws()` fires once per card drawn, so a multi-card draw yields that many pings.
 * The cast trigger fires on *cast* (`Triggers.anyPlayer.casts`), so its draw — and the ping it
 * causes — resolve before the spell, even if that spell is later countered.
 */
val NivMizzetParun = card("Niv-Mizzet, Parun") {
    manaCost = "{U}{U}{U}{R}{R}{R}"
    colorIdentity = "RU"
    typeLine = "Legendary Creature — Dragon Wizard"
    power = 5
    toughness = 5
    oracleText = "This spell can't be countered.\n" +
        "Flying\n" +
        "Whenever you draw a card, Niv-Mizzet deals 1 damage to any target.\n" +
        "Whenever a player casts an instant or sorcery spell, you draw a card."

    cantBeCountered = true

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.draws()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "192"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f3d2dc5-7b9d-4af6-9f3b-4de90fbf63c9.jpg?1783934125"
        ruling(
            "2018-10-05",
            "If an effect instructs you to draw multiple cards, Niv-Mizzet's first triggered ability triggers that many times. You choose targets for those abilities after you've drawn all of the cards.",
        )
        ruling(
            "2018-10-05",
            "Niv-Mizzet's second triggered ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered.",
        )
    }
}
