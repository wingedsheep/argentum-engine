package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Diplomacy of the Wastes
 * {2}{B}
 * Sorcery
 *
 * Target opponent reveals their hand. You choose a nonland card from it. That player discards
 * that card. If you control a Warrior, that player loses 2 life.
 *
 * The hand-strip is Gix's Caress's shape. The Warrior rider sits outside the gather/select/discard
 * chain and is checked at resolution, so per the 2014-11-24 ruling the opponent loses 2 life even
 * when there was nothing to discard.
 */
val DiplomacyOfTheWastes = card("Diplomacy of the Wastes") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it. " +
        "That player discards that card. If you control a Warrior, that player loses 2 life."

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
            run(
                Effects.If(
                    condition = Conditions.YouControl(GameObjectFilter.Permanent.withSubtype("Warrior")),
                    then = Effects.LoseLife(2, opponent)
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "Jack Wang"
        flavorText = "\"Our emissaries are gifted negotiators.\"\n—Alesha, Who Smiles at Death"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69faf63b-f39b-4005-a35d-1204faaa6d57.jpg?1783938698"
        ruling(
            "2014-11-24",
            "If you control a Warrior as Diplomacy of the Wastes resolves, the target opponent will lose 2 life " +
                "even if that player didn't discard a card (perhaps because they had no cards in hand).",
        )
    }
}
