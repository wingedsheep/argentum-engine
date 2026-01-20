package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.SkipUntapEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object Exhaustion {
    val definition = CardDefinition.sorcery(
        name = "Exhaustion",
        manaCost = ManaCost.parse("{2}{U}"),
        oracleText = "Creatures and lands target opponent controls don't untap during their next untap step."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "54",
            rarity = Rarity.RARE,
            artist = "DiTerlizzi",
            imageUri = "https://cards.scryfall.io/normal/front/9/d/9d6a5c33-cf74-4cec-a4f4-1aac9e7b8f79.jpg",
            scryfallId = "9d6a5c33-cf74-4cec-a4f4-1aac9e7b8f79",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Exhaustion") {
        spell(SkipUntapEffect(EffectTarget.Opponent, affectsCreatures = true, affectsLands = true))
    }
}
