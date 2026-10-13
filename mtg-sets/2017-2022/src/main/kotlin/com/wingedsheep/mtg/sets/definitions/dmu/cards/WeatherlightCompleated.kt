package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Weatherlight Compleated
 * {2}
 * Legendary Artifact — Vehicle
 * 5/5
 *
 * Flying
 * As long as Weatherlight Compleated has four or more phyresis counters on it, it's a Phyrexian
 * creature in addition to its other types.
 * Whenever a creature you control dies, put a phyresis counter on Weatherlight Compleated. Then draw
 * a card if it has seven or more phyresis counters on it. If it doesn't, scry 1.
 *
 * - The type change is two Layer 4 statics (CREATURE + the Phyrexian creature type) gated on the
 *   live phyresis count; once it's a creature its printed 5/5 applies (CR 301.7c).
 * - The draw-or-scry check reads the counter count after the counter is placed. If Weatherlight
 *   has left the battlefield by then, the source-counter read falls back to its last-known
 *   counters (ruling), so the count it left with decides.
 * - It has no crew ability; it only becomes a creature through its own second ability.
 */
val WeatherlightCompleated = card("Weatherlight Compleated") {
    manaCost = "{2}"
    typeLine = "Legendary Artifact — Vehicle"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "As long as Weatherlight Compleated has four or more phyresis counters on it, it's a Phyrexian " +
        "creature in addition to its other types.\n" +
        "Whenever a creature you control dies, put a phyresis counter on Weatherlight Compleated. Then " +
        "draw a card if it has seven or more phyresis counters on it. If it doesn't, scry 1."

    keywords(Keyword.FLYING)

    val fourOrMore = Conditions.SourceCounterCountAtLeast(CounterType.PHYRESIS, 4)

    staticAbility {
        condition = fourOrMore
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }
    staticAbility {
        condition = fourOrMore
        ability = GrantSubtype("Phyrexian", GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.AddCounters(CounterType.PHYRESIS, 1, EffectTarget.Self) then
            Effects.If(
                Conditions.SourceCounterCountAtLeast(CounterType.PHYRESIS, 7),
                Effects.DrawCards(1),
                otherwise = Effects.Scry(1),
            )
        description = "Whenever a creature you control dies, put a phyresis counter on Weatherlight Compleated. " +
            "Then draw a card if it has seven or more phyresis counters on it. If it doesn't, scry 1."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "242"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/976fd9d9-a9b5-40b6-ad38-4b3b263e1ebc.jpg?1783921267"
        ruling("2022-09-09", "If more than one creature dies at the same time, Weatherlight Compleated’s triggered ability will trigger once for each of those creatures. As each of those resolve, its controller will take the instructed actions. For example, if it has no phyresis counters on it and seven creatures die, its controller will put a counter on it and scry 1 as each of the first six instances of the ability resolve, then put a counter on it and draw a card as the last instance resolves.")
        ruling("2022-09-09", "If Weatherlight Compleated has left the battlefield by the time its triggered ability resolves, you won’t be able to add any counters to it. Whether you draw a card or scry 1 will be determined by the number of counters it had on it before it left the battlefield.")
        ruling("2022-09-09", "If Weatherlight Compleated is a creature and dies, its last ability will trigger.")
        ruling("2022-09-09", "If Weatherlight Compleated loses its abilities, the type-changing effect of its second ability will continue to apply to it. This is because effects that change an object’s types are always applied before effects that remove abilities. In that case, if it has four or more phyresis counters on it, it will be a legendary Phyrexian Vehicle artifact creature with no abilities. This is true even if it somehow gained its fourth phyresis counter after losing its abilities.")
    }
}
