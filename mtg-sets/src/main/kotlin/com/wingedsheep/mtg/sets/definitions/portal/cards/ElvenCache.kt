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
        imageUri = "https://cards.scryfall.io/normal/front/0/2/023e40fb-91aa-4b88-ae2f-0e7192f5b8f3.jpg"
    }
}
