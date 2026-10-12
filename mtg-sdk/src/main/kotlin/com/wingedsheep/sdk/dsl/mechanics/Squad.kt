package com.wingedsheep.sdk.dsl.mechanics

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.CardBuilder
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.CastChoiceMade
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/** Squad's linked additional-cost and entry-trigger abilities. */
fun CardBuilder.squad(manaCost: String? = null, additionalCost: AdditionalCost? = null) {
    keywordAbility(KeywordAbility.OptionalAdditionalCost(
        manaCost = manaCost?.let(ManaCost::parse),
        additionalCost = additionalCost,
        multi = true,
        displayPrefix = "Squad",
        keyword = Keyword.SQUAD,
        declaredSlot = ChoiceSlot.SQUAD,
    ))
    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = CastChoiceMade(ChoiceSlot.SQUAD)
        effect = Effects.CreateTokenCopyOfTarget(
            target = EffectTarget.TriggeringEntity,
            count = DynamicAmount.CastChoice(ChoiceSlot.SQUAD),
        )
    }
}
