package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Skull of Ramos
 * {3}
 * Artifact
 *
 * {T}: Add {B}.
 * Sacrifice this artifact: Add {B}.
 *
 * One of the five "of Ramos" rocks, identical but for the colour: a tap-for-one ability and a
 * sacrifice-for-one ability, both mana abilities (CR 605.1a), so neither uses the stack and both
 * can be activated while paying a cost. `manaAbility` also settles each ability's timing.
 */
val SkullOfRamos = card("Skull of Ramos") {
    manaCost = "{3}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "{T}: Add {B}.\n" +
        "Sacrifice this artifact: Add {B}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "312"
        artist = "David Martin"
        flavorText = "Ramos fell, and there was night."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f071957c-9bea-4d00-9ffd-30f98d57b8d2.jpg"
    }
}
