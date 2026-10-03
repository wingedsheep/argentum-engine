package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.PreventDamagePerCounter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val RockHydra = card("Rock Hydra") {
    manaCost = "{X}{R}{R}"
    typeLine = "Creature — Hydra"
    power = 0
    toughness = 0
    oracleText = "This creature enters with X +1/+1 counters on it.\nFor each 1 damage that would be dealt to this creature, if it has a +1/+1 counter on it, remove a +1/+1 counter from it and prevent that 1 damage.\n{R}: Prevent the next 1 damage that would be dealt to this creature this turn.\n{R}{R}{R}: Put a +1/+1 counter on this creature. Activate only during your upkeep."

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))
    replacementEffect(PreventDamagePerCounter(CounterType.PLUS_ONE_PLUS_ONE))

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.PreventDamage(target = EffectTarget.Self, amount = DynamicAmounts.fixed(1))
    }
    activatedAbility {
        cost = Costs.Mana("{R}{R}{R}")
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.DuringStep(Step.UPKEEP), ActivationRestriction.OnlyDuringYourTurn)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Jeff A. Menges"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/410ac9e6-fbc1-4cc8-84db-84e2eb1bab97.jpg?1783948682"
        ruling("2008-08-01", "You can activate the activated ability that prevents damage any time, even if no source seems likely to deal damage to you in the near future. It will prevent the next 1 damage that would be dealt this turn, even if that turns out to be much later in the turn.")
        ruling("2008-08-01", "If damage that would be dealt to Rock Hydra can't be prevented, you still remove a +1/+1 counter from it for each 1 damage dealt.")
        ruling("2004-10-04", "If put onto the battlefield without casting it from your hand, X will be zero.")
        ruling("2004-10-04", "Once on the battlefield, the X is considered to be zero when calculating its mana cost.")
    }
}
