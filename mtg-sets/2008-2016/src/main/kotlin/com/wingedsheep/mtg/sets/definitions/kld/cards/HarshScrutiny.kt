package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Harsh Scrutiny — Kaladesh #85
 * {B} · Sorcery
 *
 * Target opponent reveals their hand. You choose a creature card from it. That player discards
 * that card. Scry 1.
 *
 * Duress's reveal-choose-discard pipeline with a creature filter, followed by Scry 1. The scry
 * happens even if the opponent has no creature card to discard (2016-09-20 ruling).
 */
val HarshScrutiny = card("Harsh Scrutiny") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a creature card from it. " +
        "That player discards that card. Scry 1."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = opponentHand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Creature,
                prompt = "Choose a creature card to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            discard(toDiscard, opponent.asPlayer)
            run(Effects.Scry(1))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Vincent Proce"
        flavorText = "\"If you've done nothing wrong, you have nothing to fear.\"\n—Enforcer-Chief Ranaj"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe68b484-cc8d-4a0b-94c2-1fec9090dfcd.jpg?1783937205"
        ruling("2016-09-20", "You scry 1 even if that player has no creature card to discard.")
    }
}
