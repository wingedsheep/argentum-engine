package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnDeath
import com.wingedsheep.rulesengine.ability.PutOnTopOfLibraryEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object UndyingBeast {
    val definition = CardDefinition.creature(
        name = "Undying Beast",
        manaCost = ManaCost.parse("{3}{B}"),
        subtypes = setOf(Subtype.BEAST),
        power = 3,
        toughness = 2,
        oracleText = "When Undying Beast dies, put it on top of its owner's library.",
        metadata = ScryfallMetadata(
            collectorNumber = "113",
            rarity = Rarity.COMMON,
            artist = "Steve Luke",
            imageUri = "https://cards.scryfall.io/normal/front/9/c/9c95c752-3add-4830-8159-036b8689f40a.jpg",
            scryfallId = "9c95c752-3add-4830-8159-036b8689f40a",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Undying Beast") {
        triggered(
            trigger = OnDeath(),
            effect = PutOnTopOfLibraryEffect(EffectTarget.Self)
        )
    }
}
