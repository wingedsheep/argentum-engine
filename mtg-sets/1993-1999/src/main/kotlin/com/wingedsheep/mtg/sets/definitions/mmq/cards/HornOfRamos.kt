package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Horn of Ramos
 * {3}
 * Artifact
 *
 * {T}: Add {G}.
 * Sacrifice this artifact: Add {G}.
 *
 * One of the five "of Ramos" rocks, identical but for the colour: a tap-for-one ability and a
 * sacrifice-for-one ability, both mana abilities (CR 605.1a), so neither uses the stack and both
 * can be activated while paying a cost. `manaAbility` also settles each ability's timing.
 */
val HornOfRamos = card("Horn of Ramos") {
    manaCost = "{3}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "{T}: Add {G}.\n" +
        "Sacrifice this artifact: Add {G}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "299"
        artist = "David Martin"
        flavorText = "Ramos touched, and there was life."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b17f541-8e9d-43b0-b688-e3f2e7fa55c8.jpg"
    }
}
