package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object CoralEel {
    val definition = CardDefinition.creature(
        name = "Coral Eel",
        manaCost = ManaCost.parse("{1}{U}"),
        subtypes = setOf(Subtype.EEL),
        power = 2,
        toughness = 1,
        metadata = ScryfallMetadata(
            collectorNumber = "49",
            rarity = Rarity.COMMON,
            artist = "Una Fricker",
            flavorText = "Some fishers like to eat eels, and some eels like to eat fishers.",
            imageUri = "https://cards.scryfall.io/normal/front/3/5/35bbb10f-c118-4905-8329-3963af415178.jpg",
            scryfallId = "35bbb10f-c118-4905-8329-3963af415178",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Coral Eel") {
        // Vanilla creature - no abilities
    }
}
