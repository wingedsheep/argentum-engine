package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.beamMeUp
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Federation Resupply
 * {1}{W}
 * Sorcery
 * Search your library for an Aura, Equipment, or Spacecraft card, reveal it, put it into your
 * hand, then shuffle.
 * Beam me up {3}{W} (You may cast this card from your graveyard for {3}{W} if you also return a
 * creature you control to its owner's hand. Then exile this spell.)
 */
val FederationResupply = card("Federation Resupply") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Search your library for an Aura, Equipment, or Spacecraft card, reveal it, put it into " +
        "your hand, then shuffle.\nBeam me up {3}{W} (You may cast this card from your graveyard for {3}{W} " +
        "if you also return a creature you control to its owner's hand. Then exile this spell.)"
    beamMeUp("{3}{W}")
    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.withSubtype(Subtype.AURA) or
                GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT) or
                GameObjectFilter.Any.withSubtype("Spacecraft"),
            destination = SearchDestination.HAND,
            reveal = true
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Ryan Valle"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/5626c58f-9245-437a-9844-b023320cf709.jpg?1793273787"
    }
}
