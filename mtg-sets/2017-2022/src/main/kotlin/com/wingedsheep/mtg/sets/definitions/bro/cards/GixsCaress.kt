package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Gix's Caress
 * {2}{B}
 * Sorcery
 *
 * Target opponent reveals their hand. You choose a nonland card from it.
 * That player discards that card.
 * Create a tapped Powerstone token.
 */
val GixsCaress = card("Gix's Caress") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it. That player discards that card.\n" +
        "Create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = opponentHand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            discard(toDiscard, opponent.asPlayer)
            run(Effects.CreatePowerstone(1, tapped = true))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Ignatius Budi"
        flavorText = "\"Show me all the delicious misery you've sown.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1140cb9-6c48-4054-966e-5cc02aa7d5a4.jpg?1783920089"
    }
}
