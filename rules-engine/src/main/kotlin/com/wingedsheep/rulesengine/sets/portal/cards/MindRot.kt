package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DiscardCardsEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object MindRot {
    val definition = CardDefinition.sorcery(
        name = "Mind Rot",
        manaCost = ManaCost.parse("{2}{B}"),
        oracleText = "Target player discards two cards."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "101",
            rarity = Rarity.COMMON,
            artist = "Steve Luke",
            imageUri = "https://cards.scryfall.io/normal/front/b/9/b91d355d-8409-4f0b-87ce-7590a8b9ebc0.jpg",
            scryfallId = "b91d355d-8409-4f0b-87ce-7590a8b9ebc0",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Mind Rot") {
        spell(DiscardCardsEffect(2, EffectTarget.Opponent))
    }
}
