package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddCombatPhaseEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Aggravated Assault
 * {2}{R}
 * Enchantment
 * {3}{R}{R}: Untap all creatures you control. After this main phase, there is an
 * additional combat phase followed by an additional main phase. Activate only as a sorcery.
 */
val AggravatedAssault = card("Aggravated Assault") {
    manaCost = "{2}{R}"
    typeLine = "Enchantment"
    oracleText = "{3}{R}{R}: Untap all creatures you control. After this main phase, there is an additional combat phase followed by an additional main phase. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Mana("{3}{R}{R}")
        timing = TimingRule.SorcerySpeed
        effect = CompositeEffect(
            listOf(
                ForEachInGroupEffect(GroupFilter.AllCreaturesYouControl, TapUntapEffect(EffectTarget.Self, tap = false)),
                AddCombatPhaseEffect
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "185"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c99c5707-d5f2-4675-bfca-e801e6b0f627.jpg?1562942627"
    }
}
