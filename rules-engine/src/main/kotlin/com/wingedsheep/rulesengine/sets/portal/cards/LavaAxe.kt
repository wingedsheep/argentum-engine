package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DealDamageEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object LavaAxe {
    val definition = CardDefinition.sorcery(
        name = "Lava Axe",
        manaCost = ManaCost.parse("{4}{R}"),
        oracleText = "Lava Axe deals 5 damage to target player or planeswalker."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "137",
            rarity = Rarity.COMMON,
            artist = "Adrian Smith",
            flavorText = "Swing your axe as a broom, to sweep away the foe.",
            imageUri = "https://cards.scryfall.io/normal/front/f/2/f2bebbad-76aa-4388-891a-583e8af9509d.jpg",
            scryfallId = "f2bebbad-76aa-4388-891a-583e8af9509d",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Lava Axe") {
        spell(DealDamageEffect(5, EffectTarget.Opponent))
    }
}
