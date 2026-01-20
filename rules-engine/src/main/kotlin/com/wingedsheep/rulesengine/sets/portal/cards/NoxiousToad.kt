package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DiscardCardsEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnDeath
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object NoxiousToad {
    val definition = CardDefinition.creature(
        name = "Noxious Toad",
        manaCost = ManaCost.parse("{2}{B}"),
        subtypes = setOf(Subtype.FROG),
        power = 1,
        toughness = 1,
        oracleText = "When Noxious Toad dies, each opponent discards a card.",
        metadata = ScryfallMetadata(
            collectorNumber = "104",
            rarity = Rarity.UNCOMMON,
            artist = "Adrian Smith",
            imageUri = "https://cards.scryfall.io/normal/front/b/5/b5ec75ba-bae2-4ccc-b18b-ad4639cfb548.jpg",
            scryfallId = "b5ec75ba-bae2-4ccc-b18b-ad4639cfb548",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Noxious Toad") {
        triggered(
            trigger = OnDeath(),
            effect = DiscardCardsEffect(1, EffectTarget.EachOpponent)
        )
    }
}
