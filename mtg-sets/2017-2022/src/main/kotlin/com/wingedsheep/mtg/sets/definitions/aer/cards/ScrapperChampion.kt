package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scrapper Champion
 * {3}{R}
 * Creature — Human Artificer
 * 2/2
 * Double strike
 * When this creature enters, you get {E}{E} (two energy counters).
 * Whenever this creature attacks, you may pay {E}{E}. If you do, put a +1/+1 counter on it.
 *
 * Aether Revolt is the earliest printing; the only later one is Kaladesh Remastered (KLR), a
 * digital-only set this repo doesn't carry.
 */
val ScrapperChampion = card("Scrapper Champion") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 2
    oracleText = "Double strike (This creature deals both first-strike and regular combat damage.)\n" +
        "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "Whenever this creature attacks, you may pay {E}{E}. If you do, put a +1/+1 counter on it."

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 2),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/117ef25c-e9cf-4801-a753-2c329c2a8bdb.jpg?1783936749"
    }
}
