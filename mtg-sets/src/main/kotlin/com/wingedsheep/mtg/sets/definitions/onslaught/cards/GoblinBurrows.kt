package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Goblin Burrows
 * Land
 * {T}: Add {C}.
 * {1}{R}, {T}: Target Goblin creature can't be blocked this turn.
 */
val GoblinBurrows = card("Goblin Burrows") {
    typeLine = "Land"

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
        effect = GrantKeywordUntilEndOfTurnEffect(Keyword.CANT_BE_BLOCKED, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "318"
        artist = "David Martin"
        imageUri = "https://cards.scryfall.io/large/front/a/5/a5064cd2-8762-4e08-8c3c-be6f31e9ab61.jpg?1562933960"
    }
}
