package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Prying Questions — Eldritch Moon #101
 * {2}{B} · Sorcery
 *
 * Target opponent loses 3 life and puts a card from their hand on top of their library.
 *
 * Life loss, then Chittering Rats' Gather → Select → Move tuck: the targeted opponent
 * (`Chooser.TargetPlayer`) picks the card, no reveal, and it goes on top of their own library.
 * An empty hand gathers nothing — the opponent still loses 3 life (ruling 2016-07-13).
 */
val PryingQuestions = card("Prying Questions") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent loses 3 life and puts a card from their hand on top of their library."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(3, opponent) then Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val tucked = chooseExactly(
                1,
                from = hand,
                chooser = Chooser.TargetPlayer,
                prompt = "Choose a card to put on top of your library"
            )
            toLibraryTop(tucked, opponent.asPlayer, order = CardOrder.Preserve)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "David Palumbo"
        flavorText = "What remained of the Lunarch Inquisition found new ways to extract confessions."
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac9fcf7c-8785-4285-b583-7060edabc3c6.jpg?1783937475"
        ruling("2016-07-13", "Prying Questions can target an opponent who has no cards in hand. That player just loses 3 life.")
    }
}
