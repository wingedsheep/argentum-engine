package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.beamMeUp
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * There's Coffee in that Nebula
 * {1}{W}
 * Sorcery
 * Look at the top five cards of your library. You may reveal a Plains card or nonland permanent
 * card with mana value 3 or less from among them and put it into your hand. Put the rest on the
 * bottom in a random order.
 * Beam me up {2}{W} (You may cast this card from your graveyard for {2}{W} if you also return a
 * creature you control to its owner's hand. Then exile this spell.)
 */
val TheresCoffeeInThatNebula = card("There's Coffee in that Nebula") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Look at the top five cards of your library. You may reveal a Plains card or nonland " +
        "permanent card with mana value 3 or less from among them and put it into your hand. Put the rest " +
        "on the bottom in a random order.\nBeam me up {2}{W} (You may cast this card from your graveyard " +
        "for {2}{W} if you also return a creature you control to its owner's hand. Then exile this spell.)"
    beamMeUp("{2}{W}")
    spell {
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 5,
            filter = GameObjectFilter.Any.withSubtype(Subtype.PLAINS) or
                GameObjectFilter.NonlandPermanent.manaValueAtMost(3),
            prompt = "You may reveal a Plains card or nonland permanent card with mana value 3 or less " +
                "and put it into your hand"
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Pauline Voss"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8dd7e0c6-84f6-4570-af93-4c64e78c2868.jpg?1792668331"
    }
}
