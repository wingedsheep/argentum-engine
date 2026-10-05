package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Faerie Vandal
 * {1}{U}
 * Creature — Faerie Rogue
 * 1/2
 * Flash
 * Flying
 * Whenever you draw your second card each turn, put a +1/+1 counter on this creature.
 */
val FaerieVandal = card("Faerie Vandal") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Rogue"
    power = 1
    toughness = 2
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Flying\n" +
        "Whenever you draw your second card each turn, put a +1/+1 counter on this creature."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Paul Scott Canavan"
        flavorText = "History may be written by the triumphant, but it's often rewritten by the troublesome."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/789c5c2b-3e51-4f6c-ac76-d276facf716f.jpg?1783932658"
        ruling(
            "2019-10-04",
            "The triggered ability can trigger only once each turn. It doesn't matter whether the permanent " +
                "with that ability was on the battlefield when the first card was drawn. If it's not on the " +
                "battlefield when the second card is drawn, the ability can't trigger at all that turn. It " +
                "won't trigger when the third or fourth card is drawn."
        )
    }
}
