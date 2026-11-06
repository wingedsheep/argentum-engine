package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Foundry Hornet
 * {3}{B}
 * Creature — Insect
 * 2/3
 * Flying
 * When this creature enters, if you control a creature with a +1/+1 counter on it, creatures your
 * opponents control get -1/-1 until end of turn.
 */
val FoundryHornet = card("Foundry Hornet") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Insect"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, if you control a creature with a +1/+1 counter on it, creatures " +
        "your opponents control get -1/-1 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouControl(
            GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE)
        )
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesOpponentsControl,
            Effects.ModifyStats(-1, -1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Christopher Moeller"
        flavorText = "Its buzzing is easily mistaken for the hum of a thopter's wings."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6dc7865-16ed-4d12-bdb4-d40fbdd48a23.jpg?1783936763"
        ruling("2017-02-09", "The set of creatures affected by Foundry Hornet's triggered ability is determined as the ability resolves. Creatures opponents begin to control later in the turn and permanents opponents control that become creatures later in the turn won't get -1/-1. The effect continues until end of turn even if you stop controlling a creature with a +1/+1 counter on it.")
        ruling("2017-02-09", "If you don't control a creature with a +1/+1 counter as Foundry Hornet enters the battlefield, its ability doesn't trigger, even if you can give a +1/+1 counter to a creature right away. If you control no creatures with a +1/+1 counter as the ability resolves, nothing happens.")
        ruling("2017-02-09", "If an effect causes Foundry Hornet to enter the battlefield with a +1/+1 counter on it, it satisfies its own triggered ability.")
    }
}
