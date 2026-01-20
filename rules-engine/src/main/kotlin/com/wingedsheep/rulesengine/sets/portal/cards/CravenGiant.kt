package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CantBlock
import com.wingedsheep.rulesengine.ability.StaticTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object CravenGiant {
    val definition = CardDefinition.creature(
        name = "Craven Giant",
        manaCost = ManaCost.parse("{2}{R}"),
        subtypes = setOf(Subtype.GIANT),
        power = 4,
        toughness = 1,
        oracleText = "Craven Giant can't block.",
        metadata = ScryfallMetadata(
            collectorNumber = "121",
            rarity = Rarity.COMMON,
            artist = "Ron Spencer",
            flavorText = "\"The best armor is to keep out of range.\" —Italian proverb",
            imageUri = "https://cards.scryfall.io/normal/front/4/a/4a2e1c12-f848-43b4-9505-851c66a509f1.jpg",
            scryfallId = "4a2e1c12-f848-43b4-9505-851c66a509f1",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Craven Giant") {
        staticAbility(CantBlock(StaticTarget.SourceCreature))
    }
}
