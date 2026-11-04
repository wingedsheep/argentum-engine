package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Dark Inquiry
 * {2}{B}
 * Sorcery
 * Target opponent reveals their hand. You choose a nonland card from it. That player discards that card.
 *
 * The Duress pipeline (`usg/cards/Duress.kt`) with a nonland filter, as in Distress.
 */
val DarkInquiry = card("Dark Inquiry") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it. That player discards that card."

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
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "Naomi Baker"
        flavorText = "\"Your mind is no longer yours to waste.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42eaa634-0add-4e3d-9eee-5712598dda3f.jpg?1783935315"
    }
}
