package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnDeath
import com.wingedsheep.rulesengine.ability.ShuffleIntoLibraryEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object AlabasterDragon {
    val definition = CardDefinition.creature(
        name = "Alabaster Dragon",
        manaCost = ManaCost.parse("{4}{W}{W}"),
        subtypes = setOf(Subtype.DRAGON),
        power = 4,
        toughness = 4,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying\nWhen Alabaster Dragon dies, shuffle it into its owner's library.",
        metadata = ScryfallMetadata(
            collectorNumber = "1",
            rarity = Rarity.RARE,
            artist = "Ted Naifeh",
            imageUri = "https://cards.scryfall.io/normal/front/1/e/1edc6ec1-3b34-45e0-8573-39eba1d10efa.jpg",
            scryfallId = "1edc6ec1-3b34-45e0-8573-39eba1d10efa",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Alabaster Dragon") {
        keywords(Keyword.FLYING)
        triggered(
            trigger = OnDeath(),
            effect = ShuffleIntoLibraryEffect(EffectTarget.Self)
        )
    }
}
