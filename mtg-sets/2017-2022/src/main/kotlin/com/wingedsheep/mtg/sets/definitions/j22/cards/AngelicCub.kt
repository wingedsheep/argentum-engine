package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Angelic Cub
 * {1}{W}
 * Creature — Cat Angel
 * 1/1
 *
 * Whenever this creature becomes the target of a spell or ability for the first time each turn,
 * put a +1/+1 counter on it.
 * As long as this creature has three or more +1/+1 counters on it, it has flying.
 *
 * The trigger is `becomesTarget(firstTimeEachTurn = true)` with no controller axis, so its window is
 * the first time *any* spell or ability targets it this turn — an opponent's targeting closes it too
 * (unlike Valiant's "you control"). The flying half is a [Conditions.SourceCounterCountAtLeast]-gated
 * static, as on Skyknight Squire.
 */
val AngelicCub = card("Angelic Cub") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Angel"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature becomes the target of a spell or ability for the first time each turn, " +
        "put a +1/+1 counter on it.\n" +
        "As long as this creature has three or more +1/+1 counters on it, it has flying."

    triggeredAbility {
        trigger = Triggers.self.becomesTarget(firstTimeEachTurn = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.PLUS_ONE_PLUS_ONE, 3)
        ability = GrantKeyword(Keyword.FLYING, Filters.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "Miranda Meeks"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f45e1832-1070-4d12-aba9-dcad47a02eda.jpg?1783919197"
        ruling(
            "2024-11-08",
            "Angelic Cub's first ability will resolve before the spell or ability that caused it to trigger."
        )
        ruling(
            "2024-11-08",
            "If a spell or ability has one or more of its targets changed to Angelic Cub, Angelic Cub's first ability will trigger if it hasn't yet been the target of a spell or ability this turn."
        )
        ruling(
            "2024-11-08",
            "If you create a copy of a spell on the stack and target Angelic Cub, Angelic Cub's first ability will trigger as long as it hasn't yet been the target of a spell or ability this turn."
        )
    }
}
