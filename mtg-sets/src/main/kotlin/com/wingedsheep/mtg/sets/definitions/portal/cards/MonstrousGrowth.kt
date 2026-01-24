package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ModifyStatsEffect
import com.wingedsheep.sdk.targeting.CreatureTargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Monstrous Growth
 * {1}{G}
 * Sorcery
 * Target creature gets +4/+4 until end of turn.
 */
val MonstrousGrowth = card("Monstrous Growth") {
    manaCost = "{1}{G}"
    typeLine = "Sorcery"

    spell {
        target = TargetCreature(filter = CreatureTargetFilter.Any)
        effect = ModifyStatsEffect(
            powerModifier = 4,
            toughnessModifier = 4,
            target = EffectTarget.ContextTarget(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Dan Frazier"
        flavorText = "Size does matter."
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86d56826-b75a-4c61-8fd3-62f20f8d8bd3.jpg"
    }
}
