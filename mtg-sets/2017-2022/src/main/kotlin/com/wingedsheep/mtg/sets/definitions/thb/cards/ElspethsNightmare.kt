package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Elspeth's Nightmare
 * {2}{B}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Destroy target creature an opponent controls with power 2 or less.
 * II — Target opponent reveals their hand. You choose a noncreature, nonland card from it. That
 * player discards that card.
 * III — Exile target opponent's graveyard.
 *
 * Chapter II is Duress's reveal-and-choose pipeline; chapter III is Tormod's Crypt's
 * gather-the-graveyard-and-exile, aimed at a target opponent.
 */
val ElspethsNightmare = card("Elspeth's Nightmare") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Destroy target creature an opponent controls with power 2 or less.\n" +
        "II — Target opponent reveals their hand. You choose a noncreature, nonland card from it. " +
        "That player discards that card.\n" +
        "III — Exile target opponent's graveyard."

    sagaChapter(1) {
        val creature = target(TargetFilter(GameObjectFilter.Creature.opponentControls().powerAtMost(2)))
        effect = Effects.Destroy(creature)
    }

    sagaChapter(2) {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = opponentHand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Noncreature and GameObjectFilter.Nonland,
                prompt = "Choose a noncreature, nonland card to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            discard(toDiscard, opponent.asPlayer)
        }
    }

    sagaChapter(3) {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, opponent.asPlayer))
            exile(graveyard, opponent.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37ccd974-d2bc-4fcf-94a7-0a868a04cb98.jpg?1783931568"
    }
}
