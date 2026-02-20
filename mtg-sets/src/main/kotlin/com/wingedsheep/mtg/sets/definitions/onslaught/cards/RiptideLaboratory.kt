package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Riptide Laboratory
 * Land
 * {T}: Add {C}.
 * {1}{U}, {T}: Return target Wizard you control to its owner's hand.
 */
val RiptideLaboratory = card("Riptide Laboratory") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{1}{U}, {T}: Return target Wizard you control to its owner's hand."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Tap)
        target = TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Wizard").youControl())
        )
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.HAND)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "322"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/large/front/d/9/d993c973-2eb6-423c-8ee9-10749a751524.jpg?1562946819"
    }
}
