package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureHasSubtype
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayerOrPlaneswalker
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Sorcerer's Wand
 * {1}
 * Artifact — Equipment
 * Equipped creature has "{T}: This creature deals 1 damage to target player or planeswalker.
 * If this creature is a Wizard, it deals 2 damage instead."
 * Equip {3}
 */
val SorcerersWand = card("Sorcerer's Wand") {
    manaCost = "{1}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has \"{T}: This creature deals 1 damage to target player or planeswalker. " +
        "If this creature is a Wizard, it deals 2 damage instead.\"\n" +
        "Equip {3}"

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Tap,
                effect = DealDamageEffect(
                    amount = DynamicAmount.Conditional(
                        condition = EnchantedCreatureHasSubtype(Subtype("Wizard")),
                        ifTrue = DynamicAmount.Fixed(2),
                        ifFalse = DynamicAmount.Fixed(1)
                    ),
                    target = EffectTarget.ContextTarget(0),
                    damageSource = EffectTarget.Self
                ),
                targetRequirements = listOf(TargetPlayerOrPlaneswalker())
            )
        )
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69ee3692-54e3-42de-85c6-0a3b5c6a2402.jpg?1562737162"
        ruling("2018-04-27", "Whether the equipped creature is a Wizard is checked only as the ability resolves.")
        ruling("2018-04-27", "The equipped creature, not Sorcerer's Wand, is the source of the damage-dealing ability and of the damage dealt.")
    }
}
