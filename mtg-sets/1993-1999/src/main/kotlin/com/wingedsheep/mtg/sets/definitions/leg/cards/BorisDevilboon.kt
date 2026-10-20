package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Boris Devilboon
 * {3}{B}{R}
 * Legendary Creature — Zombie Wizard
 * 2/2
 * {2}{B}{R}, {T}: Create a 1/1 black and red Demon creature token named Minor Demon.
 *
 * Scryfall has no Minor Demon token printing, so the token carries no image.
 */
val BorisDevilboon = card("Boris Devilboon") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Zombie Wizard"
    oracleText = "{2}{B}{R}, {T}: Create a 1/1 black and red Demon creature token named Minor Demon."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}{R}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK, Color.RED),
            creatureTypes = setOf("Demon"),
            name = "Minor Demon"
        )
        description = "{2}{B}{R}, {T}: Create a 1/1 black and red Demon creature token named Minor Demon."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "223"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82ae30e8-2dcd-46b8-925b-cc24e11fb95d.jpg?1783948040"
    }
}
