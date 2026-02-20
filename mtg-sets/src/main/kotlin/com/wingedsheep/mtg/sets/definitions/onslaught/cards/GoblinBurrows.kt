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
 * Goblin Burrows
 * Land
 * {T}: Add {C}.
 * {1}{R}, {T}: Target Goblin creature gets +2/+0 until end of turn.
 */
val GoblinBurrows = card("Goblin Burrows") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{1}{R}, {T}: Target Goblin creature gets +2/+0 until end of turn."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.Tap)
        target = TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Goblin"))
        )
        effect = ModifyStatsEffect(2, 0, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "318"
        artist = "David Martin"
        imageUri = "https://cards.scryfall.io/large/front/a/5/a5064cd2-8762-4e08-8c3c-be6f31e9ab61.jpg?1562933960"
    }
}
