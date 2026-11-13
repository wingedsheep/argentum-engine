package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oran-Rief Hydra — Battle for Zendikar #181
 * {4}{G}{G} · Creature — Hydra · 5/5
 * Trample
 * Landfall — Whenever a land you control enters, put a +1/+1 counter on this creature. If that land
 * is a Forest, put two +1/+1 counters on this creature instead.
 *
 * "If that land is a Forest" reads the triggering land on resolution, the same
 * `EntityMatches(TriggeringEntity, …)` shape as Emeria Shepherd's Plains check.
 */
val OranRiefHydra = card("Oran-Rief Hydra") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hydra"
    power = 5
    toughness = 5
    oracleText = "Trample\n" +
        "Landfall — Whenever a land you control enters, put a +1/+1 counter on this creature. If that " +
        "land is a Forest, put two +1/+1 counters on this creature instead."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                EffectTarget.TriggeringEntity,
                GameObjectFilter.Land.withSubtype("Forest"),
            ),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
            otherwise = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
        )
        description = "Landfall — Whenever a land you control enters, put a +1/+1 counter on this " +
            "creature. If that land is a Forest, put two +1/+1 counters on this creature instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74c3276b-2cbc-4266-b16a-bf72c044d95b.jpg?1783938186"
    }
}
