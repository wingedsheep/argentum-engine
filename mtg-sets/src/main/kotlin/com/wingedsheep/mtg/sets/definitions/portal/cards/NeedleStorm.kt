package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Needle Storm
 * {2}{G}
 * Sorcery
 * Needle Storm deals 4 damage to each creature with flying.
 */
val NeedleStorm = card("Needle Storm") {
    manaCost = "{2}{G}"
    typeLine = "Sorcery"

    spell {
        effect = ForEachInGroupEffect(
            GroupFilter.AllCreatures.withKeyword(Keyword.FLYING),
            DealDamageEffect(4, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "179"
        artist = "Charles Gillespie"
        flavorText = "The forest defends its domain against all who soar above."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29a44e44-94b1-4bd2-8e00-6bd2ec07ee4c.jpg"
    }
}
