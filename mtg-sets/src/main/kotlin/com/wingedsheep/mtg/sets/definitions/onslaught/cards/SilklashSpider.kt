package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Silklash Spider
 * {3}{G}{G}
 * Creature — Spider
 * 2/7
 * Reach
 * {X}{G}{G}: Silklash Spider deals X damage to each creature with flying.
 */
val SilklashSpider = card("Silklash Spider") {
    manaCost = "{3}{G}{G}"
    typeLine = "Creature — Spider"
    power = 2
    toughness = 7
    oracleText = "Reach\n{X}{G}{G}: Silklash Spider deals X damage to each creature with flying."

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Mana("{X}{G}{G}")
        effect = ForEachInGroupEffect(GroupFilter.AllCreatures.withKeyword(Keyword.FLYING), DealDamageEffect(DynamicAmount.XValue, EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "281"
        artist = "Iain McCaig"
        flavorText = "\"The only thing that flies over the Krosan Forest is the wind.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e41680e2-6689-4263-a5a3-9fb2e4280d52.jpg?1562932614"
    }
}
