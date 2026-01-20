package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CantBlock
import com.wingedsheep.rulesengine.ability.StaticTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object HulkingCyclops {
    val definition = CardDefinition.creature(
        name = "Hulking Cyclops",
        manaCost = ManaCost.parse("{3}{R}{R}"),
        subtypes = setOf(Subtype.CYCLOPS),
        power = 5,
        toughness = 5,
        oracleText = "Hulking Cyclops can't block.",
        metadata = ScryfallMetadata(
            collectorNumber = "134",
            rarity = Rarity.UNCOMMON,
            artist = "Paolo Parente",
            flavorText = "Anyone can get around a cyclops, but few can stand in its way.",
            imageUri = "https://cards.scryfall.io/normal/front/f/2/f20ae982-8a70-4dd3-8254-0d447954f580.jpg",
            scryfallId = "f20ae982-8a70-4dd3-8254-0d447954f580",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Hulking Cyclops") {
        staticAbility(CantBlock(StaticTarget.SourceCreature))
    }
}
