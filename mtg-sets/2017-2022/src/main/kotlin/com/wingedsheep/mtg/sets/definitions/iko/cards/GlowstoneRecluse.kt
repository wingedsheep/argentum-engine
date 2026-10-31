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
 * Glowstone Recluse
 * {2}{G}
 * Creature — Spider
 * 2/3
 * Mutate {3}{G}
 * Reach
 * Whenever this creature mutates, put two +1/+1 counters on it.
 */
val GlowstoneRecluse = card("Glowstone Recluse") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    power = 2
    toughness = 3
    oracleText = "Mutate {3}{G} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Reach\n" +
        "Whenever this creature mutates, put two +1/+1 counters on it."

    keywordAbility(KeywordAbility.mutate("{3}{G}"))
    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.mutates()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "156"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06a9698d-400c-4c31-82cd-28d0897fe28c.jpg?1783931035"
    }
}
