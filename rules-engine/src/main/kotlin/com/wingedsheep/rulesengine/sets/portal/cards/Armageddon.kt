package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DestroyAllLandsEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object Armageddon {
    val definition = CardDefinition.sorcery(
        name = "Armageddon",
        manaCost = ManaCost.parse("{3}{W}"),
        oracleText = "Destroy all lands."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "5",
            rarity = Rarity.RARE,
            artist = "John Avon",
            flavorText = "\"O miserable of happy! Is this the end Of this new glorious world . . . ?\" —John Milton, Paradise Lost",
            imageUri = "https://cards.scryfall.io/normal/front/2/0/2073ca8b-2bca-4539-94d7-989da157e4b8.jpg",
            scryfallId = "2073ca8b-2bca-4539-94d7-989da157e4b8",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Armageddon") {
        spell(DestroyAllLandsEffect)
    }
}
