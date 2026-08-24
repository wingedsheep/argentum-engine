package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Heart of Ramos
 * {3}
 * Artifact
 *
 * {T}: Add {R}.
 * Sacrifice this artifact: Add {R}.
 *
 * One of the five "of Ramos" rocks, identical but for the colour: a tap-for-one ability and a
 * sacrifice-for-one ability, both mana abilities (CR 605.1a), so neither uses the stack and both
 * can be activated while paying a cost. `manaAbility` also settles each ability's timing.
 */
val HeartOfRamos = card("Heart of Ramos") {
    manaCost = "{3}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "{T}: Add {R}.\n" +
        "Sacrifice this artifact: Add {R}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "296"
        artist = "David Martin"
        flavorText = "Ramos bled, and there was fire."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0046226-7563-4345-aa4b-a2c732c2780a.jpg"
    }
}
