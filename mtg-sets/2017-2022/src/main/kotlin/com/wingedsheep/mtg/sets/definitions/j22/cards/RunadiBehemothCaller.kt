package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Runadi, Behemoth Caller
 * {2}{G}
 * Legendary Creature — Cat Shaman
 * 1/3
 *
 * Whenever you cast a creature spell with mana value 5 or greater, that creature enters with X
 * additional +1/+1 counters on it, where X is its mana value minus 4.
 * Creatures you control with three or more +1/+1 counters on them have haste.
 * {T}: Add {G}.
 *
 * - The cast trigger is Torgal, A Fine Hound's shape: the counters go on the creature spell while
 *   it is still on the stack ([EffectTarget.TriggeringEntity]) and travel with it as it resolves,
 *   so the creature enters with them even if Runadi has left by then. X reads the spell's own mana
 *   value, which is at least 5, so X is never negative.
 * - The haste grant is a static over `withCounter(PLUS_ONE_PLUS_ONE, atLeast = 3)` — the threshold
 *   form of the counter filter, read live, so a creature loses haste the moment it drops below
 *   three counters.
 */
val RunadiBehemothCaller = card("Runadi, Behemoth Caller") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Cat Shaman"
    power = 1
    toughness = 3
    oracleText = "Whenever you cast a creature spell with mana value 5 or greater, that creature " +
        "enters with X additional +1/+1 counters on it, where X is its mana value minus 4.\n" +
        "Creatures you control with three or more +1/+1 counters on them have haste.\n" +
        "{T}: Add {G}."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.manaValueAtLeast(5))
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.manaValueOf(EffectTarget.TriggeringEntity) - 4,
            EffectTarget.TriggeringEntity,
        )
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.HASTE,
            GroupFilter(
                GameObjectFilter.Creature.youControl()
                    .withCounter(CounterType.PLUS_ONE_PLUS_ONE, atLeast = 3)
            ),
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Billy Christian"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4f5500f-e82c-44f3-8be8-cee4c86824b1.jpg?1783919179"
    }
}
