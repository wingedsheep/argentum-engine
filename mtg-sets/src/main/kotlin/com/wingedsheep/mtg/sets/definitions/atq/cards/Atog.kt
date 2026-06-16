package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Atog
 * {1}{R}
 * Creature — Atog
 * 1/2
 * Sacrifice an artifact: This creature gets +2/+2 until end of turn.
 */
val Atog = card("Atog") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Atog"
    power = 1
    toughness = 2
    oracleText = "Sacrifice an artifact: This creature gets +2/+2 until end of turn."

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Artifact)
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        description = "Sacrifice an artifact: This creature gets +2/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/2249fc40-4412-48fd-800a-7ea3678aee3f.jpg?1562902227"
    }
}
