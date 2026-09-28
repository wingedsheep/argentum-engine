package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Loyal Retainers
 * {2}{W}
 * Creature — Human Advisor
 * 1/1
 * Sacrifice this creature: Return target legendary creature card from your graveyard to the
 * battlefield. Activate only during your turn, before attackers are declared.
 */
val LoyalRetainers = card("Loyal Retainers") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Advisor"
    power = 1
    toughness = 1
    oracleText = "Sacrifice this creature: Return target legendary creature card from your graveyard to the battlefield. Activate only during your turn, before attackers are declared."

    activatedAbility {
        cost = Costs.SacrificeSelf
        val creature = target(TargetFilter(GameObjectFilter.Creature.legendary().ownedByYou(), zone = Zone.GRAVEYARD))
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
        )
        effect = Effects.Move(creature, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Solomon Au Yeung"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2c37964-1c0d-40e6-9947-8be04fe14427.jpg?1783946130"
    }
}
