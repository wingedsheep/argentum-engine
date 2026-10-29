package com.wingedsheep.mtg.sets.definitions.arb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lorescale Coatl
 * {1}{G}{U}
 * Creature — Snake
 * 2/2
 * Whenever you draw a card, put a +1/+1 counter on this creature.
 *
 * Chasm Skulker's draw trigger: `Triggers.you.draws()` fires once per card drawn.
 */
val LorescaleCoatl = card("Lorescale Coatl") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Snake"
    power = 2
    toughness = 2
    oracleText = "Whenever you draw a card, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Greg Staples"
        flavorText = "\"The enlightenment I never found in etherium I have found traced in the coatl's scales.\"\n" +
            "—Ranalus, vedalken heretic"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e81c31c9-d7d7-4b45-893e-7e36d26c645f.jpg?1783942420"
        ruling(
            "2020-06-23",
            "If a spell or ability causes you to put a card into your hand without specifically using the word " +
                "\"draw,\" it's not a card drawn.",
        )
    }
}
