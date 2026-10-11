package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Plumb the Forbidden — Strixhaven: School of Mages #81
 * {1}{B} · Instant
 *
 * As an additional cost to cast this spell, you may sacrifice one or more creatures. When you do,
 * copy this spell for each creature sacrificed this way.
 * You draw a card and lose 1 life.
 *
 * The creatures are sacrificed as the spell is cast. The "when you do" trigger is a cast trigger
 * that reads the count off the spell itself (`permanentsSacrificedToCast(TriggeringEntity)`) and
 * only triggers when at least one creature was sacrificed; each copy is put on the stack, not cast
 * (CR 707.10), so it neither re-triggers this ability nor sacrifices anything.
 */
val PlumbTheForbidden = card("Plumb the Forbidden") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may sacrifice one or more creatures. " +
        "When you do, copy this spell for each creature sacrificed this way.\n" +
        "You draw a card and lose 1 life."

    additionalCost(Costs.additional.SacrificePermanents(GameObjectFilter.Creature))

    triggeredAbility {
        val sacrificed = DynamicAmounts.permanentsSacrificedToCast(EffectTarget.TriggeringEntity)
        trigger = Triggers.self.isCast()
        interveningIf = Conditions.CompareAmounts(sacrificed, ComparisonOperator.GTE, 1)
        effect = Effects.Repeat(sacrificed, Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity))
        description = "When you do, copy this spell for each creature sacrificed this way."
    }

    spell {
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Andrey Kuzinskiy"
        flavorText = "Strixhaven forbade all magic from the Blood Age, so that is where Extus began his research."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5034227f-3b8a-45bf-917c-c2cbd98f2192.jpg?1783927362"
    }
}
