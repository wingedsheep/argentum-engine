package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DrawCardsEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ReturnToHandEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object SymbolOfUnsummoning {
    val definition = CardDefinition.sorcery(
        name = "Symbol of Unsummoning",
        manaCost = ManaCost.parse("{2}{U}"),
        oracleText = "Return target creature to its owner's hand.\nDraw a card."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "70",
            rarity = Rarity.COMMON,
            artist = "Adam Rex",
            flavorText = ". . . inviting the soul to wander for a spell in abysses of solitude . . . .",
            imageUri = "https://cards.scryfall.io/normal/front/5/5/55811106-9f30-4e34-924e-2c9401b49574.jpg",
            scryfallId = "55811106-9f30-4e34-924e-2c9401b49574",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Symbol of Unsummoning") {
        spell(
            ReturnToHandEffect(EffectTarget.TargetCreature) then
            DrawCardsEffect(1)
        )
    }
}
