package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Chain of Vapor
 * {U}
 * Instant
 * Return target nonland permanent to its owner's hand. Then that permanent's controller
 * may sacrifice a land of their choice. If the player does, they may copy this spell
 * and may choose a new target for that copy.
 */
val ChainOfVapor = card("Chain of Vapor") {
    manaCost = "{U}"
    typeLine = "Instant"
    oracleText = "Return target nonland permanent to its owner's hand. Then that permanent's controller may sacrifice a land of their choice. If the player does, they may copy this spell and may choose a new target for that copy."

    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter.NonlandPermanent))
        effect = Effects.BounceAndChainCopy(
            target = t,
            targetFilter = TargetFilter.NonlandPermanent,
            spellName = "Chain of Vapor"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30f6b4a2-5780-46e9-b239-459d2cf37743.jpg?1562395835"
    }
}
