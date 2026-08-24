package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Inquisition
 * {2}{B}
 * Sorcery
 * Target player reveals their hand. Inquisition deals damage to that player equal to the number of
 * white cards in their hand.
 *
 * Baleful Stare's shape with the payoff pointed at the revealer instead of at you: reveal, then
 * count. The count is `DynamicAmount.Count` over the *target's* hand, so it is read at resolution
 * from the same hand the reveal just showed — nothing moves in between.
 *
 * "White cards", not white creatures and not Plains: a colour test on the card, which catches a
 * white artifact or a white land the same way the printed card does.
 */
val Inquisition = card("Inquisition") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player reveals their hand. Inquisition deals damage to that player equal " +
        "to the number of white cards in their hand."

    spell {
        val victim = target("target player", TargetPlayer())
        effect = Effects.Composite(
            RevealHandEffect(victim),
            Effects.DealDamage(
                DynamicAmount.Count(
                    Player.ContextPlayer(0),
                    Zone.HAND,
                    GameObjectFilter.Any.withColor(Color.WHITE),
                ),
                victim,
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "Anson Maddocks"
        flavorText = "Many of those entrusted to Primata Delphine's care tended to express " +
            "themselves with screams."
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f133f06-6398-4db1-8577-66c16fd3e00d.jpg?1783947939"
    }
}
