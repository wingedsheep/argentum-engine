package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Secluded Starforge
 * Land
 * {T}: Add {C}.
 * {2}, {T}, Tap X untapped artifacts you control: Target creature gets +X/+0 until end of turn. Activate only as a sorcery.
 * {5}, {T}: Create a 2/2 colorless Robot artifact creature token.
 */
val SecludedStarforge = card("Secluded Starforge") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{2}, {T}, Tap X untapped artifacts you control: Target creature gets +X/+0 until end of turn. Activate only as a sorcery.\n{5}, {T}: Create a 2/2 colorless Robot artifact creature token."

    // Basic mana ability: {T}: Add {C}
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    // Pump ability: {2}, {T}, Tap X untapped artifacts you control: Target creature gets +X/+0 until end of turn
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.TapXPermanents(GameObjectFilter.Artifact)
        )
        val target = target("target creature", Targets.Creature)
        effect = ModifyStatsEffect(DynamicAmount.XValue, DynamicAmount.Fixed(0), target, Duration.EndOfTurn)
        timing = TimingRule.SorcerySpeed
    }

    // Token creation ability: {5}, {T}: Create a 2/2 Robot token
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{5}"),
            Costs.Tap
        )
        effect = CreateTokenEffect(
            power = 2,
            toughness = 2,
            colors = setOf(), // colorless
            creatureTypes = setOf("Robot"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/c/4/c46f9a07-005c-44b7-8057-b2f00b274dd6.jpg?1756281130"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "257"
        artist = "Chris Rahn"
        flavorText = "The heat of a sun condensed into a forge."
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a997ff9f-045a-44a2-983d-f36414cef1ab.jpg?1752947602"
    }
}
