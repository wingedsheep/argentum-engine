package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Quirion Beastcaller (DMU #175)
 * {1}{G}
 * Creature — Dryad Warrior
 * 2/2
 *
 * Whenever you cast a creature spell, put a +1/+1 counter on this creature.
 * When this creature dies, distribute X +1/+1 counters among any number of target creatures you
 * control, where X is the number of +1/+1 counters on this creature.
 *
 * X is the last-known +1/+1 counter count off the dies event. "Any number of target creatures"
 * is `unlimited` capped by X (CR 601.2d — each target needs at least one counter); the cap is
 * snapshotted as the trigger goes on the stack, where the split is also announced (CR 603.3d).
 */
val QuirionBeastcaller = card("Quirion Beastcaller") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dryad Warrior"
    power = 2
    toughness = 2
    oracleText = "Whenever you cast a creature spell, put a +1/+1 counter on this creature.\n" +
        "When this creature dies, distribute X +1/+1 counters among any number of target creatures " +
        "you control, where X is the number of +1/+1 counters on this creature."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        targets(
            TargetFilter.CreatureYouControl,
            unlimited = true,
            dynamicMaxCount = DynamicAmounts.lastKnownPlusOneCounters(),
        )
        effect = Effects.DistributeCountersAmongTargets(DynamicAmounts.lastKnownPlusOneCounters())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a501319-59a7-45d4-a6ca-666276095f87.jpg?1783921296"
        ruling("2022-09-09", "You choose the targets and announce how the +1/+1 counters will be distributed as you put Quirion Beastcaller's second ability on the stack. Each target must receive at least one +1/+1 counter.")
        ruling("2022-09-09", "If some of the creatures are illegal targets as the second triggered ability tries to resolve, the original distribution of counters still applies and the counters that would have been put on illegal targets are lost.")
    }
}
