package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object SpinedWurm {
    val definition = CardDefinition.creature(
        name = "Spined Wurm",
        manaCost = ManaCost.parse("{4}{G}"),
        subtypes = setOf(Subtype.WURM),
        power = 5,
        toughness = 4,
        metadata = ScryfallMetadata(
            collectorNumber = "185",
            rarity = Rarity.COMMON,
            artist = "Colin MacNeil",
            flavorText = "It has more teeth than fit in its mouth.",
            imageUri = "https://cards.scryfall.io/normal/front/0/0/0053bd00-90fd-48c2-8f79-952d5d1e3e74.jpg",
            scryfallId = "0053bd00-90fd-48c2-8f79-952d5d1e3e74",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Spined Wurm") { }
}
