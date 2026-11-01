package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost

/**
 * Rafter Demon
 * {2}{B}{R}
 * Creature — Demon
 * 4/2
 * Spectacle {3}{B}{R} (You may cast this spell for its spectacle cost rather than its mana cost if
 * an opponent lost life this turn.)
 * When this creature enters, if its spectacle cost was paid, each opponent discards a card.
 *
 * Spectacle is a [SelfAlternativeCost] gated on [Conditions.OpponentLostLifeThisTurn] (as Rix Maadi
 * Reveler); the engine stamps the paid flag onto the permanent, so the intervening "if" reads
 * [Conditions.SelfAlternativeCostWasPaid].
 */
val RafterDemon = card("Rafter Demon") {
    manaCost = "{2}{B}{R}"
    typeLine = "Creature — Demon"
    oracleText = "Spectacle {3}{B}{R} (You may cast this spell for its spectacle cost rather than its mana cost " +
        "if an opponent lost life this turn.)\n" +
        "When this creature enters, if its spectacle cost was paid, each opponent discards a card."
    power = 4
    toughness = 2

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{3}{B}{R}"),
        condition = Conditions.OpponentLostLifeThisTurn,
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.SelfAlternativeCostWasPaid
        effect = Effects.EachOpponentDiscards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "196"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf8dad4d-bc4f-46fb-892c-dbf6481cdc46.jpg"
    }
}
