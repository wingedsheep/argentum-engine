package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DestroyEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OwnerGainsLifeEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object PathOfPeace {
    val definition = CardDefinition.sorcery(
        name = "Path of Peace",
        manaCost = ManaCost.parse("{3}{W}"),
        oracleText = "Destroy target creature. Its owner gains 4 life."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "21",
            rarity = Rarity.COMMON,
            artist = "Pete Venters",
            flavorText = "The soldier reaped the profits of peace.",
            imageUri = "https://cards.scryfall.io/normal/front/a/1/a1f3e1c9-bfad-49a1-b171-6fa344ef2eef.jpg",
            scryfallId = "a1f3e1c9-bfad-49a1-b171-6fa344ef2eef",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Path of Peace") {
        spell(
            DestroyEffect(EffectTarget.TargetCreature) then
            OwnerGainsLifeEffect(4)
        )
    }
}
