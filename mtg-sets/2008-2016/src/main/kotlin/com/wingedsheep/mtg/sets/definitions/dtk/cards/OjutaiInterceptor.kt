package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ojutai Interceptor
 * {3}{U}
 * Creature — Bird Soldier
 * 3/1
 * Flying
 * Megamorph {3}{U}
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a
 * +1/+1 counter on the creature when it is turned face up for its megamorph cost.
 */
val OjutaiInterceptor = card("Ojutai Interceptor") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Soldier"
    power = 3
    toughness = 1
    oracleText = "Flying\n" +
        "Megamorph {3}{U} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)"

    keywords(Keyword.FLYING)

    // Megamorph {3}{U}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{3}{U}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Johann Bodin"
        flavorText = "\"Though I may soar, I could never fly as high as the dragons.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59a84666-ad2b-494c-b52b-ecee14788ef2.jpg?1783938605"
    }
}
