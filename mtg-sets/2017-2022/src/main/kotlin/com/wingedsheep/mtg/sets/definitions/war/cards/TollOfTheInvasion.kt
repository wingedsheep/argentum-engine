package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Toll of the Invasion
 * {2}{B}
 * Sorcery
 * Target opponent reveals their hand. You choose a nonland card from it. That player discards that card.
 * Amass Zombies 1.
 *
 * You still amass 1 even if that player has no nonland cards to discard (2019-05-03 ruling).
 */
val TollOfTheInvasion = card("Toll of the Invasion") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it. That player discards that card.\n" +
        "Amass Zombies 1. (Put a +1/+1 counter on an Army you control. It's also a Zombie. " +
        "If you don't control an Army, create a 0/0 black Zombie Army creature token first.)"

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
            run(Effects.Amass(1, "Zombie"))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "Joe Slucher"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/4471c2a5-aa2c-47e2-8238-1eb366c8adc9.jpg?1783933438"
        ruling("2019-05-03", "You still amass 1 even if that player has no nonland cards to discard.")
        ruling(
            "2023-06-16",
            "Some spells and abilities that amass Zombies may require targets. If each target chosen is an illegal target as that spell or ability tries to resolve, it won't resolve. You won't amass Zombies."
        )
    }
}
