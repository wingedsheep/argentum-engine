package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DrainEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object VampiricFeast {
    val definition = CardDefinition.sorcery(
        name = "Vampiric Feast",
        manaCost = ManaCost.parse("{5}{B}"),
        oracleText = "Vampiric Feast deals 4 damage to any target and you gain 4 life."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "114",
            rarity = Rarity.UNCOMMON,
            artist = "D. Alexander Gregory",
            flavorText = "It's not always gold the thief is after.",
            imageUri = "https://cards.scryfall.io/normal/front/1/9/19500ffb-bfad-46d6-8a6e-d134405959c0.jpg",
            scryfallId = "19500ffb-bfad-46d6-8a6e-d134405959c0",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Vampiric Feast") {
        spell(DrainEffect(4, EffectTarget.AnyTarget))
    }
}
