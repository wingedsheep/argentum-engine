package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.DrawCardsEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.targeting.TargetCreature

/**
 * Impolite Entrance
 *
 * {R} Sorcery
 * Target creature gains trample and haste until end of turn. Draw a card.
 */
object ImpoliteEntrance {
    val definition = CardDefinition.sorcery(
        name = "Impolite Entrance",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Target creature gains trample and haste until end of turn. Draw a card."
    )

    val script = cardScript("Impolite Entrance") {
        targets(TargetCreature())

        // Grant trample, then grant haste, then draw
        spell(
            GrantKeywordUntilEndOfTurnEffect(
                keyword = Keyword.TRAMPLE,
                target = EffectTarget.TargetCreature
            ) then GrantKeywordUntilEndOfTurnEffect(
                keyword = Keyword.HASTE,
                target = EffectTarget.TargetCreature
            ) then DrawCardsEffect(
                count = 1,
                target = EffectTarget.Controller
            )
        )
    }
}
