package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Restore the Peace
 * {1}{W}{U}
 * Instant
 * Return each creature that dealt damage this turn to its owner's hand.
 *
 * The group reads the per-permanent dealt-damage turn stamp (`hasDealtDamageThisTurn()`, the
 * active voice), so combat and noncombat damage both count. Only creatures on the battlefield are
 * gathered — one that dealt damage and then died stays in the graveyard, per the ruling.
 */
val RestoreThePeace = card("Restore the Peace") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "Return each creature that dealt damage this turn to its owner's hand."

    spell {
        effect = Patterns.Group.returnAllToHand(
            GroupFilter(GameObjectFilter.Creature.hasDealtDamageThisTurn())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Kev Walker"
        flavorText = "\"You can always count on the Azorius to ruin a party.\"\n—Massacre Girl"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/105902f6-99d0-4bee-9dfd-87a92ac04d91.jpg?1783940023"
        ruling("2013-04-15", "Restore the Peace has no effect on the damage that was dealt.")
        ruling(
            "2013-04-15",
            "Only creatures on the battlefield will be returned. If a creature dealt damage and then died, the creature card won't be returned from the graveyard to its owner's hand."
        )
    }
}
