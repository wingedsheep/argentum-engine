package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Cloudsculpt Technician
 * {2}{U}
 * Creature — Jellyfish Artificer
 * Flying
 * As long as you control an artifact, this creature gets +1/+0.
 */
val CloudsculptTechnician = card("Cloudsculpt Technician") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Jellyfish Artificer"
    power = 1
    toughness = 4
    oracleText = "Flying\nAs long as you control an artifact, this creature gets +1/+0."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantDynamicStatsEffect(
                target = StaticTarget.SourceCreature,
                powerBonus = DynamicAmount.Fixed(1),
                toughnessBonus = DynamicAmount.Fixed(0)
            ),
            condition = Conditions.ControlArtifact
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Elizabeth Peiró"
        flavorText = "A cloudsculptor at work is like a carefully balanced weather system, and just as dangerous to interfere with."
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51077a54-15cf-4088-8e84-088d72e8e861.jpg?1752946745"
    }
}
