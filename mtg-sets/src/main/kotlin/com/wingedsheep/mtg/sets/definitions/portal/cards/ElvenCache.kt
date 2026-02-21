package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.core.Zone

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
        val t = target("target", Targets.CardInGraveyard)
        effect = MoveToZoneEffect(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Randy Gallegos"
        flavorText = "The elves never forget where they buried their treasures."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68939020-eb6a-4d77-a850-4df96cf01918.jpg"
    }
}
