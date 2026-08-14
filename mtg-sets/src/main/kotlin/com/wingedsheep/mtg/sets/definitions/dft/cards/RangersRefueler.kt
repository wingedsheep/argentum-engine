package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rangers' Refueler — Aetherdrift #55
 * {1}{U} · Artifact — Vehicle · 3/3
 *
 * The exhaust ability animates the Vehicle permanently; crew can still animate it until end of turn
 * before the exhaust ability is used.
 */
val RangersRefueler = card("Rangers' Refueler") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Vehicle"
    power = 3
    toughness = 3
    oracleText = "Whenever you activate an exhaust ability, draw a card.\n" +
        "Exhaust — {4}: This Vehicle becomes an artifact creature. Put a +1/+1 counter on it. " +
        "(Activate each exhaust ability only once.)\n" +
        "Crew 2"

    triggeredAbility {
        trigger = Triggers.YouActivateExhaustAbility
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Mana("{4}")
        isExhaust = true
        effect = Effects.Composite(
            Effects.BecomeCreature(
                target = EffectTarget.Self,
                power = 3,
                toughness = 3,
                duration = Duration.Permanent,
            ),
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
        )
        description = "Exhaust — {4}: This Vehicle becomes an artifact creature. Put a +1/+1 counter on it."
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Samuel Perin"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67d2d713-8acb-4e3d-bd1d-0416fe9b9ef6.jpg?1783907906"
        ruling("2025-02-07", "Exhaust abilities can be activated any time you could normally activate an ability.")
        ruling(
            "2025-02-07",
            "If an exhaust ability of a permanent is activated, and then that permanent leaves the " +
                "battlefield and returns to the battlefield, it becomes a new object so its exhaust " +
                "ability can be activated again."
        )
        ruling(
            "2025-02-07",
            "If an ability triggers whenever you activate an exhaust ability, that ability resolves " +
                "before the exhaust ability resolves."
        )
    }
}
