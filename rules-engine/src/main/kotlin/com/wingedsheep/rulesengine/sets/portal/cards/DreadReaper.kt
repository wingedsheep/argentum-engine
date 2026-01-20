package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.LoseLifeEffect
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object DreadReaper {
    val definition = CardDefinition.creature(
        name = "Dread Reaper",
        manaCost = ManaCost.parse("{3}{B}{B}{B}"),
        subtypes = setOf(Subtype.HORROR),
        power = 6,
        toughness = 5,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying\nWhen Dread Reaper enters the battlefield, you lose 5 life.",
        metadata = ScryfallMetadata(
            collectorNumber = "89",
            rarity = Rarity.RARE,
            artist = "Christopher Rush",
            imageUri = "https://cards.scryfall.io/normal/front/e/b/eb25d674-11f3-42d2-ba2f-e9a5d55a7852.jpg",
            scryfallId = "eb25d674-11f3-42d2-ba2f-e9a5d55a7852",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Dread Reaper") {
        keywords(Keyword.FLYING)
        triggered(
            trigger = OnEnterBattlefield(),
            effect = LoseLifeEffect(5, EffectTarget.Controller)
        )
    }
}
