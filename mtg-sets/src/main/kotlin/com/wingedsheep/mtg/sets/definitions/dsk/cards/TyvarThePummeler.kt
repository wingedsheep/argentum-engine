package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Tyvar, the Pummeler — {1}{G}{G}
 * Legendary Creature — Elf Warrior
 * 3/3
 * Tap another untapped creature you control: Tyvar gains indestructible until end of turn. Tap it.
 * {3}{G}{G}: Creatures you control get +X/+X until end of turn, where X is the greatest power
 *   among creatures you control.
 */
val TyvarThePummeler = card("Tyvar, the Pummeler") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Warrior"
    oracleText = "Tap another untapped creature you control: Tyvar gains indestructible until end of turn. Tap it.\n{3}{G}{G}: Creatures you control get +X/+X until end of turn, where X is the greatest power among creatures you control."
    power = 3
    toughness = 3

    // Tap another untapped creature you control: Tyvar gains indestructible until end of turn. Tap it.
    // "Tap it" refers to Tyvar — he taps himself as part of the effect (see Drudge Sentinel pattern).
    activatedAbility {
        cost = Costs.TapAnotherPermanent(GameObjectFilter.Creature)
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self),
            Effects.Tap(EffectTarget.Self)
        )
        description = "Tyvar gains indestructible until end of turn. Tap it."
    }

    // {3}{G}{G}: Creatures you control get +X/+X until end of turn, where X is the greatest power.
    // Snapshot X before the pump so mid-loop power increases don't inflate X.
    activatedAbility {
        val x = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
        cost = Costs.Mana("{3}{G}{G}")
        effect = Effects.Composite(
            Effects.StoreNumber("tyvar_pump_x", x),
            GroupPatterns.modifyStatsForAll(
                power = DynamicAmount.VariableReference("tyvar_pump_x"),
                toughness = DynamicAmount.VariableReference("tyvar_pump_x"),
                filter = GroupFilter.AllCreaturesYouControl
            )
        )
        description = "Creatures you control get +X/+X until end of turn, where X is the greatest power among creatures you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "202"
        artist = "Olivier Bernard"
        flavorText = "\"They just keep coming and coming—perfect for my morning workout!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f7687b1-630a-4e95-89c7-eaf456d8cb68.jpg?1726286622"
    }
}
