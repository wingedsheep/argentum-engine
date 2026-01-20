package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DrawCardsEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost

object CloakOfFeathers {
    val definition = CardDefinition.sorcery(
        name = "Cloak of Feathers",
        manaCost = ManaCost.parse("{U}"),
        oracleText = "Target creature gains flying until end of turn.\nDraw a card."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "44",
            rarity = Rarity.COMMON,
            artist = "Rebecca Guay",
            flavorText = "A thousand feathers from a thousand birds, sewn with magic and song.",
            imageUri = "https://cards.scryfall.io/normal/front/9/7/9746790c-a426-4135-8c9d-afb82a0c26b8.jpg",
            scryfallId = "9746790c-a426-4135-8c9d-afb82a0c26b8",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Cloak of Feathers") {
        spell(
            GrantKeywordUntilEndOfTurnEffect(Keyword.FLYING, EffectTarget.TargetCreature) then
            DrawCardsEffect(1)
        )
    }
}
