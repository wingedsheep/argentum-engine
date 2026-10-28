package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.model.Rarity

/**
 * Peer into the Abyss
 * {4}{B}{B}{B}
 * Sorcery
 * Target player draws cards equal to half the number of cards in their library and loses half
 * their life. Round up each time.
 *
 * Both halves are read at resolution off the targeted player, in printed order: the draw count
 * first (rounded up), then — after the draws — half their current life total (rounded up).
 */
val PeerIntoTheAbyss = card("Peer into the Abyss") {
    manaCost = "{4}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player draws cards equal to half the number of cards in their library and " +
        "loses half their life. Round up each time."

    spell {
        val player = target(Targets.Player)
        effect = Effects.DrawCards(
            DynamicAmounts.zone(player.asPlayer, Zone.LIBRARY).count() divRoundedUp 2,
            player
        ) then Effects.LoseHalfLife(roundUp = true, target = player, lifePlayer = player.asPlayer)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "Izzy"
        flavorText = "\"Oh, don't be so dramatic. When your eyes bleed, and your brain leaks out your ears, then we'll talk about lost sanity.\"\n—Braids, dementia summoner"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aac00055-640e-4749-8d23-d242e6d0b23a.jpg?1783930701"
        ruling(
            "2020-06-23",
            "The life lost is rounded up, not the remaining life total. For example, if the target player has 7 " +
                "life, they lose 4 life and end up with 3. Similarly, the number of cards drawn is rounded up, " +
                "not the number of cards remaining in the library."
        )
        ruling(
            "2020-06-23",
            "To draw the cards, first the player determines how many cards to draw, then draws them. This event " +
                "may be modified so that a different number of cards are actually drawn."
        )
    }
}
