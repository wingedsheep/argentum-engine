package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Colossal Growth
 * {1}{G}
 * Instant
 * Kicker {R}
 * Target creature gets +3/+3 until end of turn. If this spell was kicked, instead that creature
 * gets +4/+4 and gains trample and haste until end of turn.
 */
val ColossalGrowth = card("Colossal Growth") {
    manaCost = "{1}{G}"
    colorIdentity = "GR"
    typeLine = "Instant"
    oracleText = "Kicker {R} (You may pay an additional {R} as you cast this spell.)\n" +
        "Target creature gets +3/+3 until end of turn. If this spell was kicked, instead that " +
        "creature gets +4/+4 and gains trample and haste until end of turn."

    keywordAbility(KeywordAbility.kicker("{R}"))

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = WasKicked,
            then = Effects.ModifyStats(4, 4, t) then
                Effects.GrantKeyword(Keyword.TRAMPLE, t) then
                Effects.GrantKeyword(Keyword.HASTE, t),
            otherwise = Effects.ModifyStats(3, 3, t)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Eelis Kyttanen"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9142f35-75f4-44c7-b03c-79c4a2d43c88.jpg?1783921304"
    }
}
