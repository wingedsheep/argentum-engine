package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Starlit Sanctum
 * Land
 * {T}: Add {C}.
 * {W}, {T}, Sacrifice a Cleric creature: You gain life equal to the sacrificed creature's toughness.
 * {B}, {T}, Sacrifice a Cleric creature: Target player loses life equal to the sacrificed creature's power.
 */
val StarlitSanctum = card("Starlit Sanctum") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{W}, {T}, Sacrifice a Cleric creature: You gain life equal to the sacrificed creature's toughness.\n{B}, {T}, Sacrifice a Cleric creature: Target player loses life equal to the sacrificed creature's power."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{W}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Cleric"))
        )
        effect = GainLifeEffect(
            amount = DynamicAmount.SacrificedPermanentToughness,
            target = EffectTarget.Controller
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Cleric"))
        )
        target = TargetPlayer()
        effect = LoseLifeEffect(
            amount = DynamicAmount.SacrificedPermanentPower,
            target = EffectTarget.ContextTarget(0)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "325"
        artist = "Ben Thompson"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ace5e601-2583-4d9c-8bdf-aa33666c717c.jpg?1562935857"
    }
}
