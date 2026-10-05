package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Drag Under
 * {2}{U}
 * Sorcery
 * Return target creature to its owner's hand.
 * Draw a card.
 *
 * The draw rides on the same single-target spell, so if the creature becomes an illegal target the
 * whole spell doesn't resolve and no card is drawn (2016-07-13 ruling).
 */
val DragUnder = card("Drag Under") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return target creature to its owner's hand.\nDraw a card."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ReturnToHand(creature) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Tianhua X"
        flavorText = "Some took to their boats to escape the unfolding nightmare. The remains of their " +
            "vessels now litter the shores of Nephalia."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dffa444-92ff-41d8-8b55-8896808bbfab.jpg?1783937500"

        ruling(
            "2016-07-13",
            "If the targeted creature becomes an illegal target before Drag Under resolves, you won't draw a card."
        )
    }
}
