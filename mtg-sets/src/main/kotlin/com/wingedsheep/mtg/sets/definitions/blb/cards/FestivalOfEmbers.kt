package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Festival of Embers
 * {4}{R}
 * Enchantment
 *
 * During your turn, you may cast instant and sorcery spells from your graveyard
 * by paying 1 life in addition to their other costs.
 * If a card or token would be put into your graveyard from anywhere, exile it instead.
 * {1}{R}: Sacrifice this enchantment.
 */
val FestivalOfEmbers = card("Festival of Embers") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "During your turn, you may cast instant and sorcery spells from your graveyard by paying 1 life in addition to their other costs.\nIf a card or token would be put into your graveyard from anywhere, exile it instead.\n{1}{R}: Sacrifice this enchantment."

    // During your turn, you may cast instant and sorcery spells from your graveyard
    // by paying 1 life in addition to their other costs.
    staticAbility {
        ability = MayCastFromGraveyard(
            filter = GameObjectFilter.InstantOrSorcery,
            lifeCost = 1,
            duringYourTurnOnly = true
        )
    }

    // If a card or token would be put into your graveyard from anywhere, exile it instead.
    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter(
                    controllerPredicate = ControllerPredicate.OwnedByYou
                ),
                to = Zone.GRAVEYARD
            )
        )
    )

    // {1}{R}: Sacrifice this enchantment.
    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = SacrificeSelfEffect
        description = "{1}{R}: Sacrifice this enchantment."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "134"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/4433ee12-2013-4fdc-979f-ae065f63a527.jpg?1721426613"
        ruling("2024-07-26", "While you control Festival of Embers, abilities that trigger whenever a creature you own dies won't trigger because those cards and tokens are never put into your graveyard.")
        ruling("2024-07-26", "If Festival of Embers is destroyed by a spell, Festival of Embers will be exiled and then the spell will be put into its owner's graveyard.")
        ruling("2024-07-26", "If you discard a card while you control Festival of Embers, abilities that function when that card is discarded (such as madness) still work even though that card never reaches a graveyard.")
    }
}
