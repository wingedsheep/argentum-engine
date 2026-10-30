package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyDevotion
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Altar of the Pantheon (Theros Beyond Death #231)
 * {3}
 * Artifact
 * Your devotion to each color and each combination of colors is increased by one.
 * {T}: Add one mana of any color. If you control a God, a Demigod, or a legendary enchantment,
 * you gain 1 life.
 */
val AltarOfThePantheon = card("Altar of the Pantheon") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Your devotion to each color and each combination of colors is increased by one.\n" +
        "{T}: Add one mana of any color. If you control a God, a Demigod, or a legendary enchantment, you gain 1 life."

    staticAbility {
        ability = ModifyDevotion(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana() then Effects.If(
            condition = Conditions.Any(
                Conditions.YouControl(GameObjectFilter.Any.withAnySubtype("God", "Demigod")),
                Conditions.YouControl(GameObjectFilter.Enchantment.legendary()),
            ),
            then = Effects.GainLife(1),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "231"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1f2a587-443d-4626-a6f6-9a26838a42b1.jpg?1783931517"
    }
}
