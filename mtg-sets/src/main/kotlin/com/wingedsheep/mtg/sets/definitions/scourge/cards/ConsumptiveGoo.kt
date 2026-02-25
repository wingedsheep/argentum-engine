package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Consumptive Goo
 * {B}{B}
 * Creature — Ooze
 * 1/1
 * {2}{B}{B}: Target creature gets -1/-1 until end of turn. Put a +1/+1 counter on Consumptive Goo.
 */
val ConsumptiveGoo = card("Consumptive Goo") {
    manaCost = "{B}{B}"
    typeLine = "Creature — Ooze"
    power = 1
    toughness = 1
    oracleText = "{2}{B}{B}: Target creature gets -1/-1 until end of turn. Put a +1/+1 counter on Consumptive Goo."

    activatedAbility {
        cost = AbilityCost.Mana(ManaCost.parse("{2}{B}{B}"))
        val t = target("target creature", Targets.Creature)
        effect = ModifyStatsEffect(
            powerModifier = -1,
            toughnessModifier = -1,
            target = t,
            duration = Duration.EndOfTurn
        ).then(
            AddCountersEffect(
                counterType = "+1/+1",
                count = 1,
                target = EffectTarget.Self
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Carl Critchlow"
        flavorText = "Silent as fog and relentless as plague, it is wet, creeping death."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f0f549f-6607-483a-9d89-2019ca9ef571.jpg?1562525434"
        ruling("2004-10-04", "If it targets itself, it gets both -1/-1 and the +1/+1 counter during resolution. Since state-based actions (like being at zero toughness) are not checked until resolution is done, it does not die.")
    }
}
