package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost

/**
 * Rix Maadi Reveler
 * {1}{R}
 * Creature — Human Shaman
 * 2/2
 * Spectacle {2}{B}{R} (You may cast this spell for its spectacle cost rather than its mana cost if
 * an opponent lost life this turn.)
 * When this creature enters, discard a card, then draw a card. If this creature's spectacle cost
 * was paid, instead discard your hand, then draw three cards.
 *
 * Spectacle is a [SelfAlternativeCost] gated on [Conditions.OpponentLostLifeThisTurn] (as Blade
 * Juggler). The engine stamps "its own alternative cost was paid" onto the permanent the spell
 * becomes, so [Conditions.SelfAlternativeCostWasPaid] picks between the two branches on resolution.
 */
val RixMaadiReveler = card("Rix Maadi Reveler") {
    manaCost = "{1}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Human Shaman"
    oracleText = "Spectacle {2}{B}{R} (You may cast this spell for its spectacle cost rather than its mana cost " +
        "if an opponent lost life this turn.)\n" +
        "When this creature enters, discard a card, then draw a card. If this creature's spectacle cost was paid, " +
        "instead discard your hand, then draw three cards."
    power = 2
    toughness = 2

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{2}{B}{R}"),
        condition = Conditions.OpponentLostLifeThisTurn,
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(
            Conditions.SelfAlternativeCostWasPaid,
            then = Patterns.Hand.discardHand() then Effects.DrawCards(3),
            otherwise = Effects.Discard(1) then Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Sara Winters"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c40396da-18e2-42ca-a78c-4838a38f68b8.jpg"
    }
}
