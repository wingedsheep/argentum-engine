package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Blood Celebrant
 * {B}
 * Creature — Human Cleric
 * 1/1
 * {B}, Pay 1 life: Add one mana of any color.
 */
val BloodCelebrant = card("Blood Celebrant") {
    manaCost = "{B}"
    typeLine = "Creature — Human Cleric"
    oracleText = "{B}, Pay 1 life: Add one mana of any color."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.PayLife(1))
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Ben Thompson"
        flavorText = "Their blood is the nectar that nourishes the Cabal."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/805de325-6f14-4a52-bb85-f9a9545d82a4.jpg?1562920849"
    }
}
