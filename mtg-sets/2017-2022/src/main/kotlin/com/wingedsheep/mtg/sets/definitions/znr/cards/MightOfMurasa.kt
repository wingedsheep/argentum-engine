package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Might of Murasa — Zendikar Rising #194
 * {1}{G} · Instant
 *
 * Kicker {2}{G}
 * Target creature gets +3/+3 until end of turn. If this spell was kicked,
 * that creature gets +5/+5 until end of turn instead.
 */
val MightOfMurasa = card("Might of Murasa") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Kicker {2}{G} (You may pay an additional {2}{G} as you cast this spell.)\n" +
        "Target creature gets +3/+3 until end of turn. If this spell was kicked, " +
        "that creature gets +5/+5 until end of turn instead."

    keywordAbility(KeywordAbility.kicker("{2}{G}"))

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = WasKicked,
            then = Effects.ModifyStats(5, 5, t),
            otherwise = Effects.ModifyStats(3, 3, t)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "194"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42914fd9-e56e-4c3e-a445-fdfa45a45178.jpg?1783929336"
    }
}
