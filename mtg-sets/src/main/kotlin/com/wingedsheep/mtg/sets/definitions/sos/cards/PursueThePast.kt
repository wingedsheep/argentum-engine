package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Pursue the Past
 * {R}{W}
 * Sorcery
 *
 * You gain 2 life. You may discard a card. If you do, draw two cards.
 * Flashback {2}{R}{W}
 *
 * Loot effect: the unconditional life gain resolves first, then the optional "may discard a
 * card. If you do, draw two cards" is the standard MayEffect → IfYouDo loot shape. Flashback is
 * a keyword ability so the spell can be re-cast from the graveyard for {2}{R}{W}.
 */
val PursueThePast = card("Pursue the Past") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Sorcery"
    oracleText = "You gain 2 life. You may discard a card. If you do, draw two cards.\n" +
        "Flashback {2}{R}{W} (You may cast this card from your graveyard for its flashback cost. " +
        "Then exile it.)"

    spell {
        effect = Effects.GainLife(2)
            .then(
                MayEffect(
                    effect = IfYouDoEffect(
                        action = Patterns.Hand.discardCards(1),
                        ifYouDo = Effects.DrawCards(2),
                    ),
                    descriptionOverride = "You may discard a card. If you do, draw two cards.",
                ),
            )
    }

    keywordAbility(KeywordAbility.flashback("{2}{R}{W}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "216"
        artist = "Craig Elliott"
        flavorText = "\"Stones in your path are not obstacles, but discoveries.\"\n—Velomachus Lorehold"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/4584d5f7-b1f1-4c8e-80c5-ad35e44a968e.jpg?1775938502"
    }
}
