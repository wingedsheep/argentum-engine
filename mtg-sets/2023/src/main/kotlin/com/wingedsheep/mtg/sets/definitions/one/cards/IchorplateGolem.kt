package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ichorplate Golem — Phyrexia: All Will Be One #230
 * {3} · Artifact Creature — Phyrexian Golem · Uncommon
 * 2/3
 *
 * Whenever a creature you control enters, if it has one or more oil counters on it, put an oil
 * counter on it.
 * Creatures you control with oil counters on them get +1/+1.
 *
 * Modeling notes:
 *  - "If it has one or more oil counters on it" is an intervening-if (CR 603.4) on the entering
 *    creature, checked both when the trigger would fire and on resolution.
 *  - The lord reads projected state over creatures you control carrying an oil counter.
 */
val IchorplateGolem = card("Ichorplate Golem") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Phyrexian Golem"
    power = 2
    toughness = 3
    oracleText = "Whenever a creature you control enters, if it has one or more oil counters on it, " +
        "put an oil counter on it.\n" +
        "Creatures you control with oil counters on them get +1/+1."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        interveningIf = Conditions.EntityMatches(
            EffectTarget.TriggeringEntity,
            GameObjectFilter.Any.withCounter(CounterType.OIL),
        )
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.TriggeringEntity)
    }

    staticAbility {
        ability = ModifyStats(1, 1, GroupFilter(GameObjectFilter.Creature.youControl().withCounter(CounterType.OIL)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Sam Wolfe Connelly"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e555abb1-ffd4-4143-9be8-ec8a758f5c2a.jpg?1783917991"
    }
}
