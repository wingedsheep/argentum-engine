package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object MonstrousGrowth {
    val definition = CardDefinition.sorcery(
        name = "Monstrous Growth",
        manaCost = ManaCost.parse("{1}{G}"),
        oracleText = "Target creature gets +4/+4 until end of turn."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "173",
            rarity = Rarity.COMMON,
            artist = "Dan Frazier",
            flavorText = "Some cats are born fighters; others need a little persuading.",
            imageUri = "https://cards.scryfall.io/normal/front/1/f/1fd2edb9-0b53-432e-bb3b-171d2a85439d.jpg",
            scryfallId = "1fd2edb9-0b53-432e-bb3b-171d2a85439d",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Monstrous Growth") {
        spell(ModifyStatsEffect(4, 4, EffectTarget.TargetCreature, untilEndOfTurn = true))
    }
}
