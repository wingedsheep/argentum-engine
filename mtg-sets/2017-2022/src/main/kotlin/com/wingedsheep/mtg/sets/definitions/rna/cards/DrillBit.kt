package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Drill Bit
 * {2}{B}
 * Sorcery
 * Spectacle {B} (You may cast this spell for its spectacle cost rather than its mana cost if an
 * opponent lost life this turn.)
 * Target player reveals their hand. You choose a nonland card from it. That player discards that card.
 *
 * The hand-strip is Thoughtseize's shape: reveal, gather the target's hand, the caster picks a
 * nonland card, and it moves as a real discard.
 */
val DrillBit = card("Drill Bit") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Spectacle {B} (You may cast this spell for its spectacle cost rather than its mana cost " +
        "if an opponent lost life this turn.)\n" +
        "Target player reveals their hand. You choose a nonland card from it. That player discards that card."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{B}"),
        condition = Conditions.OpponentLostLifeThisTurn,
    )

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(player))
            val revealedHand = gather(CardSource.FromZone(Zone.HAND, player.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = revealedHand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            discard(toDiscard, player.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Sidharth Chaturvedi"
        flavorText = "Never boring."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73f8710c-9c5e-4d11-b45f-0728c54bd631.jpg?1783933694"
    }
}
