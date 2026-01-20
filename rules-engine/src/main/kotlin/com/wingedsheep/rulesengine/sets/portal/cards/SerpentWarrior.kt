package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.LoseLifeEffect
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object SerpentWarrior {
    val definition = CardDefinition.creature(
        name = "Serpent Warrior",
        manaCost = ManaCost.parse("{2}{B}"),
        subtypes = setOf(Subtype.SERPENT, Subtype.WARRIOR),
        power = 3,
        toughness = 3,
        oracleText = "When Serpent Warrior enters the battlefield, you lose 3 life.",
        metadata = ScryfallMetadata(
            collectorNumber = "109",
            rarity = Rarity.COMMON,
            artist = "Roger Raupp",
            imageUri = "https://cards.scryfall.io/normal/front/c/3/c364fd06-64c5-45f6-8ed5-64f44a1e8bda.jpg",
            scryfallId = "c364fd06-64c5-45f6-8ed5-64f44a1e8bda",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Serpent Warrior") {
        triggered(
            trigger = OnEnterBattlefield(),
            effect = LoseLifeEffect(3, EffectTarget.Controller)
        )
    }
}
