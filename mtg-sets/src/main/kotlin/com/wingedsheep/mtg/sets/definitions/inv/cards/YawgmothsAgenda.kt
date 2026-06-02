package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.RestrictSpellsCastPerTurn
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Yawgmoth's Agenda
 * {3}{B}{B}
 * Enchantment
 *
 * You can't cast more than one spell each turn.
 * You may play lands and cast spells from your graveyard.
 * If a card would be put into your graveyard from anywhere, exile it instead.
 */
val YawgmothsAgenda = card("Yawgmoth's Agenda") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "You can't cast more than one spell each turn.\nYou may play lands and cast spells from your graveyard.\nIf a card would be put into your graveyard from anywhere, exile it instead."

    // You can't cast more than one spell each turn.
    staticAbility {
        ability = RestrictSpellsCastPerTurn(maxPerTurn = 1)
    }

    // You may play lands and cast spells from your graveyard.
    staticAbility {
        ability = MayPlayLandsFromGraveyard
    }
    staticAbility {
        ability = MayCastFromGraveyard(filter = GameObjectFilter.Nonland)
    }

    // If a card would be put into your graveyard from anywhere, exile it instead.
    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter(
                    controllerPredicate = ControllerPredicate.OwnedByYou
                ),
                to = Zone.GRAVEYARD
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "135"
        artist = "Arnie Swekel"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50f7ea7f-4f17-4f78-b68e-693e265ca829.jpg?1591196215"
        ruling("2004-10-04", "The cards in your graveyard are not considered to be in your hand for any reason. For example, you can't discard them.")
        ruling("2004-10-04", "You count spells that were cast this turn before this card enters.")
        ruling("2004-10-04", "It will exile itself if it is going to the graveyard from the battlefield.")
    }
}
