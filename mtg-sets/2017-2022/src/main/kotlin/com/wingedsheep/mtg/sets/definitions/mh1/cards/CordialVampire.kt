package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cordial Vampire
 * {B}{B}
 * Creature — Vampire
 * 1/1
 * Whenever this creature or another creature dies, put a +1/+1 counter on each Vampire you control.
 *
 * "This creature or another creature dies" is any creature dying (the Falkenrath Noble trigger);
 * the counters go on every Vampire you control as the trigger resolves.
 */
val CordialVampire = card("Cordial Vampire") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    oracleText = "Whenever this creature or another creature dies, put a +1/+1 counter on each Vampire you control."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dies()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.VAMPIRE).youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Winona Nelson"
        flavorText = "\"Please, come in! Hors d'oeuvres are on the table.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a90a1c44-ee0f-4c12-bfaf-6f371bcce167.jpg?1783933130"
        ruling("2019-06-14", "If Cordial Vampire dies at the same time as one or more other creatures, its ability triggers for each of them.")
        ruling("2019-06-14", "If a Vampire you control is dealt lethal damage at the same time as another creature, they die at the same time. That Vampire can't receive a counter from Cordial Vampire's ability in time to save it.")
    }
}
