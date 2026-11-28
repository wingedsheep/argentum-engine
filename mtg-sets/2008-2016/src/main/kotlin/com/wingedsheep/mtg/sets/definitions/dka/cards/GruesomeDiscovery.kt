package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType

/**
 * Gruesome Discovery
 * {2}{B}{B}
 * Sorcery
 * Target player discards two cards.
 * Morbid — If a creature died this turn, instead that player reveals their hand, you choose two
 * cards from it, then that player discards those cards.
 */
val GruesomeDiscovery = card("Gruesome Discovery") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player discards two cards.\nMorbid — If a creature died this turn, instead that player reveals their hand, you choose two cards from it, then that player discards those cards."

    spell {
        val player = target(Targets.Player)
        effect = Effects.If(
            condition = Conditions.CreatureDiedThisTurn,
            then = Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, player.asPlayer), revealed = true)
                val discarded = chooseExactly(2, hand, prompt = "Choose two cards for that player to discard")
                move(
                    discarded, CardDestination.ToZone(Zone.GRAVEYARD, player.asPlayer),
                    moveType = MoveType.Discard
                )
            },
            otherwise = Patterns.Hand.discardCards(2, player)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "John Stanko"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0a9d733-24b6-49ed-a15a-c00285eea4b2.jpg?1783940829"
    }
}
