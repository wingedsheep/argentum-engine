package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Mind Twist
 * {X}{B}
 * Sorcery
 * Target player discards X cards at random.
 *
 * Gather the target player's hand → the engine picks X of them at random
 * ([com.wingedsheep.sdk.scripting.effects.SelectionMode.Random], which caps at the hand size) →
 * discard. X greater than the hand discards the whole hand; X = 0 discards nothing.
 */
val MindTwist = card("Mind Twist") {
    manaCost = "{X}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player discards X cards at random."

    spell {
        val victim = target(Targets.Player)
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(zone = Zone.HAND, player = victim.asPlayer))
            val discarded = chooseRandom(DynamicAmounts.xValue(), from = hand)
            discard(discarded, victim.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Julie Baroh"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eee9e106-a248-49d2-b8c8-6bbcd56ce739.jpg?1783948693"
    }
}
