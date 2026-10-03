package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * White Orchid Phantom
 * {W}{W}
 * Creature — Spirit Knight
 * 2/2
 * Flying, first strike
 * When this creature enters, destroy up to one target nonbasic land. Its controller may search
 * their library for a basic land card, put it onto the battlefield tapped, then shuffle.
 *
 * Same shape as [SunderingEruption]: "its controller" is [Player.ControllerOf] the targeted land,
 * read via last-known information once the land is gone (and live if it survived — the ruling
 * says the search still happens). With zero targets chosen, `ControllerOf` resolves to no player,
 * so [Effects.ForEachPlayer] runs nothing and nobody searches.
 */
val WhiteOrchidPhantom = card("White Orchid Phantom") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit Knight"
    power = 2
    toughness = 2
    oracleText = "Flying, first strike\n" +
        "When this creature enters, destroy up to one target nonbasic land. Its controller may search " +
        "their library for a basic land card, put it onto the battlefield tapped, then shuffle."

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val land = target(TargetFilter.NonbasicLand, optional = true)
        effect = Effects.Destroy(land) then
            Effects.ForEachPlayer(
                Player.ControllerOf("the destroyed land"),
                listOf(
                    Effects.May(
                        Patterns.Library.searchLibrary(
                            filter = GameObjectFilter.BasicLand,
                            count = 1,
                            destination = SearchDestination.BATTLEFIELD,
                            entersTapped = true,
                        )
                    )
                )
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Zoltan Boros"
        flavorText = "His oath required no service beyond death, but his honor demanded it."
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f8d885c-5b57-457f-a658-fd0b79cf98cc.jpg?1783911295"
        ruling("2024-06-07", "If the target land isn't destroyed by White Orchid Phantom's last ability (perhaps because the land has indestructible), its controller still gets to search for a basic land card.")
        ruling("2024-06-07", "If the target land is an illegal target when White Orchid Phantom's last ability tries to resolve, it won't resolve and none of its effects will happen. That land's controller won't get to search for a basic land card.")
    }
}
