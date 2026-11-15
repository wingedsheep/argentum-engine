package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Overgrown Zealot
 * {1}{G}
 * Creature — Elf Druid
 * {T}: Add one mana of any color.
 * {T}: Add two mana of any one color. Spend this mana only to turn permanents face up.
 * 0/4
 */
val OvergrownZealot = card("Overgrown Zealot") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 0
    toughness = 4
    oracleText = "{T}: Add one mana of any color.\n{T}: Add two mana of any one color. Spend this " +
        "mana only to turn permanents face up."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add one mana of any color."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(2, restriction = ManaRestriction.TurnPermanentsFaceUpOnly())
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add two mana of any one color. Spend this mana only to turn permanents face up."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "Tyler Walpole"
        flavorText = "Even as the roots twisted painfully into his flesh, he sang out in joy, for he " +
            "could feel himself becoming one with the woods."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96d68d29-499a-4864-be18-bd98fda0d173.jpg?1726286585"
    }
}
