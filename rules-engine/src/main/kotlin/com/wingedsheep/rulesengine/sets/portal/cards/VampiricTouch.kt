package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DrainEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object VampiricTouch {
    val definition = CardDefinition.sorcery(
        name = "Vampiric Touch",
        manaCost = ManaCost.parse("{2}{B}"),
        oracleText = "Vampiric Touch deals 2 damage to target opponent or planeswalker and you gain 2 life."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "115",
            rarity = Rarity.COMMON,
            artist = "Zina Saunders",
            flavorText = "A touch, not comforting, but of death.",
            imageUri = "https://cards.scryfall.io/normal/front/2/3/231f7598-8c47-4828-8240-e2a545a7ac5b.jpg",
            scryfallId = "231f7598-8c47-4828-8240-e2a545a7ac5b",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Vampiric Touch") {
        spell(DrainEffect(2, EffectTarget.Opponent))
    }
}
