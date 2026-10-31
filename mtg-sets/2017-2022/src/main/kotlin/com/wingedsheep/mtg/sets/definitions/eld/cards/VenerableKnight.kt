package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Venerable Knight
 * {W}
 * Creature — Human Knight
 * 2/1
 *
 * When this creature dies, put a +1/+1 counter on target Knight you control.
 */
val VenerableKnight = card("Venerable Knight") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    oracleText = "When this creature dies, put a +1/+1 counter on target Knight you control."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.dies()
        val knight = target(TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.KNIGHT).youControl()))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, knight)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Colin Boyer"
        flavorText = "\"May this blade guide you on your great journey, as it did me on mine.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0001f1ef-b957-4a55-b47f-14839cdbab6f.jpg"
    }
}
