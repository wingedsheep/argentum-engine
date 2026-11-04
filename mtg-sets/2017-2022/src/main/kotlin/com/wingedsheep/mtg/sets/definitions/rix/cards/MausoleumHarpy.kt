package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mausoleum Harpy — Rivals of Ixalan #78
 * {4}{B} · Creature — Harpy · 3/3
 *
 * Flying
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * Whenever another creature you control dies, if you have the city's blessing, put a +1/+1 counter on
 * this creature.
 *
 * Feral Ghoul's "another creature you control dies" counter trigger, with the blessing as an
 * intervening-if (`interveningIf`, Twilight Prophet's shape) so it neither triggers nor resolves
 * without the city's blessing.
 */
val MausoleumHarpy = card("Mausoleum Harpy") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Harpy"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "Whenever another creature you control dies, if you have the city's blessing, put a +1/+1 counter on this creature."

    keywords(Keyword.FLYING, Keyword.ASCEND)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        interveningIf = Conditions.YouHaveCitysBlessing
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54b40f3c-d75e-483f-b978-8b3bf30cc7ce.jpg?1783935309"
        ruling(
            "2018-01-19",
            "If another creature you control is dealt lethal damage at the same time that Mausoleum Harpy is dealt lethal damage, " +
                "Mausoleum Harpy won't be saved by the +1/+1 counter that would have been put on it."
        )
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
