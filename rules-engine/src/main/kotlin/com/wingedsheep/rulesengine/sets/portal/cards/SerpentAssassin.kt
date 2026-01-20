package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DestroyEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object SerpentAssassin {
    val definition = CardDefinition.creature(
        name = "Serpent Assassin",
        manaCost = ManaCost.parse("{3}{B}{B}"),
        subtypes = setOf(Subtype.SERPENT, Subtype.ASSASSIN),
        power = 2,
        toughness = 2,
        oracleText = "When Serpent Assassin enters the battlefield, you may destroy target nonblack creature.",
        metadata = ScryfallMetadata(
            collectorNumber = "108",
            rarity = Rarity.RARE,
            artist = "Roger Raupp",
            imageUri = "https://cards.scryfall.io/normal/front/1/0/1018f6ff-5eaa-4fe1-ba20-544df799f5b2.jpg",
            scryfallId = "1018f6ff-5eaa-4fe1-ba20-544df799f5b2",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Serpent Assassin") {
        triggered(
            trigger = OnEnterBattlefield(),
            effect = DestroyEffect(EffectTarget.TargetNonblackCreature)
        )
    }
}
