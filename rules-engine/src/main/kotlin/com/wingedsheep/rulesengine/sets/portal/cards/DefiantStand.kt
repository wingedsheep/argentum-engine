package com.wingedsheep.rulesengine.sets.portal.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.TapUntapEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

object DefiantStand {
    val definition = CardDefinition.instant(
        name = "Defiant Stand",
        manaCost = ManaCost.parse("{1}{W}"),
        oracleText = "Cast this spell only during the declare attackers step and only if you've been attacked this step.\nTarget creature gets +1/+3 until end of turn. Untap that creature."
    ).copy(
        metadata = ScryfallMetadata(
            collectorNumber = "12",
            rarity = Rarity.UNCOMMON,
            artist = "Hannibal King",
            imageUri = "https://cards.scryfall.io/normal/front/9/c/9cc37fd2-5c34-4522-8113-e6dd2181550b.jpg",
            scryfallId = "9cc37fd2-5c34-4522-8113-e6dd2181550b",
            releaseDate = "1997-05-01"
        )
    )

    // Note: The timing restriction (declare attackers step + being attacked) requires
    // additional spell casting validation logic in the game rules engine.
    val script = cardScript("Defiant Stand") {
        spell(
            ModifyStatsEffect(1, 3, EffectTarget.TargetCreature, untilEndOfTurn = true) then
            TapUntapEffect(EffectTarget.TargetCreature, tap = false)
        )
    }
}
