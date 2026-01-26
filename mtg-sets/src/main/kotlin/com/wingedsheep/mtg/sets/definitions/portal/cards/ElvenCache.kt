package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.ReturnFromGraveyardEffect
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.targeting.GraveyardCardFilter
import com.wingedsheep.sdk.targeting.TargetCardInGraveyard

/**
 * Elven Cache
 * {2}{G}{G}
 * Sorcery
 * Return target card from your graveyard to your hand.
 */
val ElvenCache = card("Elven Cache") {
    manaCost = "{2}{G}{G}"
    typeLine = "Sorcery"

    spell {
        target = TargetCardInGraveyard(filter = GraveyardCardFilter.Any)
        effect = ReturnFromGraveyardEffect(
            filter = CardFilter.AnyCard,
            destination = SearchDestination.HAND
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Randy Gallegos"
        flavorText = "The elves never forget where they buried their treasures."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68939020-eb6a-4d77-a850-4df96cf01918.jpg"
    }
}
