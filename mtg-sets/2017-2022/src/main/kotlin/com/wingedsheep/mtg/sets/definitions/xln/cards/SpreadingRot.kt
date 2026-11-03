package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spreading Rot
 * {4}{B}
 * Sorcery
 * Destroy target land. Its controller loses 2 life.
 */
val SpreadingRot = card("Spreading Rot") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target land. Its controller loses 2 life."

    spell {
        val land = target(TargetFilter.Land)
        effect = Effects.Destroy(land) then Effects.LoseLife(2, EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Yeong-Hao Han"
        flavorText = "\"What is this foul presence that defies the sun's cleansing rays?\"\n—Itzama the Crested"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca8a160c-5e89-4347-8e88-3541b6d221e0.jpg?1783935752"
    }
}
