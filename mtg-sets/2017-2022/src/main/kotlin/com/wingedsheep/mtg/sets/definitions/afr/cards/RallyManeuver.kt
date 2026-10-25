package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Rally Maneuver
 * {2}{W}
 * Instant
 * Target creature gets +2/+0 and gains first strike until end of turn. Up to one other target
 * creature gets +0/+2 and gains lifelink until end of turn.
 *
 * Two targets: the primary creature, then an optional [TargetOther] creature (the Arm the Cathars
 * shape). A skipped second target simply confers nothing.
 */
val RallyManeuver = card("Rally Maneuver") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+0 and gains first strike until end of turn. Up to one other " +
        "target creature gets +0/+2 and gains lifelink until end of turn."

    spell {
        val primary = target(TargetFilter.Creature)
        val second = target(TargetOther(TargetObject(filter = TargetFilter.Creature, optional = true)))
        effect = Effects.ModifyStats(2, 0, primary) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, primary) then
            Effects.ModifyStats(0, 2, second) then
            Effects.GrantKeyword(Keyword.LIFELINK, second)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Vincent Proce"
        flavorText = "\"Don't give up! You are steel and fire, and victory is yours to seize.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/2/926ce53e-8e30-4018-a9e7-7266791b5d27.jpg?1783926524"
    }
}
