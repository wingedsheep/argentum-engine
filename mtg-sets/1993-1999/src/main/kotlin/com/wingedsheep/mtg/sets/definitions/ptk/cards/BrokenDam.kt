package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Broken Dam
 * {U}
 * Sorcery
 * Tap one or two target creatures without horsemanship.
 */
val BrokenDam = card("Broken Dam") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Tap one or two target creatures without horsemanship."

    spell {
        targets(
            TargetFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.HORSEMANSHIP)),
            count = 2,
            minCount = 1
        )
        effect = Effects.ForEachTarget(Effects.Tap(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Jiang Zhuqing"
        flavorText = "Using nature to their advantage, wise Three Kingdoms generals often let dammed rivers loose to destroy their enemies."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0f7b8b1-f1dc-46a3-8f4a-c6181e8a049f.jpg?1783946124"
    }
}
