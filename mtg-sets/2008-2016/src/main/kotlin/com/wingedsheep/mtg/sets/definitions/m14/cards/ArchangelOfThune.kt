package com.wingedsheep.mtg.sets.definitions.m14.cards

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
 * Archangel of Thune
 * {3}{W}{W}
 * Creature — Angel
 * 3/4
 * Flying
 * Lifelink
 * Whenever you gain life, put a +1/+1 counter on each creature you control.
 */
val ArchangelOfThune = card("Archangel of Thune") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "Lifelink (Damage dealt by this creature also causes you to gain that much life.)\n" +
        "Whenever you gain life, put a +1/+1 counter on each creature you control."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.youControl()),
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
        description = "Whenever you gain life, put a +1/+1 counter on each creature you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "5"
        artist = "James Ryman"
        flavorText = "Even the wicked have nightmares."
        imageUri = "https://cards.scryfall.io/normal/front/5/3/531cba81-afd7-4be4-adec-87edb77ba2a9.jpg?1783939946"
        ruling("2020-08-07", "If a creature you control is dealt lethal damage at the same time that you gain life, it won't receive a counter from Archangel of Thune's ability in time to save it.")
        ruling("2020-08-07", "Each creature with lifelink dealing combat damage causes a separate life-gaining event. For example, if two creatures you control with lifelink deal combat damage at the same time, the ability will trigger twice. However, if a single creature you control with lifelink deals combat damage to multiple creatures, players, and/or planeswalkers at the same time (perhaps because it has trample or was blocked by more than one creature), the ability will trigger only once.")
        ruling("2020-08-07", "If you gain an amount of life \"for each\" of something, that life is gained as one event and the ability triggers only once.")
    }
}
