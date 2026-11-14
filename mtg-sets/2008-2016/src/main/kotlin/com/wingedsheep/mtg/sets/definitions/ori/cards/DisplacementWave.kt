package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Displacement Wave
 * {X}{U}{U}
 * Sorcery
 * Return all nonland permanents with mana value X or less to their owners' hands.
 *
 * A non-targeted mass bounce ([Patterns.Group.returnAllToHand]) over every nonland permanent, any
 * controller, whose mana value is at most the X paid for this spell — `manaValueAtMostX()` reads X
 * at resolution, the same bound Forced March and Pernicious Deed use. Tokens (mana value 0 unless
 * copying something) and permanents with {X} in their cost (X counts as 0) are returned even when
 * X is 0. Every matching permanent is returned simultaneously, each to its owner's hand.
 */
val DisplacementWave = card("Displacement Wave") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return all nonland permanents with mana value X or less to their owners' hands."

    spell {
        effect = Patterns.Group.returnAllToHand(
            GroupFilter(GameObjectFilter.NonlandPermanent.manaValueAtMostX())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Seb McKinnon"
        flavorText = "The sea respects no boundaries, least of all the coast."
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad6fef1f-6cc9-4e24-a1be-fc313774f28d.jpg?1783938352"
        ruling("2015-06-22", "If a permanent has an {X} in its mana cost, that X is 0.")
        ruling("2015-06-22", "A token has mana value 0 unless it's a copy of something else, in which case it has the mana cost of whatever it's copying.")
    }
}
