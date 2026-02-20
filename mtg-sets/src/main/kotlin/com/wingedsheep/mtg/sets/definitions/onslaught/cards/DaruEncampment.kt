package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Daru Encampment
 * Land
 * {T}: Add {C}.
 * {W}, {T}: Target Soldier creature gets +1/+1 until end of turn.
 */
val DaruEncampment = card("Daru Encampment") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{W}, {T}: Target Soldier creature gets +1/+1 until end of turn."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        target = TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Soldier"))
        )
        effect = ModifyStatsEffect(1, 1, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "315"
        artist = "Tony Szczudlo"
        imageUri = "https://cards.scryfall.io/large/front/c/5/c5869f08-fac8-44b6-8142-7d7ecccab414.jpg?1562941659"
    }
}
