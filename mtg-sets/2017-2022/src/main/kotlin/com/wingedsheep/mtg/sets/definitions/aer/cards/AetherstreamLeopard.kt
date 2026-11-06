package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aetherstream Leopard
 * {2}{G}
 * Creature — Cat
 * 2/3
 * Trample
 * When this creature enters, you get {E} (an energy counter).
 * Whenever this creature attacks, you may pay {E}. If you do, it gets +2/+0 until end of turn.
 */
val AetherstreamLeopard = card("Aetherstream Leopard") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    power = 2
    toughness = 3
    oracleText = "Trample\n" +
        "When this creature enters, you get {E} (an energy counter).\n" +
        "Whenever this creature attacks, you may pay {E}. If you do, it gets +2/+0 until end of turn."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(1)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 1),
            then = Effects.ModifyStats(2, 0, EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f3a727a-5eca-44de-aa8f-0d2573db83eb.jpg?1783936746"
    }
}
