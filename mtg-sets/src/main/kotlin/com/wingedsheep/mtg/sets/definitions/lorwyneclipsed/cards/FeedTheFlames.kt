package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect

/**
 * Feed the Flames
 * {3}{R}
 * Instant
 *
 * Feed the Flames deals 5 damage to target creature. If that creature would die
 * this turn, exile it instead.
 */
val FeedTheFlames = card("Feed the Flames") {
    manaCost = "{3}{R}"
    typeLine = "Instant"
    oracleText = "Feed the Flames deals 5 damage to target creature. " +
        "If that creature would die this turn, exile it instead."

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = MarkExileOnDeathEffect(creature)
            .then(Effects.DealDamage(5, creature))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Xabi Gaztelua"
        flavorText = "Fire is always hungry and never picky."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59740755-c353-4b6c-a84c-3b76133ce3ec.jpg?1767957151"
    }
}
