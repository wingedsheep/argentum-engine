package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Jade Bearer
 * {G}
 * Creature — Merfolk Shaman
 * 1/1
 *
 * When this creature enters, put a +1/+1 counter on another target Merfolk you control.
 */
val JadeBearer = card("Jade Bearer") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Merfolk Shaman"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, put a +1/+1 counter on another target Merfolk you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Permanent.withSubtype("Merfolk").youControl().other())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Alex Konstad"
        flavorText = "\"Sisora wore this, who defeated the tyrant of Azcanta, and before her Natena of Vuhana's band. May it bring you their strength.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/9/696bb954-353e-488b-a1e8-0df75da6339b.jpg?1783935285"
    }
}
