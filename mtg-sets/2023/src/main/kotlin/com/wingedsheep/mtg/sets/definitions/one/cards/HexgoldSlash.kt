package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hexgold Slash
 * {R}
 * Instant
 * Hexgold Slash deals 2 damage to target creature. If that creature has toxic, Hexgold Slash
 * deals 4 damage to that creature instead.
 *
 * "Has toxic" is `withKeyword(TOXIC)`, which matches any toxic N — printed or granted — off the
 * projected keyword, checked as the spell resolves.
 */
val HexgoldSlash = card("Hexgold Slash") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Hexgold Slash deals 2 damage to target creature. If that creature has toxic, " +
        "Hexgold Slash deals 4 damage to that creature instead."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(Filters.Creature.withKeyword(Keyword.TOXIC), t),
            then = Effects.DealDamage(4, t),
            otherwise = Effects.DealDamage(2, t)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Eli Minaya"
        flavorText = "\"No matter how tough they are, even Phyrexians prefer to keep their guts on the inside.\"\n—Jor Kadeen"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ec44465-68d4-4dca-a313-7f2ab0ce2e63.jpg?1783918029"
    }
}
