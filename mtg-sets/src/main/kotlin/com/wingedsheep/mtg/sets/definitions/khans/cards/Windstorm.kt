package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Windstorm
 * {X}{G}
 * Instant
 * Windstorm deals X damage to each creature with flying.
 */
val Windstorm = card("Windstorm") {
    manaCost = "{X}{G}"
    typeLine = "Instant"
    oracleText = "Windstorm deals X damage to each creature with flying."

    spell {
        effect = ForEachInGroupEffect(
            GroupFilter.AllCreatures.withKeyword(Keyword.FLYING),
            DealDamageEffect(DynamicAmount.XValue, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Jonas De Ro"
        flavorText = "\"When the last dragon fell, its spirit escaped as a roar into the wind.\" —Temur tale"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/154dc31c-ac9d-4b78-b92b-e7bacc532915.jpg?1562782948"
    }
}
