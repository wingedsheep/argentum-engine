package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity

/**
 * Purphoros's Intervention
 * {X}{R}
 * Sorcery
 * Choose one —
 * • Create an X/1 red Elemental creature token with trample and haste. Sacrifice it at the beginning
 *   of the next end step.
 * • Purphoros's Intervention deals twice X damage to target creature or planeswalker.
 *
 * Heliod's Intervention's sibling: both modes read the one X paid at cast time. The token is
 * Hungry for More's `sacrificeAtStep = Step.END` shape with Sylvan Offering's `dynamicPower = X`
 * (evaluated once as the token is created, so it's a fixed X/1). The second mode is a single hit of
 * `X * 2` damage (ruling: not X damage dealt twice).
 */
val PurphorossIntervention = card("Purphoros's Intervention") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Create an X/1 red Elemental creature token with trample and haste. Sacrifice it at the beginning " +
        "of the next end step.\n" +
        "• Purphoros's Intervention deals twice X damage to target creature or planeswalker."

    spell {
        modal(chooseCount = 1) {
            mode("Create an X/1 red Elemental token with trample and haste; sacrifice it at the next end step") {
                effect = Effects.CreateToken(
                    power = 0,
                    toughness = 1,
                    colors = setOf(Color.RED),
                    creatureTypes = setOf("Elemental"),
                    keywords = setOf(Keyword.TRAMPLE, Keyword.HASTE),
                    sacrificeAtStep = Step.END,
                    dynamicPower = DynamicAmounts.xValue(),
                    imageUri = "https://cards.scryfall.io/normal/front/6/0/60466c78-155e-442b-8022-795e1e9de8df.jpg?1783931453",
                )
            }
            mode("Deal twice X damage to target creature or planeswalker") {
                val victim = target(Targets.CreatureOrPlaneswalker)
                effect = Effects.DealDamage(DynamicAmounts.xValue() * 2, victim)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "151"
        artist = "Aleksi Briclot"
        flavorText = "\"I bring a fire to recast all the world.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecc911ee-0e12-4b10-add7-9a9d63c29443.jpg?1783931547"
        ruling(
            "2020-01-24",
            "The second mode of Purphoros's Intervention has it deal damage to the target equal to twice X. " +
                "It doesn't deal X damage then deal X damage again."
        )
    }
}
