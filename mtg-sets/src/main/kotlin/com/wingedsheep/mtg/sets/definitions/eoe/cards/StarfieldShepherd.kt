package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Starfield Shepherd
 * {3}{W}{W}
 * Creature — Angel
 * Flying
 * When this creature enters, search your library for a basic Plains card or a creature card with mana value 1 or less, reveal it, put it into your hand, then shuffle.
 * Warp {1}{W} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)
 * 3/2
 */
val StarfieldShepherd = card("Starfield Shepherd") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    oracleText = "Flying\nWhen this creature enters, search your library for a basic Plains card or a creature card with mana value 1 or less, reveal it, put it into your hand, then shuffle.\n" +
        "Warp {1}{W} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)"
    power = 3
    toughness = 2

    keywords(Keyword.FLYING)

    // When this creature enters, search your library for a basic Plains card or a creature card with mana value 1 or less, reveal it, put it into your hand, then shuffle.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val searchFilter = GameObjectFilter.BasicLand.withSubtype(Subtype.PLAINS) or GameObjectFilter.Creature.manaValueAtMost(1)
        effect = EffectPatterns.searchLibrary(
            filter = searchFilter,
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        )
        
        description = "When this creature enters, search your library for a basic Plains card or a creature card with mana value 1 or less, reveal it, put it into your hand, then shuffle."
    }

    // Warp ability
    warp = "{1}{W}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/1226e575-aa78-4c68-be1d-6e5c2dc6315b.jpg?1752946696"
    }
}
