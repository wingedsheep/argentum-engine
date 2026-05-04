package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Kavaron Turbodrone
 * {2}{R}
 * Artifact Creature — Robot Scout
 * {T}: Target creature you control gets +1/+1 and gains haste until end of turn. Activate only as a sorcery.
 */
val KavaronTurbodrone = card("Kavaron Turbodrone") {
    manaCost = "{2}{R}"
    typeLine = "Artifact Creature — Robot Scout"
    power = 2
    toughness = 3
    oracleText = "{T}: Target creature you control gets +1/+1 and gains haste until end of turn. Activate only as a sorcery."

    // {T}: Target creature you control gets +1/+1 and gains haste until end of turn. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Tap
        val target = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.Composite(
            listOf(
                Effects.ModifyStats(1, 1, target),
                Effects.GrantKeyword(Keyword.HASTE, target)
            )
        )
        timing = TimingRule.SorcerySpeed
        description = "{T}: Target creature you control gets +1/+1 and gains haste until end of turn. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Leesha Hannigan"
        flavorText = "\"Safe? Buddy, the whole planet is falling apart, we don't have time for safe.\"\n—Dorgan, Kav roboticist"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5e92cdd-75df-499e-94f7-22287f1000b3.jpg?1753683197"
    }
}
