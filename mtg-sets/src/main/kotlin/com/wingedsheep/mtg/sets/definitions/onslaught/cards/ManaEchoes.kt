package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.triggers.OnOtherCreatureEnters

/**
 * Mana Echoes
 * {2}{R}{R}
 * Enchantment
 * Whenever a creature enters, you may add an amount of {C} equal to the number
 * of creatures you control that share a creature type with it.
 */
val ManaEchoes = card("Mana Echoes") {
    manaCost = "{2}{R}{R}"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature enters, you may add an amount of {C} equal to the number of creatures you control that share a creature type with it."

    triggeredAbility {
        trigger = OnOtherCreatureEnters(youControlOnly = false)
        effect = MayEffect(
            Effects.AddColorlessMana(DynamicAmount.CreaturesSharingTypeWithTriggeringEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "218"
        artist = "Scott M. Fischer"
        flavorText = "When the ground is saturated with mana, even the lightest footstep can bring it to the surface."
        imageUri = "https://cards.scryfall.io/large/front/1/b/1b15d04c-62cb-4704-8cc7-9842cef27a1b.jpg?1562899467"
    }
}
