package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlaneswalker

/**
 * Flame-Blessed Bolt
 * {R}
 * Instant
 * Flame-Blessed Bolt deals 2 damage to target creature or planeswalker. If that creature or
 * planeswalker would die this turn, exile it instead.
 */
val FlameBlessedBolt = card("Flame-Blessed Bolt") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Flame-Blessed Bolt deals 2 damage to target creature or planeswalker. If that creature or planeswalker would die this turn, exile it instead."
    spell {
        val t = target("target", TargetCreatureOrPlaneswalker())
        effect = Effects.Composite(
            DealDamageEffect(2, t),
            MarkExileOnDeathEffect(t)
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Andreas Zafiratos"
        flavorText = "\"I noticed you were short on party favors, so I brought my own.\"\n—Higa, slayer-captain of Gatstaf"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1771a8f-7bea-4bb0-9949-566ee6613b93.jpg?1782703077"
    }
}
