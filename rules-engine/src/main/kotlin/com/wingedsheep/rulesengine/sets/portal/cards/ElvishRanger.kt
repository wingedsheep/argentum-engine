package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object ElvishRanger {
    val definition = CardDefinition.creature(
        name = "Elvish Ranger",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype.ELF, Subtype.of("Ranger")),
        power = 4,
        toughness = 1,
        oracleText = ""
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "165",
            rarity = Rarity.COMMON,
            artist = "DiTerlizzi",
            flavorText = "It's up to you if you enter their woods—and up to them if you leave.",
            imageUri = "https://cards.scryfall.io/normal/front/2/6/26caff65-3a96-46f2-8f0b-e5091b632a2e.jpg",
            scryfallId = "26caff65-3a96-46f2-8f0b-e5091b632a2e",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Elvish Ranger") {
        // Vanilla creature - no abilities
    }
}
