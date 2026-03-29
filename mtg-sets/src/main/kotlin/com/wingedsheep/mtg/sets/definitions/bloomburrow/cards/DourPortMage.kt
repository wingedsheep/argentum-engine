package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Dour Port-Mage
 * {1}{U}
 * Creature — Frog Wizard
 * 1/3
 *
 * Whenever one or more other creatures you control leave the battlefield
 * without dying, draw a card.
 * {1}{U}, {T}: Return another target creature you control to its owner's hand.
 */
val DourPortMage = card("Dour Port-Mage") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Frog Wizard"
    power = 1
    toughness = 3
    oracleText = "Whenever one or more other creatures you control leave the battlefield without dying, draw a card.\n" +
        "{1}{U}, {T}: Return another target creature you control to its owner's hand."

    // Whenever one or more other creatures you control leave the battlefield without dying, draw a card.
    triggeredAbility {
        trigger = Triggers.OneOrMoreLeaveWithoutDying(excludeSelf = true)
        effect = Effects.DrawCards(1)
    }

    // {1}{U}, {T}: Return another target creature you control to its owner's hand.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{U}"),
            Costs.Tap
        )
        val creature = target("another creature you control", TargetCreature(filter = TargetFilter.OtherCreatureYouControl))
        effect = Effects.ReturnToHand(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Ryan Pancoast"
        flavorText = "\"Find your mooring and stop clogging the shipping lanes!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/4/6402133e-eed1-4a46-9667-8b7a310362c1.jpg?1721426066"

        ruling("2024-07-26", "Dour Port-Mage's first ability triggers whenever one or more creatures you control go to any zone other than the graveyard from the battlefield. They could be returned to your hand, your library, or exiled, for example.")
        ruling("2024-07-26", "If Dour-Port Mage leaves the battlefield at the same time one or more other creatures you control leave the battlefield without dying, its first ability will still trigger.")
    }
}
