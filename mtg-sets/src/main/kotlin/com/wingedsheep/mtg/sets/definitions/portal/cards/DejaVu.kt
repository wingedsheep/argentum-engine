package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReturnFromGraveyardEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.scripting.Zone
import com.wingedsheep.sdk.targeting.TargetCardInGraveyard

/**
 * Déjà Vu
 * {2}{U}
 * Sorcery
 * Return target sorcery card from your graveyard to your hand.
 */
val DejaVu = card("Déjà Vu") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"

    spell {
        target = TargetCardInGraveyard(
            filter = TargetFilter(GameObjectFilter.Sorcery.ownedByYou(), zone = Zone.Graveyard)
        )
        effect = ReturnFromGraveyardEffect()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Hannibal King"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c93d4e9-7fd6-4814-b86b-89b92d1dad3b.jpg"
    }
}
