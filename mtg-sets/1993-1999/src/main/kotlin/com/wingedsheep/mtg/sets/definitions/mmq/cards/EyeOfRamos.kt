package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Eye of Ramos
 * {3}
 * Artifact
 *
 * {T}: Add {U}.
 * Sacrifice this artifact: Add {U}.
 *
 * One of the five "of Ramos" rocks, identical but for the colour: a tap-for-one ability and a
 * sacrifice-for-one ability, both mana abilities (CR 605.1a), so neither uses the stack and both
 * can be activated while paying a cost. `manaAbility` also settles each ability's timing.
 */
val EyeOfRamos = card("Eye of Ramos") {
    manaCost = "{3}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "{T}: Add {U}.\n" +
        "Sacrifice this artifact: Add {U}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "294"
        artist = "David Martin"
        flavorText = "Ramos wept, and there were seas."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78d22400-39f6-444d-b508-783a7df7e945.jpg"
    }
}
