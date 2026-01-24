package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.ReturnFromGraveyardEffect
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.targeting.GraveyardCardFilter
import com.wingedsheep.sdk.targeting.TargetCardInGraveyard

/**
 * Raise Dead
 * {B}
 * Sorcery
 * Return target creature card from your graveyard to your hand.
 */
val RaiseDead = card("Raise Dead") {
    manaCost = "{B}"
    typeLine = "Sorcery"

    spell {
        target = TargetCardInGraveyard(filter = GraveyardCardFilter.CreatureInYourGraveyard)
        effect = ReturnFromGraveyardEffect(
            filter = CardFilter.CreatureCard,
            destination = SearchDestination.HAND
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Jeff A. Menges"
        flavorText = "\"The dead serve as well as the living.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4e5f6a7-8b9c-0d1e-2f3a-4b5c6d7e8f9a.jpg"
    }
}
