package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Aeronaut Cavalry
 * {4}{W}
 * Creature — Human Soldier
 * 3/4
 * Flying
 * When this creature enters, put a +1/+1 counter on another target Soldier you control.
 *
 * "Soldier" is a bare tribal noun, so the target is a *permanent* with the subtype, made "another"
 * with `.other()`.
 */
val AeronautCavalry = card("Aeronaut Cavalry") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, put a +1/+1 counter on another target Soldier you control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Permanent.withSubtype("Soldier").youControl().other())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Lorenzo Mastroianni"
        flavorText = "\"The ground is where wars are waged, but the air is where they are won.\"\n—Harbin, vanguard aviator"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38a62bb2-bc33-44d4-9a7e-92c9ea7d3c2c.jpg"
    }
}
