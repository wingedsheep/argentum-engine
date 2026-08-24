package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tooth of Ramos
 * {3}
 * Artifact
 *
 * {T}: Add {W}.
 * Sacrifice this artifact: Add {W}.
 *
 * One of the five "of Ramos" rocks, identical but for the colour: a tap-for-one ability and a
 * sacrifice-for-one ability, both mana abilities (CR 605.1a), so neither uses the stack and both
 * can be activated while paying a cost. `manaAbility` also settles each ability's timing.
 */
val ToothOfRamos = card("Tooth of Ramos") {
    manaCost = "{3}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "{T}: Add {W}.\n" +
        "Sacrifice this artifact: Add {W}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "313"
        artist = "David Martin"
        flavorText = "Ramos smiled, and there was day."
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a3b999d-8e63-4647-a921-15e169022096.jpg"
    }
}
