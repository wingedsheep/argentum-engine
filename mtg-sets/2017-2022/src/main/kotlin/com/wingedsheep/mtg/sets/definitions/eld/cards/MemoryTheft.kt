package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Memory Theft
 * {2}{B}
 * Sorcery
 * Target opponent reveals their hand. You choose a nonland card from it. That player discards that
 * card. You may put a card that has an Adventure that player owns from exile into that player's
 * graveyard.
 *
 * The Distress pipeline, then an optional pick among the face-up exiled cards that player owns with
 * [CardPredicate.HasAdventure] — a layout characteristic, so any adventurer card qualifies, not only
 * one that went on an adventure (2019-10-04 ruling). The exile pick happens even when the hand held
 * no nonland card.
 */
val MemoryTheft = card("Memory Theft") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it. That player discards " +
        "that card. You may put a card that has an Adventure that player owns from exile into that player's " +
        "graveyard."

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

            val exiled = gather(
                CardSource.FromZone(
                    Zone.EXILE,
                    opponent.asPlayer,
                    GameObjectFilter.Any.withCardPredicate(CardPredicate.HasAdventure).faceUp()
                )
            )
            val toGraveyard = chooseUpTo(
                1,
                from = exiled,
                chooser = Chooser.Controller,
                prompt = "You may put a card with an Adventure from exile into its owner's graveyard"
            )
            move(toGraveyard, CardDestination.ToZone(Zone.GRAVEYARD, opponent.asPlayer))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Micah Epstein"
        flavorText = "In the wilds, the ravens steal far more than baubles."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/397fd49b-a520-4b0e-9ab9-71675ab5969d.jpg?1783932634"
        ruling(
            "2019-10-04",
            "An effect may refer to a card, spell, or permanent that \"has an Adventure.\" This refers to a card, " +
                "spell, or permanent that has an adventurer card's set of alternative characteristics, even if " +
                "they're not being used and even if that card was never cast as an Adventure."
        )
        ruling(
            "2019-10-04",
            "If an effect refers to a card, spell, or permanent that has an Adventure, it won't find an instant " +
                "or sorcery spell on the stack that's been cast as an Adventure."
        )
        ruling(
            "2019-10-04",
            "You choose which exiled adventurer card, if any, to put into that player's graveyard after seeing " +
                "their hand. You may do this even if that player has no nonland cards in hand, or even no cards at all."
        )
        ruling(
            "2019-10-04",
            "Memory Theft allows you to put any face-up exiled card with an Adventure that player owns into their " +
                "graveyard, not just one that was cast as an Adventure and exiled."
        )
    }
}
