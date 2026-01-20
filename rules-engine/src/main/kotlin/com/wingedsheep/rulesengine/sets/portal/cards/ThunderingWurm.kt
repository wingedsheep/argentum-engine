package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.SacrificeUnlessDiscardEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object ThunderingWurm {
    val definition = CardDefinition.creature(
        name = "Thundering Wurm",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype.WURM),
        power = 4,
        toughness = 4,
        oracleText = "When Thundering Wurm enters, sacrifice it unless you discard a land card."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "189",
            rarity = Rarity.RARE,
            artist = "Paolo Parente",
            imageUri = "https://cards.scryfall.io/normal/front/8/b/8b0ba623-d17f-4f0e-b914-da139a3971df.jpg",
            scryfallId = "8b0ba623-d17f-4f0e-b914-da139a3971df",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Thundering Wurm") {
        triggered(
            trigger = OnEnterBattlefield(),
            effect = SacrificeUnlessDiscardEffect(landOnly = true)
        )
    }
}
