package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lattice-Blade Mantis
 * {3}{G}
 * Creature — Phyrexian Insect
 * 4/3
 *
 * This creature enters with two oil counters on it.
 * Whenever this creature attacks, you may remove an oil counter from it. If you do, untap it and it
 * gets +1/+1 until end of turn.
 *
 * "If you do" gates on an oil counter actually coming off (`CountersRemoved`), so accepting with no
 * oil counters left gives neither the untap nor the pump.
 */
val LatticeBladeMantis = card("Lattice-Blade Mantis") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Phyrexian Insect"
    power = 4
    toughness = 3
    oracleText = "This creature enters with two oil counters on it.\n" +
        "Whenever this creature attacks, you may remove an oil counter from it. If you do, untap it " +
        "and it gets +1/+1 until end of turn."

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 2, selfOnly = true))

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.RemoveCounters(CounterType.OIL, 1, EffectTarget.Self),
                then = Effects.Untap(EffectTarget.Self) then
                    Effects.ModifyStats(1, 1, EffectTarget.Self),
                successCriterion = SuccessCriterion.CountersRemoved,
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Durion"
        flavorText = "\"Urabrask doesn't think big enough.\"\n—Glissa Sunslayer"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7427def-c4b2-475a-8dc9-7e89409d9abb.jpg?1783918014"
    }
}
