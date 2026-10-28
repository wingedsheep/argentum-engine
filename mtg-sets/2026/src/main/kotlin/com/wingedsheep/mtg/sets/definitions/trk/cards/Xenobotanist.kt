package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Xenobotanist
 * {1}{G}
 * Creature — Vulcan Scientist
 * 1/3
 *
 * {T}: Add one mana of any color.
 */
val Xenobotanist = card("Xenobotanist") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Vulcan Scientist"
    colorIdentity = "G"
    oracleText = "{T}: Add one mana of any color."
    power = 1
    toughness = 3
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "224"
        artist = "Marta Nael"
        flavorText = "\"Electromagnetic stimulation triggers bioluminescence and causes a 22% increase in aesthetic appeal. But to what end?\""
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20a97cda-42b2-44f1-8e77-f64dd1c98f6f.jpg?1785981613"
    }
}

