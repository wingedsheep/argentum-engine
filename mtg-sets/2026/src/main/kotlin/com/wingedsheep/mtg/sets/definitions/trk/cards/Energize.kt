package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.beamMeUp
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Energize
 * {1}{U}
 * Sorcery
 * Return target creature an opponent controls to its owner's hand.
 * Beam me up {3}{U} (You may cast this card from your graveyard for {3}{U} if you also return a
 * creature you control to its owner's hand. Then exile this spell.)
 */
val Energize = card("Energize") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return target creature an opponent controls to its owner's hand.\n" +
        "Beam me up {3}{U} (You may cast this card from your graveyard for {3}{U} if you also " +
        "return a creature you control to its owner's hand. Then exile this spell.)"
    beamMeUp("{3}{U}")
    spell {
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ReturnToHand(creature)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Carlos C Diaz"
        flavorText = "\"Beam us up! BEAM US UP!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24e72db3-a8d9-4553-92fe-ba6e40c4f62e.jpg?1792668391"
    }
}
