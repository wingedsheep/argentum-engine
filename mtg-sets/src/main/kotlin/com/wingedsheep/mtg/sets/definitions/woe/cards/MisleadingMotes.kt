package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Misleading Motes
 * {3}{U}
 * Instant
 *
 * Target creature's owner puts it on their choice of the top or bottom of their library.
 */
val MisleadingMotes = card("Misleading Motes") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target creature's owner puts it on their choice of the top or bottom of their library."

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.PutOnTopOrBottomOfLibrary(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Raoul Vitale"
        flavorText = "Yarvis was almost certain he'd seen the same tree an hour ago, but then again, " +
            "didn't all trees look alike? He shelved his doubts and followed the friendly, twinkling " +
            "motes deeper into the forest."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c6c7a43-c3d1-450e-834a-54e1b9def1cd.jpg?1783915117"

        ruling(
            "2023-09-01",
            "The creature's owner chooses whether to put it on the top or bottom of their library. " +
                "If multiple cards are put into the library this way (such as when the spell targets " +
                "a melded permanent), that creature's owner puts all the cards on top or all the " +
                "cards on the bottom. They put them in whatever order they wish, and do not need to " +
                "reveal the order."
        )
    }
}
