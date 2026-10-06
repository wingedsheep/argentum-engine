package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AnimateLandGroup
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kormus Bell — Limited Edition Alpha #256
 * {4} · Artifact
 *
 * All Swamps are 1/1 black creatures that are still lands.
 *
 * Living Lands' group animation over Swamps, plus Ambush Commander's colour: every Swamp on the
 * battlefield (either player's) gains the creature type, the colour black, and base P/T 1/1,
 * keeping its land types.
 */
val KormusBell = card("Kormus Bell") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "All Swamps are 1/1 black creatures that are still lands."

    staticAbility {
        ability = AnimateLandGroup(
            filter = GroupFilter(GameObjectFilter.Land.withSubtype("Swamp")),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "256"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f4ef7a1-148d-44ac-89ed-0ef379cca0c6.jpg?1783948664"
        ruling(
            "2008-08-01",
            "A noncreature permanent that turns into a creature can attack, and its {T} abilities can be " +
                "activated, only if its controller has continuously controlled that permanent since the " +
                "beginning of their most recent turn. It doesn't matter how long the permanent has been a creature."
        )
        ruling(
            "2004-10-04",
            "The lands are both lands and creatures at the same time. They are affected by anything that " +
                "affects either permanent type."
        )
        ruling("2004-10-04", "It affects Swamps controlled by any and all players.")
    }
}
