package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DestroyEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnDeath
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object FireSnake {
    val definition = CardDefinition.creature(
        name = "Fire Snake",
        manaCost = ManaCost.parse("{4}{R}"),
        subtypes = setOf(Subtype.SERPENT),
        power = 3,
        toughness = 1,
        oracleText = "When Fire Snake dies, destroy target land.",
        metadata = ScryfallMetadata(
            collectorNumber = "127",
            rarity = Rarity.COMMON,
            artist = "Steve Luke",
            flavorText = "The snake's final thrashings only spread the fire within it.",
            imageUri = "https://cards.scryfall.io/normal/front/d/4/d4c36e32-59e8-4e3d-903e-a264211f2a82.jpg",
            scryfallId = "d4c36e32-59e8-4e3d-903e-a264211f2a82",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Fire Snake") {
        triggered(
            trigger = OnDeath(),
            effect = DestroyEffect(EffectTarget.TargetLand)
        )
    }
}
