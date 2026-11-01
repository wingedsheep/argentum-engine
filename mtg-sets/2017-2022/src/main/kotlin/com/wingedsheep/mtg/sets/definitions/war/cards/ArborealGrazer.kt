package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Arboreal Grazer — War of the Spark #149
 * {G} · Creature — Sloth Beast · 0/3
 *
 * Reach
 * When this creature enters, you may put a land card from your hand onto the battlefield tapped.
 */
val ArborealGrazer = card("Arboreal Grazer") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Sloth Beast"
    power = 0
    toughness = 3
    oracleText = "Reach\nWhen this creature enters, you may put a land card from your hand onto the battlefield tapped."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Land,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "149"
        artist = "Jason Rainville"
        flavorText = "\"We fight to stop a tyrant, but we also fight for the gentle creatures who have no concept of what befalls their world.\"\n" +
            "—Vivien Reid"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4a5f86f-44a8-4735-909a-770586d33a15.jpg?1783933418"
    }
}
