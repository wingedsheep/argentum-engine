package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kuldotha Cackler
 * {2}{R}
 * Creature — Phyrexian Hyena
 * 2/3
 *
 * Trample
 * Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the number of
 * permanents you control with oil counters on them.
 */
val KuldothaCackler = card("Kuldotha Cackler") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Phyrexian Hyena"
    power = 2
    toughness = 3
    oracleText = "Trample\n" +
        "Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the number of " +
        "permanents you control with oil counters on them."

    keywords(Keyword.TRAMPLE)

    // X is counted on resolution of the trigger.
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ModifyStats(
            power = DynamicAmounts.count(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Permanent.withCounter(CounterType.OIL),
            ),
            toughness = DynamicAmounts.fixed(0),
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn,
        )
        description = "Whenever this creature attacks, it gets +X/+0 until end of turn, where X is " +
            "the number of permanents you control with oil counters on them."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Maxime Minard"
        flavorText = "You would laugh too if you just burned through a tyrranax's stomach."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7155c42-a824-4bbe-9f7a-fa01e7625fd6.jpg?1783918028"
    }
}
