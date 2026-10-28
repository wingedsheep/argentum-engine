package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shell Shield — Zendikar Rising #79
 * {U} · Instant
 *
 * Kicker {1}
 * Target creature you control gets +0/+3 until end of turn. If this spell was kicked,
 * that creature also gains hexproof until end of turn.
 */
val ShellShield = card("Shell Shield") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Kicker {1} (You may pay an additional {1} as you cast this spell.)\n" +
        "Target creature you control gets +0/+3 until end of turn. If this spell was kicked, " +
        "that creature also gains hexproof until end of turn. " +
        "(It can't be the target of spells or abilities your opponents control.)"

    keywordAbility(KeywordAbility.kicker("{1}"))

    spell {
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(0, 3, t) then
            Effects.If(
                condition = WasKicked,
                then = Effects.GrantKeyword(Keyword.HEXPROOF, t)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "79"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3daede39-8eef-4272-a483-312dbd2dd30f.jpg?1783929386"
    }
}
