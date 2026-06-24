package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Chocobo Kick
 * {1}{G}
 * Sorcery
 *
 * Kicker—Return a land you control to its owner's hand. (You may return a land you control to its
 * owner's hand in addition to any other costs as you cast this spell.)
 * Target creature you control deals damage equal to its power to target creature an opponent
 * controls. If this spell was kicked, the creature you control deals twice that much damage instead.
 *
 * The kicker is a non-mana additional cost (returning a land). When it's paid, the damage dealt by
 * the controlled creature is doubled — modeled by branching the damage amount on [WasKicked].
 */
val ChocoboKick = card("Chocobo Kick") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Kicker—Return a land you control to its owner's hand. (You may return a land you control to its owner's hand in addition to any other costs as you cast this spell.)\n" +
        "Target creature you control deals damage equal to its power to target creature an opponent controls. If this spell was kicked, the creature you control deals twice that much damage instead."

    keywordAbility(KeywordAbility.kicker(Costs.additional.ReturnToHand(GameObjectFilter.Land)))

    spell {
        val yourCreature = target("creature you control", Targets.CreatureYouControl)
        val theirCreature = target("creature an opponent controls", Targets.CreatureOpponentControls)
        effect = ConditionalEffect(
            condition = WasKicked,
            effect = Effects.DealDamage(
                DynamicAmount.Multiply(DynamicAmounts.targetPower(0), 2),
                theirCreature,
                damageSource = yourCreature
            ),
            elseEffect = Effects.DealDamage(
                DynamicAmounts.targetPower(0),
                theirCreature,
                damageSource = yourCreature
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Ben Wootten"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff8c8be0-8223-499c-8704-cb68e0a42ce2.jpg?1748706427"
    }
}
