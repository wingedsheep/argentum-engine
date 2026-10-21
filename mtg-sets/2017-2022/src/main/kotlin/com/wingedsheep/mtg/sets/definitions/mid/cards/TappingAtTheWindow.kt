package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Tapping at the Window
 * {1}{G}
 * Sorcery
 * Look at the top three cards of your library. You may reveal a creature card from among them and
 * put it into your hand. Put the rest into your graveyard.
 * Flashback {2}{G}
 */
val TappingAtTheWindow = card("Tapping at the Window") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top three cards of your library. You may reveal a creature card from among them and put it into your hand. Put the rest into your graveyard.\n" +
        "Flashback {2}{G} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 3,
            filter = GameObjectFilter.Creature,
            prompt = "You may reveal a creature card from among them and put it into your hand",
            restDestination = CardDestination.ToZone(Zone.GRAVEYARD),
            restOrder = CardOrder.Preserve,
        )
    }

    keywordAbility(KeywordAbility.flashback("{2}{G}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "201"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba75dc52-7ab2-43ec-afac-0a9bcf24cde9.jpg?1783925573"
    }
}
