package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreatureGroupFilter
import com.wingedsheep.sdk.scripting.GrantCantBeBlockedExceptByColorEffect

/**
 * Dread Charge
 * {3}{B}
 * Sorcery
 * Black creatures you control can't be blocked this turn except by black creatures.
 */
val DreadCharge = card("Dread Charge") {
    manaCost = "{3}{B}"
    typeLine = "Sorcery"

    spell {
        effect = GrantCantBeBlockedExceptByColorEffect(
            filter = CreatureGroupFilter.ColorYouControl(Color.BLACK),
            canOnlyBeBlockedByColor = Color.BLACK
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed0ca934-6989-415b-8816-7c66d22b4707.jpg"
    }
}
