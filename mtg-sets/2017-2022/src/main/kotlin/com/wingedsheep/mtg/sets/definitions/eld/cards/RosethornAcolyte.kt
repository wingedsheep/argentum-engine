package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Rosethorn Acolyte // Seasonal Ritual
 * {2}{G}
 * Creature — Elf Druid
 * 2/3
 * {T}: Add one mana of any color.
 *
 * Adventure: Seasonal Ritual — {G}, Sorcery — Adventure
 * Add one mana of any color.
 */
val RosethornAcolyte = card("Rosethorn Acolyte") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    oracleText = "{T}: Add one mana of any color."
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    adventure("Seasonal Ritual") {
        manaCost = "{G}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Add one mana of any color. (Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.AddAnyColorMana(1)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "Johannes Voss"
        flavorText = "\"Old battlefields make great gardens. So rich from the blood of the fallen.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a0d430f-da84-4752-940c-8457c525aac9.jpg?1783932603"
    }
}
