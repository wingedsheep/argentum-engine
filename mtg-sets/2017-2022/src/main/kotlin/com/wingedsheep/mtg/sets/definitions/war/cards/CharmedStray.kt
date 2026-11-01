package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Charmed Stray — War of the Spark #8 (canonical printing)
 * {W}
 * Creature — Cat
 * 1/1
 * Lifelink
 * When this creature enters, put a +1/+1 counter on each other creature you control named Charmed Stray.
 */
val CharmedStray = card("Charmed Stray") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat"
    power = 1
    toughness = 1
    oracleText = "Lifelink\n" +
        "When this creature enters, put a +1/+1 counter on each other creature you control named Charmed Stray."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.named("Charmed Stray").youControl(), excludeSelf = true),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Chris Rahn"
        flavorText = "\"There's something peculiar about the cats today.\"\n—Janoc, Tin Street tinker"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/357513b9-0505-4b67-9a29-36a705a175b1.jpg?1783933486"
    }
}
