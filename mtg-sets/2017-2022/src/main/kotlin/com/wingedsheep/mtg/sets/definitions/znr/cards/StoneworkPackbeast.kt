package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantSubtype

/**
 * Stonework Packbeast — Zendikar Rising #255
 * {2} · Artifact Creature — Beast · 2/1
 *
 * Stonework Packbeast is also a Cleric, Rogue, Warrior, and Wizard.
 * {2}: Add one mana of any color.
 *
 * The four self-[GrantSubtype]s are the Tajuru Paragon characteristic-defining ability (CR 604.3),
 * folded into the type line in every zone; the mana ability is the Prismite shape.
 */
val StoneworkPackbeast = card("Stonework Packbeast") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Beast"
    oracleText = "Stonework Packbeast is also a Cleric, Rogue, Warrior, and Wizard.\n" +
        "{2}: Add one mana of any color."
    power = 2
    toughness = 1

    for (subtype in listOf("Cleric", "Rogue", "Warrior", "Wizard")) {
        staticAbility { ability = GrantSubtype(subtype) }
    }

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.AddAnyColorMana()
        manaAbility = true
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "255"
        artist = "Dmitry Burmak"
        flavorText = "It will happily carry home your treasure or drag you back to civilization, as circumstances demand."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a29e17ba-d584-4296-9f43-17467edaa25f.jpg?1783929309"
        ruling(
            "2020-09-25",
            "The ability that adds creature types to Stonework Packbeast applies in all zones, not just the battlefield."
        )
    }
}
