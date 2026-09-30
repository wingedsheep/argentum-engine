package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Minwu, White Mage
 * {3}{W}{W}
 * Legendary Creature — Human Cleric
 * 3/3
 * Vigilance, lifelink
 * Whenever you gain life, put a +1/+1 counter on each Cleric you control.
 */
val MinwuWhiteMage = card("Minwu, White Mage") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Cleric"
    power = 3
    toughness = 3
    oracleText = "Vigilance, lifelink\nWhenever you gain life, put a +1/+1 counter on each Cleric you control."

    keywords(Keyword.VIGILANCE, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        // "each Cleric you control" includes Minwu herself, so no excludeSelf.
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.CLERIC).youControl()),
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Josu Hernaiz"
        flavorText = "\"I shall remain here and devote myself to the care of the wounded.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/8/6822144f-f0eb-4e10-a217-52cad36d2973.jpg?1748705852"
    }
}
