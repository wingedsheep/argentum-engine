package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.MaxBlockersRestriction
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object ChargingRhino {
    val definition = CardDefinition.creature(
        name = "Charging Rhino",
        manaCost = ManaCost.parse("{3}{G}{G}"),
        subtypes = setOf(Subtype.RHINO),
        power = 4,
        toughness = 4,
        oracleText = "Charging Rhino can't be blocked by more than one creature."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "161",
            rarity = Rarity.RARE,
            artist = "Una Fricker",
            flavorText = "A rhino rarely uses its horn to announce its charge, only to end it.",
            imageUri = "https://cards.scryfall.io/normal/front/4/9/49e47248-051c-4ee6-aad2-352ebd1f38ca.jpg",
            scryfallId = "49e47248-051c-4ee6-aad2-352ebd1f38ca",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Charging Rhino") {
        staticAbility(MaxBlockersRestriction(1))
    }
}
