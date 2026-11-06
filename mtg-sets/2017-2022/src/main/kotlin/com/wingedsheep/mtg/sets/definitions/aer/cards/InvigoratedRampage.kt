package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Invigorated Rampage
 * {1}{R}
 * Instant
 * Choose one —
 * • Target creature gets +4/+0 and gains trample until end of turn.
 * • Two target creatures each get +2/+0 and gain trample until end of turn.
 */
val InvigoratedRampage = card("Invigorated Rampage") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target creature gets +4/+0 and gains trample until end of turn.\n" +
        "• Two target creatures each get +2/+0 and gain trample until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("Target creature gets +4/+0 and gains trample until end of turn") {
                val t = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(4, 0, t) then Effects.GrantKeyword(Keyword.TRAMPLE, t)
            }
            mode("Two target creatures each get +2/+0 and gain trample until end of turn") {
                targets(TargetFilter.Creature, count = 2)
                effect = Effects.ForEachTarget(
                    Effects.ModifyStats(2, 0, EffectTarget.ContextTarget(0)) then
                        Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.ContextTarget(0))
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Craig J Spearing"
        flavorText = "Gremlins can change from pest to problem to plague in the span of a moment."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c1d1727-99d4-4aee-b6f5-8399ac1d0184.jpg"
        ruling("2017-02-09", "If you choose Invigorated Rampage's second mode and one target becomes an illegal target, the remaining target gets +2/+0 and gains trample. It doesn't get +4/+0.")
    }
}
