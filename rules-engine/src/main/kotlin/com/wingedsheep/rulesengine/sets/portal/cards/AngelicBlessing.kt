package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.CompositeEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost

object AngelicBlessing {
    val definition = CardDefinition.sorcery(
        name = "Angelic Blessing",
        manaCost = ManaCost.parse("{2}{W}"),
        oracleText = "Target creature gets +3/+3 and gains flying until end of turn."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "2",
            rarity = Rarity.COMMON,
            artist = "DiTerlizzi",
            flavorText = "A peasant can do more by faith than a king by proclamation.",
            imageUri = "https://cards.scryfall.io/normal/front/3/1/31dda640-2a00-437e-855f-173c487e7395.jpg",
            scryfallId = "31dda640-2a00-437e-855f-173c487e7395",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Angelic Blessing") {
        spell(
            CompositeEffect(listOf(
                ModifyStatsEffect(3, 3, EffectTarget.TargetCreature, untilEndOfTurn = true),
                GrantKeywordUntilEndOfTurnEffect(Keyword.FLYING, EffectTarget.TargetCreature)
            ))
        )
    }
}
