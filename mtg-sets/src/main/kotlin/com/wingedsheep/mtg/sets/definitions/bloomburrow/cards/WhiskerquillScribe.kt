package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Whiskerquill Scribe
 * {1}{R}
 * Creature — Mouse Citizen
 * 2/2
 * Valiant — Whenever this creature becomes the target of a spell or ability you control
 * for the first time each turn, you may discard a card. If you do, draw a card.
 */
val WhiskerquillScribe = card("Whiskerquill Scribe") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Mouse Citizen"
    oracleText = "Valiant — Whenever this creature becomes the target of a spell or ability you control for the first time each turn, you may discard a card. If you do, draw a card."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.Valiant
        effect = MayEffect(
            effect = EffectPatterns.discardCards(1)
                .then(Effects.DrawCards(1)),
            description_override = "You may discard a card. If you do, draw a card."
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Matt Stewart"
        flavorText = "\"Unneeded words are like dandelion seeds—cast adrift at the slightest whiff of scrutiny, only to find purchase and thrive where you least expect.\"\n—Myrtle, Valley scribe"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da653996-9bd4-40bd-afb4-48c7e070a269.jpg?1721431429"
    }
}
