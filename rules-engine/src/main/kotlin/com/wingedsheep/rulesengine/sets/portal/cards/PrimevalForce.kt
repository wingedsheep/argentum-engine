package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.SacrificeCost
import com.wingedsheep.rulesengine.ability.SacrificeUnlessEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object PrimevalForce {
    val definition = CardDefinition.creature(
        name = "Primeval Force",
        manaCost = ManaCost.parse("{2}{G}{G}{G}"),
        subtypes = setOf(Subtype.ELEMENTAL),
        power = 8,
        toughness = 8,
        oracleText = "When Primeval Force enters the battlefield, sacrifice it unless you sacrifice three Forests.",
        metadata = ScryfallMetadata(
            collectorNumber = "182",
            rarity = Rarity.RARE,
            artist = "Randy Gallegos",
            imageUri = "https://cards.scryfall.io/normal/front/1/c/1ce7fc51-0ed8-49d9-bdba-ba5a89e1e852.jpg",
            scryfallId = "1ce7fc51-0ed8-49d9-bdba-ba5a89e1e852",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Primeval Force") {
        triggered(
            trigger = OnEnterBattlefield(),
            effect = SacrificeUnlessEffect(
                permanentToSacrifice = EffectTarget.Self,
                cost = SacrificeCost(
                    filter = CardFilter.HasSubtype("Forest"),
                    count = 3
                )
            )
        )
    }
}
