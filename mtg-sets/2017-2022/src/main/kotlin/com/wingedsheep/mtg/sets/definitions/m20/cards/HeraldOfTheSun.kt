package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Herald of the Sun (M20 #23)
 * {4}{W}{W}  Creature — Angel  4/4
 *
 * Flying
 * {3}{W}: Put a +1/+1 counter on another target creature with flying.
 */
val HeraldOfTheSun = card("Herald of the Sun") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    oracleText = "Flying\n" +
        "{3}{W}: Put a +1/+1 counter on another target creature with flying."
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{3}{W}")
        val t = target(TargetFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)).other())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Micah Epstein"
        flavorText = "\"Fear neither clouds nor darkness. The light shall overcome them all.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c62f724-7fb4-4498-b5c1-cf65d86d8e95.jpg"
    }
}
