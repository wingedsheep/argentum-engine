package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.dsl.Effects

/**
 * Riptide Laboratory
 * Land
 * {T}: Add {C}.
 * {1}{U}, {T}: Return target Wizard you control to its owner's hand.
 */
val RiptideLaboratory = card("Riptide Laboratory") {
    typeLine = "Land"
    colorIdentity = "U"
    oracleText = "{T}: Add {C}.\n{1}{U}, {T}: Return target Wizard you control to its owner's hand."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Tap)
        val t = target("target", TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Wizard").youControl())
        ))
        effect = Effects.Move(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "322"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d993c973-2eb6-423c-8ee9-10749a751524.jpg?1562946819"
    }
}
