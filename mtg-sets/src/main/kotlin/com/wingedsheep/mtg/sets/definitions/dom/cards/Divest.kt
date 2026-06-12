package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Zone
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
import com.wingedsheep.sdk.dsl.Effects

/**
 * Divest
 * {B}
 * Sorcery
 * Target player reveals their hand. You choose an artifact or creature card from it.
 * That player discards that card.
 */
val Divest = card("Divest") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player reveals their hand. You choose an artifact or creature card from it. That player discards that card."

    spell {
        val t = target("target", TargetOpponent())
        effect = Effects.Pipeline {
            run(RevealHandEffect(t))
            val hand = gather(
                CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                name = "hand"
            )
            val toDiscard = chooseExactly(
                1, from = hand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Artifact or GameObjectFilter.Creature,
                prompt = "Choose an artifact or creature card to discard",
                name = "toDiscard"
            )
            move(
                toDiscard,
                destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                moveType = MoveType.Discard
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Jesper Ejsing"
        flavorText = "\"Flittersprites collect unusually valuable things: coins of fallen empires, baby teeth, and memories of treasured names.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6619810-7933-4844-8556-2a575ed7f18a.jpg?1562742646"
    }
}
