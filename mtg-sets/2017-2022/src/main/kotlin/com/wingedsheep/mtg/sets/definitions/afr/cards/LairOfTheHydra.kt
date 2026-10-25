package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lair of the Hydra
 * Land
 * If you control two or more other lands, this land enters tapped.
 * {T}: Add {G}.
 * {X}{G}: Until end of turn, this land becomes an X/X green Hydra creature. It's still a land.
 * X can't be 0.
 *
 * The X/X base P/T is evaluated once on resolution off the activation's chosen X
 * (Fractalize's dynamic [Effects.BecomeCreature] shape); `minimumXValue = 1` is "X can't be 0".
 */
val LairOfTheHydra = card("Lair of the Hydra") {
    typeLine = "Land"
    colorIdentity = "G"
    oracleText = "If you control two or more other lands, this land enters tapped.\n{T}: Add {G}.\n" +
        "{X}{G}: Until end of turn, this land becomes an X/X green Hydra creature. It's still a land. X can't be 0."

    replacementEffect(
        EntersTapped(unlessCondition = Conditions.Not(Conditions.YouControlOtherAtLeast(2, GameObjectFilter.Land)))
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{X}{G}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = DynamicAmounts.xValue(),
            toughness = DynamicAmounts.xValue(),
            creatureTypes = setOf("Hydra"),
            colors = setOf("GREEN"),
            duration = Duration.EndOfTurn,
        )
        minimumXValue = 1
        description = "{X}{G}: Until end of turn, this land becomes an X/X green Hydra creature. It's still a land. " +
            "X can't be 0."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b670bb0f-680f-4036-bdb6-ac73e866a398.jpg?1783926433"
    }
}
