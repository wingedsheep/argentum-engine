package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Riparian Tiger — Kaladesh #167
 * {3}{G}{G} · Creature — Cat · 4/4
 *
 * Trample
 * When this creature enters, you get {E}{E} (two energy counters).
 * Whenever this creature attacks, you may pay {E}{E}. If you do, it gets +2/+2 until end of turn.
 */
val RiparianTiger = card("Riparian Tiger") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    power = 4
    toughness = 4
    oracleText = "Trample\n" +
        "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "Whenever this creature attacks, you may pay {E}{E}. If you do, it gets +2/+2 until end of turn."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 2),
            then = Effects.ModifyStats(2, 2, EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/ccf7e050-9631-431c-bcae-f74d91d537b0.jpg?1783937175"
    }
}
