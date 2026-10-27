package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodsky Berserker
 * {1}{B}
 * Creature — Human Berserker
 * 1/1
 * Whenever you cast your second spell each turn, put two +1/+1 counters on this creature.
 * It gains menace until end of turn.
 */
val BloodskyBerserker = card("Bloodsky Berserker") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Berserker"
    oracleText = "Whenever you cast your second spell each turn, put two +1/+1 counters on this creature. " +
        "It gains menace until end of turn. (It can't be blocked except by two or more creatures.)"
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.you.castsNth(2)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.MENACE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Gabor Szikszai"
        flavorText = "\"Who's next?!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6308a53-de07-4ed9-80a1-3579843466c2.jpg?1783928254"
        ruling(
            "2021-02-05",
            "The triggered ability can trigger only once each turn. The ability will resolve before the second spell does. " +
                "It doesn't matter if the first spell you cast that turn has resolved, was countered, or is still on the stack."
        )
        ruling(
            "2021-02-05",
            "The triggered ability triggers only if the creature that has it is on the battlefield as you cast your second spell. " +
                "Spells you cast in a turn before that creature entered the battlefield will count."
        )
    }
}
