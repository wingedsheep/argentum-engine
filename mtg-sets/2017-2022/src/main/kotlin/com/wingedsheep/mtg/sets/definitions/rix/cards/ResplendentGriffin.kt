package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Resplendent Griffin — Rivals of Ixalan #170
 * {1}{W}{U} · Creature — Griffin · 2/2
 *
 * Flying
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * Whenever this creature attacks, if you have the city's blessing, put a +1/+1 counter on it.
 *
 * Deadeye Brawler's shape: the Ascend keyword plus an attack trigger gated by the intervening-if
 * [Conditions.YouHaveCitysBlessing].
 */
val ResplendentGriffin = card("Resplendent Griffin") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Griffin"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "Whenever this creature attacks, if you have the city's blessing, put a +1/+1 counter on it."

    keywords(Keyword.FLYING, Keyword.ASCEND)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.YouHaveCitysBlessing
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "Sam Rowan"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/915f2ad9-fd39-465c-8261-bfa34116cbcc.jpg?1783935271"
        ruling(
            "2018-01-19",
            "Once you have the city's blessing, you have it for the rest of the game, even if you lose control of some or all of your permanents. " +
                "The city's blessing isn't a permanent itself and can't be removed by any effect."
        )
        ruling(
            "2018-01-19",
            "Some cards have triggered abilities with an intervening \"if\" clause that checks whether you have the city's blessing. " +
                "You must already have the city's blessing in order for these abilities to trigger; otherwise they do nothing."
        )
    }
}
