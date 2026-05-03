package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters

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

    // Flying keyword
    staticAbility {
        effect = Effects.GrantKeyword(Keyword.FLYING)
    }

    // Static ability: +1/+0 as long as you control an artifact
    staticAbility {
        condition = Conditions.ControlArtifact
        effect = Effects.ModifyStats(1, 0)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Elizabeth Peiró"
        flavorText = "A cloudsculptor at work is like a carefully balanced weather system, and just as dangerous to interfere with."
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51077a54-15cf-4088-8e84-088d72e8e861.jpg?1752946745"
    }
}
