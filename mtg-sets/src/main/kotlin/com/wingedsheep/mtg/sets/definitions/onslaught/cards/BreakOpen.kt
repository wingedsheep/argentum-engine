package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.scripting.TurnFaceUpEffect
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Break Open
 * {1}{R}
 * Instant
 * Turn target face-down creature an opponent controls face up.
 */
val BreakOpen = card("Break Open") {
    manaCost = "{1}{R}"
    typeLine = "Instant"
    oracleText = "Turn target face-down creature an opponent controls face up."

    spell {
        target = TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.faceDown().opponentControls())
        )
        effect = TurnFaceUpEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "190"
        artist = "Alex Horley-Orlandelli"
        flavorText = "There are two ways to resolve puzzling situations: thoughtful contemplation or force. After thoughtful contemplation, most barbarians choose force."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5ae8050-b644-41db-b1e9-d9bad2173485.jpg?1562934178"
    }
}
