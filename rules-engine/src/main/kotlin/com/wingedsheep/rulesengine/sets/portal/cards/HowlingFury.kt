package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object HowlingFury {
    val definition = CardDefinition.sorcery(
        name = "Howling Fury",
        manaCost = ManaCost.parse("{2}{B}"),
        oracleText = "Target creature gets +4/+0 until end of turn."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "97",
            rarity = Rarity.COMMON,
            artist = "Mike Dringenberg",
            flavorText = "I howl my soul to the moon, and the moon howls with me.",
            imageUri = "https://cards.scryfall.io/normal/front/a/4/a49a7c61-8696-4bab-9c96-05028db3a9f9.jpg",
            scryfallId = "a49a7c61-8696-4bab-9c96-05028db3a9f9",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Howling Fury") {
        spell(ModifyStatsEffect(4, 0, EffectTarget.TargetCreature, untilEndOfTurn = true))
    }
}
