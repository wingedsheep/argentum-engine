package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hornbash Mentor
 * {2}{G}
 * Creature — Human Warrior
 * 3/3
 *
 * When this creature enters, put a trample counter on target non-Human creature you control.
 * {2}{G}, {T}: Put a +1/+1 counter on each creature you control with trample.
 *
 * A trample counter is a keyword counter ([CounterType.TRAMPLE]), so the creature keeps trample
 * for as long as the counter stays. "Each creature you control with trample" is read from
 * projected state as the ability resolves, so keyword counters and granted trample both count.
 */
val HornbashMentor = card("Hornbash Mentor") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, put a trample counter on target non-Human creature you control.\n" +
        "{2}{G}, {T}: Put a +1/+1 counter on each creature you control with trample."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val recipient = target(TargetFilter(GameObjectFilter.Creature.notSubtype(Subtype.HUMAN).youControl()))
        effect = Effects.AddCounters(CounterType.TRAMPLE, 1, recipient)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.Tap)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.TRAMPLE).youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Wisnu Tan"
        flavorText = "\"We share the same nuanced opinion of hunters and military goons: smash them.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d47d2596-a321-44c0-a9f6-87bf80abc1c4.jpg"
    }
}
