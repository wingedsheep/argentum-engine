package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Doctor, EMH
 * {3}{G/W}{G/W}
 * Legendary Artifact Creature — Illusion Doctor
 * 3/3
 *
 * When The Doctor enters, put a +1/+1 counter on target creature. Then put a +1/+1 counter on
 * each creature you control with a counter on it.
 *
 * The group is read after the first counter lands, so a target you control is counted ("with a
 * counter on it") and ends up with two counters.
 */
val TheDoctorEmh = card("The Doctor, EMH") {
    manaCost = "{3}{G/W}{G/W}"
    typeLine = "Legendary Artifact Creature — Illusion Doctor"
    colorIdentity = "GW"
    oracleText = "When The Doctor enters, put a +1/+1 counter on target creature. " +
        "Then put a +1/+1 counter on each creature you control with a counter on it."
    power = 3
    toughness = 3
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.youControl().withAnyCounter()),
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
            )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "David Robert Hovey"
        flavorText = "\"Please state the nature of the medical emergency.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8fbad0dd-5f74-40c0-9e3e-c06be8eabb14.jpg?1792508313"
    }
}
