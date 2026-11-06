package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Lay Bare the Heart — Amonkhet #96 (canonical printing)
 * {1}{B} · Sorcery
 *
 * Target opponent reveals their hand. You choose a nonlegendary, nonland card from it.
 * That player discards that card.
 */
val LayBareTheHeart = card("Lay Bare the Heart") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonlegendary, nonland card from it. " +
        "That player discards that card."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = opponentHand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland.nonlegendary(),
                prompt = "Choose a nonlegendary, nonland card to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            discard(toDiscard, opponent.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Karl Kopinski"
        flavorText = "\"True intentions lie not in the head but in the heart.\"\n—Bontu, god of ambition"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4b76ff8-b26c-4567-9339-bb6a37713f4b.jpg?1783936505"
    }
}
