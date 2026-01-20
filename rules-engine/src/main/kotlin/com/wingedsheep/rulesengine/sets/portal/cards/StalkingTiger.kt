package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.MaxBlockersRestriction
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object StalkingTiger {
    val definition = CardDefinition.creature(
        name = "Stalking Tiger",
        manaCost = ManaCost.parse("{3}{G}"),
        subtypes = setOf(Subtype.CAT),
        power = 3,
        toughness = 3,
        oracleText = "Stalking Tiger can't be blocked by more than one creature."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "186",
            rarity = Rarity.COMMON,
            artist = "Colin MacNeil",
            flavorText = "No one is happy when they notice a tiger.",
            imageUri = "https://cards.scryfall.io/normal/front/c/b/cbc78337-2d1a-4a1d-8630-fcf7a7f6abce.jpg",
            scryfallId = "cbc78337-2d1a-4a1d-8630-fcf7a7f6abce",
            releaseDate = "1997-05-01"
        )
    )

    // Note: "Can't be blocked by more than one creature" is different from menace.
    // Menace requires at least two blockers; this limits to at most one.
    val script = cardScript("Stalking Tiger") {
        staticAbility(MaxBlockersRestriction(1))
    }
}
