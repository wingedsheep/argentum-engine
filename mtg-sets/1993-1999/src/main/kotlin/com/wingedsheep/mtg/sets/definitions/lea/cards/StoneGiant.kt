package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

// Modern Oracle uses a delayed destruction trigger, independent of later characteristic/control changes.
val StoneGiant = card("Stone Giant") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    power = 3
    toughness = 4
    oracleText = "{T}: Target creature you control with toughness less than this creature's power gains flying until end of turn. Destroy that creature at the beginning of the next end step."

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.Creature.youControl().compareNumericProperty(
            CardNumericProperty.TOUGHNESS,
            ComparisonOperator.LT,
            DynamicAmounts.sourcePower(),
        ))
        effect = Effects.GrantKeyword(Keyword.FLYING, creature) then Effects.CreateDelayedTrigger(
            step = Step.END,
            watchedTarget = creature,
            expiry = DelayedTriggerExpiry.Never,
            fireOnce = true,
            effect = Effects.Destroy(EffectTarget.TriggeringEntity),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "176"
        artist = "Dameon Willich"
        flavorText = "What goes up, must come down."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7ffaedb9-25f8-4304-9085-e12505b93312.jpg?1783948681"
        ruling("2009-10-01", "If Stone Giant’s ability is activated during a turn’s end step, the delayed triggered ability won’t trigger until the beginning of the next turn’s end step. The targeted creature will be destroyed at that time.")
        ruling("2009-10-01", "When the delayed triggered ability resolves, the targeted creature is destroyed, even if it’s no longer a creature, no longer under your control, or no longer has toughness less than Stone Giant’s power at that time.")
        ruling("2008-08-01", "If the Giant’s power and/or toughness change so that its toughness is less than its power, you can have the ability target the Giant itself.")
    }
}
