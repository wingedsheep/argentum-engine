package com.wingedsheep.mtg.sets.definitions.iko.cards

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
 * Momentum Rumbler
 * {3}{R}
 * Creature — Dinosaur
 * 3/3
 * Whenever this creature attacks, if it doesn't have first strike, put a first strike counter on it.
 * Whenever this creature attacks, if it has first strike, it gains double strike until end of turn.
 *
 * Two intervening-if attack triggers (CR 603.4) reading the source's *projected* keywords, so a
 * first strike counter, an equipment grant, or a pump spell all count. Double strike alone is not
 * first strike, so a Rumbler that only has double strike still gets the counter.
 */
val MomentumRumbler = card("Momentum Rumbler") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dinosaur"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature attacks, if it doesn't have first strike, put a first strike counter on it.\n" +
        "Whenever this creature attacks, if it has first strike, it gains double strike until end of turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.Not(Conditions.SourceMatches(GameObjectFilter.Any.withKeyword(Keyword.FIRST_STRIKE)))
        effect = Effects.AddCounters(CounterType.FIRST_STRIKE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.SourceMatches(GameObjectFilter.Any.withKeyword(Keyword.FIRST_STRIKE))
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Andrey Kuzinskiy"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/3587f450-c1ba-4074-84bb-47978a2b6116.jpg?1783931046"
    }
}
