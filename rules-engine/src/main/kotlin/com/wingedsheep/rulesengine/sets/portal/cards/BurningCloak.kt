package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.DealDamageEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object BurningCloak {
    val definition = CardDefinition.sorcery(
        name = "Burning Cloak",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Target creature gets +2/+0 until end of turn. Burning Cloak deals 2 damage to that creature."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "120",
            rarity = Rarity.COMMON,
            artist = "Scott M. Fischer",
            imageUri = "https://cards.scryfall.io/normal/front/e/2/e2b8f443-dba5-45a5-bb2e-f57b4fdd1d01.jpg",
            scryfallId = "e2b8f443-dba5-45a5-bb2e-f57b4fdd1d01",
            releaseDate = "1997-05-01"
        )
    )

    val script = cardScript("Burning Cloak") {
        spell(
            ModifyStatsEffect(2, 0, EffectTarget.TargetCreature, untilEndOfTurn = true) then
            DealDamageEffect(2, EffectTarget.TargetCreature)
        )
    }
}
