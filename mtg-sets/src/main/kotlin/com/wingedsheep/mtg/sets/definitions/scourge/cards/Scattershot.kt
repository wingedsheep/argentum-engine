package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Scattershot
 * {2}{R}
 * Instant
 * Scattershot deals 1 damage to target creature.
 * Storm (When you cast this spell, copy it for each spell cast before it this turn.
 * You may choose new targets for the copies.)
 */
val Scattershot = card("Scattershot") {
    manaCost = "{2}{R}"
    typeLine = "Instant"
    oracleText = "Scattershot deals 1 damage to target creature.\nStorm (When you cast this spell, copy it for each spell cast before it this turn. You may choose new targets for the copies.)"

    spell {
        val t = target("target creature", Targets.Creature)
        effect = Effects.DealDamage(1, t)
    }

    keywords(Keyword.STORM)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Glen Angus"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf22f3e7-1626-4bab-9f62-7d4774704395.jpg?1562534789"
    }
}
