package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked

/**
 * Dauntless Unity
 * {1}{W}
 * Instant
 * Kicker {1}{W}
 * Creatures you control get +1/+1 until end of turn. If this spell was kicked, those creatures
 * get +2/+1 until end of turn instead.
 */
val DauntlessUnity = card("Dauntless Unity") {
    manaCost = "{1}{W}"
    typeLine = "Instant"
    oracleText = "Kicker {1}{W} (You may pay an additional {1}{W} as you cast this spell.)\n" +
        "Creatures you control get +1/+1 until end of turn. If this spell was kicked, " +
        "those creatures get +2/+1 until end of turn instead."

    keywordAbility(KeywordAbility.kicker("{1}{W}"))

    spell {
        effect = Effects.If(
            condition = WasKicked,
            then = Patterns.Group.modifyStatsForAll(2, 1, Filters.Group.creaturesYouControl),
            otherwise = Patterns.Group.modifyStatsForAll(1, 1, Filters.Group.creaturesYouControl)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b12a4d17-68e6-4133-99fd-e501e24e6c6b.jpg?1783929421"
    }
}
