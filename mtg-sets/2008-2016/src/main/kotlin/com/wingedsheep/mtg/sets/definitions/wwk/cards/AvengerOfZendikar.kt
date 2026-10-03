package com.wingedsheep.mtg.sets.definitions.wwk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Avenger of Zendikar
 * {5}{G}{G}
 * Creature — Elemental
 * 5/5
 * When this creature enters, create a 0/1 green Plant creature token for each land you control.
 * Landfall — Whenever a land you control enters, you may put a +1/+1 counter on each Plant
 * creature you control.
 *
 * The ETB is [Effects.CreateToken] with a dynamic count of [DynamicAmounts.landsYouControl]
 * (counted on resolution). Landfall is `Triggers.a(Land.youControl()).enters()`, made optional,
 * over an [Effects.ForEachInGroup] of Plant creatures you control adding one +1/+1 counter each.
 */
val AvengerOfZendikar = card("Avenger of Zendikar") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    oracleText = "When this creature enters, create a 0/1 green Plant creature token for each land you control.\n" +
        "Landfall — Whenever a land you control enters, you may put a +1/+1 counter on each Plant creature you control."
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = DynamicAmounts.landsYouControl(),
            power = 0,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Plant"),
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        optional = true
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withSubtype("Plant").youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "96"
        artist = "Zoltan Boros & Gabor Szikszai"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dde073e0-f329-4ab0-8e31-a48929e017ce.jpg?1783942047"
    }
}
