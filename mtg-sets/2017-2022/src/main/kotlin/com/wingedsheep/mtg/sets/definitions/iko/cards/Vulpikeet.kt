package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vulpikeet
 * {3}{W}
 * Creature — Fox Bird
 * 2/3
 * Mutate {2}{W}
 * Flying
 * Whenever this creature mutates, put a +1/+1 counter on it.
 */
val Vulpikeet = card("Vulpikeet") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Fox Bird"
    power = 2
    toughness = 3
    oracleText = "Mutate {2}{W} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Flying\n" +
        "Whenever this creature mutates, put a +1/+1 counter on it."

    keywordAbility(KeywordAbility.mutate("{2}{W}"))
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.mutates()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Leesha Hannigan"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07bce816-3d18-4ca1-b88f-7307ec7c345d.jpg?1783931082"
    }
}
