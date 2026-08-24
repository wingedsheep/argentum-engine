package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetSpell

/**
 * Thrull Wizard
 * {2}{B}
 * Creature — Thrull Wizard
 * 1/1
 * {1}{B}: Counter target black spell unless that spell's controller pays {B} or {3}.
 *
 * Erosion's shape, pointed at the stack: a [PayOrSufferEffect] whose payer is the *targeted
 * spell's* controller rather than the Wizard's, and whose cost is a genuine choice — neither
 * option subsumes the other, so a player with one Swamp and a player with three Mountains can
 * each save their spell.
 */
val ThrullWizard = card("Thrull Wizard") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Thrull Wizard"
    oracleText = "{1}{B}: Counter target black spell unless that spell's controller pays {B} or {3}."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        target = TargetSpell(filter = TargetFilter.SpellOnStack.withColor(Color.BLACK))
        effect = PayOrSufferEffect(
            cost = Costs.pay.Choice(listOf(Costs.pay.Mana("{B}"), Costs.pay.Mana("{3}"))),
            suffer = Effects.CounterSpell(),
            player = EffectTarget.TargetController,
            consequenceDescription = "counter that spell",
        )
        description =
            "{1}{B}: Counter target black spell unless that spell's controller pays {B} or {3}."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Anson Maddocks"
        flavorText = "\"In crafting intelligent Thrulls to assist in sacrifices, Sahr inadvertantly set the stage for the Thrull Rebellion.\"\n—*Sarpadian Empires, vol. II*"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4e732fb-cbef-4fd8-b704-e4d513a6cf2d.jpg?1783947898"
    }
}
