package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dark Deal
 * {2}{B}
 * Sorcery
 * Each player discards all the cards in their hand, then draws that many cards minus one.
 *
 * Per player, like Incendiary Command's wheel mode: each [Effects.ForEachPlayer] iteration gets
 * fresh stored collections, so [Patterns.Hand.discardedHand]'s count is that player's own hand
 * size. A player who discarded one card or none draws nothing (the count is floored at zero).
 */
val DarkDeal = card("Dark Deal") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each player discards all the cards in their hand, then draws that many cards minus one."

    spell {
        effect = Effects.ForEachPlayer(
            players = Player.Each,
            effects = listOf(
                Patterns.Hand.discardHand(),
                Effects.DrawCards(DynamicAmounts.nonNegative(Patterns.Hand.discardedHand.count - 1))
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "66"
        artist = "Scott Murphy"
        flavorText = "The first khans of the Sultai relied on the magic of the rakshasa to ensure the survival of the clan."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69dfaafe-2fbd-4f2f-913d-ea64e4ce44cd.jpg?1783938698"
        ruling("2014-11-24", "If a player had one card or no cards in their hand, that player won't draw any cards.")
    }
}
