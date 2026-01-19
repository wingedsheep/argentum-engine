package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.AbilityCost
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

object AdeptWatershaper {
    val definition = CardDefinition.creature(
        name = "Adept Watershaper",
        manaCost = ManaCost.parse("{U}"),
        subtypes = setOf(Subtype.of("Merfolk"), Subtype.WIZARD),
        power = 1,
        toughness = 1,
        keywords = setOf(Keyword.PROWESS),
        oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
                "{2}{U}: Target creature gains flying until end of turn."
    )

    val script = cardScript("Adept Watershaper") {
        // Use the macro to add keyword AND trigger logic
        prowess()

        // Activated Ability
        activated(
            cost = AbilityCost.Mana(generic = 2, blue = 1),
            effect = GrantKeywordUntilEndOfTurnEffect(
                keyword = Keyword.FLYING,
                target = EffectTarget.TargetCreature
            )
        )
    }
}
