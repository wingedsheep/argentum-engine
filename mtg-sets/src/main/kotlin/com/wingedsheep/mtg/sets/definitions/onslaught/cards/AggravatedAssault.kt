package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AddCombatPhaseEffect
import com.wingedsheep.sdk.scripting.CompositeEffect
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.UntapAllCreaturesYouControlEffect

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

    activatedAbility {
        cost = Costs.Mana("{3}{R}{R}")
        timing = TimingRule.SorcerySpeed
        effect = CompositeEffect(
            listOf(
                UntapAllCreaturesYouControlEffect,
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
