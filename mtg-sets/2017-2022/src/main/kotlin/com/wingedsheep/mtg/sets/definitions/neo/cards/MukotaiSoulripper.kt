package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mukotai Soulripper — Kamigawa: Neon Dynasty #113 (canonical printing)
 * {1}{B} · Artifact — Vehicle · 4/3
 *
 * Whenever this Vehicle attacks, you may sacrifice another artifact or creature. If you do,
 * put a +1/+1 counter on this Vehicle and it gains menace until end of turn.
 * Crew 2
 *
 * The sacrifice is the optional cost of an [Effects.MayPay] (`excludeSource` enforces
 * "another"); the counter and menace are the reward for paying it. The sacrifice is chosen on
 * resolution, so a creature that crewed the Vehicle this turn is a legal choice.
 */
val MukotaiSoulripper = card("Mukotai Soulripper") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 3
    oracleText = "Whenever this Vehicle attacks, you may sacrifice another artifact or creature. " +
        "If you do, put a +1/+1 counter on this Vehicle and it gains menace until end of turn.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This " +
        "Vehicle becomes an artifact creature until end of turn.)"

    keywordAbility(KeywordAbility.crew(2))

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            cost = Effects.SacrificeOwn(
                filter = GameObjectFilter.CreatureOrArtifact,
                count = 1,
                excludeSource = true,
            ),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
                Effects.GrantKeyword(Keyword.MENACE, EffectTarget.Self),
        )
        description = "Whenever this Vehicle attacks, you may sacrifice another artifact or creature. " +
            "If you do, put a +1/+1 counter on this Vehicle and it gains menace until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Eddie Mendoza"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/166775be-c3b7-4a17-9376-aa4229b147f9.jpg?1783923879"
    }
}
