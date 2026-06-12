package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Duress
 * {B}
 * Sorcery
 *
 * Target opponent reveals their hand. You choose a noncreature, nonland card from it.
 * That player discards that card.
 */
val Duress = card("Duress") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a noncreature, nonland card from it. That player discards that card."

    spell {
        val opponent = target("target opponent", TargetOpponent())
        effect = Effects.Pipeline {
            run(RevealHandEffect(opponent))
            val opponentHand = gather(
                CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                name = "opponentHand"
            )
            val toDiscard = chooseExactly(
                1, from = opponentHand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Noncreature and GameObjectFilter.Nonland,
                prompt = "Choose a noncreature, nonland card to discard",
                alwaysPrompt = true,
                showAllCards = true,
                name = "toDiscard"
            )
            move(
                toDiscard,
                CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                moveType = MoveType.Discard
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Lawrence Snelly"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca367f49-0f4a-4b7f-8104-851893fbcd8a.jpg?1562937711"
    }
}
