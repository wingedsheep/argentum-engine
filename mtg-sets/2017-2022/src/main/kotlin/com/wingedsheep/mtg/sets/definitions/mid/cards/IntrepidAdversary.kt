package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Intrepid Adversary — Innistrad: Midnight Hunt #25
 * {1}{W} · Creature — Human Scout · 3/1
 *
 * Lifelink
 * When this creature enters, you may pay {1}{W} any number of times. When you pay this cost one
 * or more times, put that many valor counters on this creature.
 * Creatures you control get +1/+1 for each valor counter on this creature.
 *
 * The repeated payment is [Effects.PayRepeatedly] under an optional reflexive trigger (CR 603.12:
 * the reflexive half triggers once, sized by the repetition count read back with
 * [DynamicAmounts.timesPaid]). The anthem is a live static over the valor counters currently on
 * the Adversary, so it includes the Adversary itself and shrinks if counters are removed.
 */
val IntrepidAdversary = card("Intrepid Adversary") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Scout"
    power = 3
    toughness = 1
    oracleText = "Lifelink\n" +
        "When this creature enters, you may pay {1}{W} any number of times. When you pay this cost " +
        "one or more times, put that many valor counters on this creature.\n" +
        "Creatures you control get +1/+1 for each valor counter on this creature."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayRepeatedly("{1}{W}"),
            optional = true,
            reflexiveEffect = Effects.AddDynamicCounters(
                CounterType.VALOR,
                DynamicAmounts.timesPaid(),
                EffectTarget.Self
            ),
            descriptionOverride = "You may pay {1}{W} any number of times. When you pay this cost " +
                "one or more times, put that many valor counters on this creature."
        )
        description = "When this creature enters, you may pay {1}{W} any number of times. When you " +
            "pay this cost one or more times, put that many valor counters on this creature."
    }

    staticAbility {
        val valor = DynamicAmounts.countersOnSelf(CounterType.VALOR)
        ability = GrantDynamicStats(
            filter = GroupFilter.AllCreaturesYouControl,
            powerBonus = valor,
            toughnessBonus = valor
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "25"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/773199f9-c83b-4a77-8342-9f1552ecf595.jpg?1783925657"
    }
}
