package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Thought Erasure — Guilds of Ravnica #206 (canonical printing)
 * {U}{B} · Sorcery
 *
 * Target opponent reveals their hand. You choose a nonland card from it. That player discards that card.
 * Surveil 1.
 *
 * The Duress pipeline (`usg/cards/Duress.kt`) with a nonland filter, then `surveil(1)`. You surveil
 * even when the opponent has no nonland card to discard (2018-10-05 ruling).
 */
val ThoughtErasure = card("Thought Erasure") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it. That player discards that card.\n" +
        "Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = hand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            discard(toDiscard, opponent.asPlayer)
        } then Patterns.Library.surveil(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Anna Steinbauer"
        flavorText = "\"It's truly the highest honor, isn't it? To be remembered? Pity for you.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7ce226ad-cb1d-47f5-90fc-27704e181884.jpg?1783934119"
        ruling("2018-10-05", "You surveil 1 even if the opponent doesn't discard a card, perhaps because they had no cards in hand at all.")
        ruling("2024-01-12", "Some spells that instruct you to surveil require targets. You can't cast a spell without choosing legal targets. If all of those targets become illegal, the spell doesn't resolve and you won't surveil.")
    }
}
