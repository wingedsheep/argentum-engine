package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.beamMeUp
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Transporter Malfunction
 * {1}{R}
 * Sorcery
 * Transporter Malfunction deals 3 damage to any target.
 * Beam me up {3}{R}{R}{R} (You may cast this card from your graveyard for {3}{R}{R}{R} if you also
 * return a creature you control to its owner's hand. Then exile this spell.)
 */
val TransporterMalfunction = card("Transporter Malfunction") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Transporter Malfunction deals 3 damage to any target.\n" +
        "Beam me up {3}{R}{R}{R} (You may cast this card from your graveyard for {3}{R}{R}{R} if you also " +
        "return a creature you control to its owner's hand. Then exile this spell.)"
    beamMeUp("{3}{R}{R}{R}")
    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(3, t)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Bartek Fedyczak"
        flavorText = "\"Looks like the Heisenberg compensator blew.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85b108c3-00b5-4dfb-90de-57fa483c4926.jpg?1793274273"
    }
}
