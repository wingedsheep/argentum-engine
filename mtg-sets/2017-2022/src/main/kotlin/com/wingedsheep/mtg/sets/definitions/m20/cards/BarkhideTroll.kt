package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Barkhide Troll (M20 #165)
 * {G}{G}  Creature — Troll  2/2
 *
 * This creature enters with a +1/+1 counter on it.
 * {1}, Remove a +1/+1 counter from this creature: This creature gains hexproof until end of turn.
 */
val BarkhideTroll = card("Barkhide Troll") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Troll"
    oracleText = "This creature enters with a +1/+1 counter on it.\n" +
        "{1}, Remove a +1/+1 counter from this creature: This creature gains hexproof until end of turn. " +
        "(It can't be the target of spells or abilities your opponents control.)"
    power = 2
    toughness = 2

    replacementEffect(
        EntersWithCounters(
            count = 1,
            selfOnly = true
        )
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.RemoveCounterFromSelf(CounterType.PLUS_ONE_PLUS_ONE, 1)
        )
        effect = Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cfd2e76-ad64-42ff-a6a2-c4cf1bec5932.jpg"
    }
}
