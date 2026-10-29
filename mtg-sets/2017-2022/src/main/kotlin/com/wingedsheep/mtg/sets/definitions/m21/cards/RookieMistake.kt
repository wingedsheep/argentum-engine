package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Rookie Mistake
 * {U}
 * Instant
 * Until end of turn, target creature gets +0/+2 and another target creature gets -2/-0.
 *
 * Two separate "target" instances; [TargetOther] forces the second to differ from the first, so
 * the spell can't be cast without two creatures to target. If one becomes illegal, the other is
 * still affected.
 */
val RookieMistake = card("Rookie Mistake") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gets +0/+2 and another target creature gets -2/-0."

    spell {
        val buffed = target(TargetFilter.Creature)
        val weakened = target(TargetOther(TargetObject(filter = TargetFilter.Creature)))
        effect = Effects.ModifyStats(0, 2, buffed) then Effects.ModifyStats(-2, 0, weakened)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Zoltan Boros"
        flavorText = "\"Stop locking your knees as you strike! Now stand up and try again.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b19de7a5-c291-405b-a2e6-8d3ac56e6570.jpg?1783930721"
        ruling("2020-06-23", "You can't cast Rookie Mistake unless you choose two creatures as targets.")
        ruling("2020-06-23", "If one target creature becomes an illegal target before Rookie Mistake resolves, the other is affected as appropriate.")
    }
}
