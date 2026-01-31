package com.wingedsheep.mtg.sets.definitions.lorwyn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ModifyStatsEffect
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Appeal to Eirdu
 * {3}{W}
 * Instant
 * Convoke (Your creatures can help cast this spell. Each creature you tap while
 * casting this spell pays for {1} or one mana of that creature's color.)
 * One or two target creatures each get +2/+1 until end of turn.
 */
val AppealToEirdu = card("Appeal to Eirdu") {
    manaCost = "{3}{W}"
    typeLine = "Instant"

    keywords(Keyword.CONVOKE)

    spell {
        target = TargetCreature(count = 2, minCount = 1)
        effect = ModifyStatsEffect(
            powerModifier = 2,
            toughnessModifier = 1,
            target = EffectTarget.ContextTarget(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Milivoj Ćeran"
    }
}
