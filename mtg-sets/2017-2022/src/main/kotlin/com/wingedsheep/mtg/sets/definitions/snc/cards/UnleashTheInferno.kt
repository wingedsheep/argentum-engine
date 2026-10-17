package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Unleash the Inferno
 * {1}{B}{R}{G}
 * Instant
 * Unleash the Inferno deals 7 damage to target creature or planeswalker. When it deals excess
 * damage this way, destroy target artifact or enchantment an opponent controls with mana value
 * less than or equal to that amount of excess damage.
 *
 * The damage stores its excess (CR 120.4a) as a pipeline number; with any excess, a reflexive
 * trigger (CR 603.12) goes on the stack whose target filter caps mana value at that stored number.
 * The reflexive trigger carries the number onto the stack, so the cap is the same when the target
 * is chosen and when it is re-checked on resolution.
 */
val UnleashTheInferno = card("Unleash the Inferno") {
    manaCost = "{1}{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Instant"
    oracleText = "Unleash the Inferno deals 7 damage to target creature or planeswalker. When it deals " +
        "excess damage this way, destroy target artifact or enchantment an opponent controls with mana " +
        "value less than or equal to that amount of excess damage."

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Pipeline {
            val excess = runStoringNumber { Effects.DealDamage(7, t, excessDamageVariable = it) }
            run(Effects.If(
                condition = Conditions.CompareAmounts(excess.amount, ComparisonOperator.GTE, 1),
                then = Effects.ReflexiveTrigger(
                    action = Effects.Nothing,
                    optional = false,
                    descriptionOverride = "destroy target artifact or enchantment an opponent controls " +
                        "with mana value less than or equal to that amount of excess damage",
                ) {
                    val permanent = target(
                        TargetFilter.ArtifactOrEnchantment.opponentControls()
                            .manaValueAtMostDynamic(excess.amount)
                    )
                    effect = Effects.Destroy(permanent)
                },
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f892607-8672-4013-86e6-0779d0b726de.jpg?1783923066"
    }
}
