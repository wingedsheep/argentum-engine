package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity

/**
 * Scrapwork Mutt
 * {2}
 * Artifact Creature — Dog
 * 2/1
 * When this creature enters, you may discard a card. If you do, draw a card.
 * Unearth {1}{R}
 *
 * The optional rummage is Immersturm Raider's spelling: [Effects.May] around [Effects.IfYouDo].
 */
val ScrapworkMutt = card("Scrapwork Mutt") {
    manaCost = "{2}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Dog"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, you may discard a card. If you do, draw a card.\n" +
        "Unearth {1}{R} ({1}{R}: Return this card from your graveyard to the battlefield. It gains haste. Exile it at " +
        "the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.IfYouDo(Patterns.Hand.discardCards(1), Effects.DrawCards(1))
        )
    }

    unearth("{1}{R}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Sidharth Chaturvedi"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/4742800a-4872-4c2d-b884-01e0ba16950c.jpg?1783920056"
    }
}
