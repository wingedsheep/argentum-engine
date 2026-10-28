package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grakmaw, Skyclave Ravager — Zendikar Rising #223 (canonical printing)
 * {1}{B}{G} · Legendary Creature — Hydra Horror · 0/0
 *
 * Both dies triggers read last-known information off the dying creature's zone change
 * ([DynamicAmounts.lastKnownPlusOneCounters]): the "another creature" trigger's intervening-if
 * reads the *dying creature's* +1/+1 count, and the self-dies token's X reads Grakmaw's own.
 */
val GrakmawSkyclaveRavager = card("Grakmaw, Skyclave Ravager") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Hydra Horror"
    power = 0
    toughness = 0
    oracleText = "Grakmaw enters with three +1/+1 counters on it.\n" +
        "Whenever another creature you control dies, if it had a +1/+1 counter on it, put a +1/+1 counter on Grakmaw.\n" +
        "When Grakmaw dies, create an X/X black and green Hydra creature token, where X is the number of +1/+1 counters on Grakmaw."

    replacementEffect(EntersWithCounters(count = 3, selfOnly = true))

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.lastKnownPlusOneCounters(),
            ComparisonOperator.GTE,
            1
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.BLACK, Color.GREEN),
            creatureTypes = setOf("Hydra"),
            dynamicPower = DynamicAmounts.lastKnownPlusOneCounters(),
            dynamicToughness = DynamicAmounts.lastKnownPlusOneCounters(),
            imageUri = "https://cards.scryfall.io/normal/front/f/2/f2ad0395-190d-4779-81b7-2832dc257828.jpg?1783929498"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "223"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd227384-e0e3-40ce-98bb-8b7cf1900ad7.jpg?1783929324"
        ruling(
            "2020-09-25",
            "Grakmaw's second ability puts just one +1/+1 counter on it, no matter how many counters the dying creature had."
        )
        ruling(
            "2020-09-25",
            "If a creature with a +1/+1 counter on it dies after receiving too many -1/-1 counters, it dies at the same time that the counters are removed, and Grakmaw's second ability triggers. Similarly, if Grakmaw dies after receiving too many -1/-1 counters, use the number of +1/+1 counters it had prior to receiving the -1/-1 counters to determine the value of X."
        )
    }
}
