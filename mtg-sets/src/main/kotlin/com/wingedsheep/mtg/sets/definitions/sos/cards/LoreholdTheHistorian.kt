package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantMiracleToCardsInHand
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Lorehold, the Historian
 * {3}{R}{W}
 * Legendary Creature — Elder Dragon
 * 5/5
 *
 * Flying, haste
 * Each instant and sorcery card in your hand has miracle {2}. (You may cast a card for its
 * miracle cost when you draw it if it's the first card you drew this turn.)
 * At the beginning of each opponent's upkeep, you may discard a card. If you do, draw a card.
 *
 * The miracle grant is a [GrantMiracleToCardsInHand]; when one of your instant/sorcery cards is
 * the first card you draw in a turn, the engine opens its miracle window so you may cast it for
 * {2} that turn.
 */
val LoreholdTheHistorian = card("Lorehold, the Historian") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 5
    toughness = 5
    oracleText = "Flying, haste\n" +
        "Each instant and sorcery card in your hand has miracle {2}. (You may cast a card for its " +
        "miracle cost when you draw it if it's the first card you drew this turn.)\n" +
        "At the beginning of each opponent's upkeep, you may discard a card. If you do, draw a card."

    keywords(Keyword.FLYING, Keyword.HASTE)

    staticAbility {
        ability = GrantMiracleToCardsInHand(
            filter = GameObjectFilter.InstantOrSorcery,
            cost = ManaCost.parse("{2}")
        )
    }

    // At the beginning of each opponent's upkeep, you may discard a card. If you do, draw a card.
    triggeredAbility {
        trigger = Triggers.EachOpponentUpkeep
        effect = MayEffect(
            IfYouDoEffect(
                action = Patterns.Hand.discardCards(1),
                ifYouDo = Effects.DrawCards(1)
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "201"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71a6701f-40f1-43ef-bff5-a5907fd67cd6.jpg?1775938396"
    }
}
