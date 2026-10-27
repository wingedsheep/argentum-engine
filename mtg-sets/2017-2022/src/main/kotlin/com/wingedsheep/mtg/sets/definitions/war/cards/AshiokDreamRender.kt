package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LibrarySearchProhibition
import com.wingedsheep.sdk.scripting.PlayersCantSearchLibraries
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ashiok, Dream Render — War of the Spark #228 (canonical printing)
 * {1}{U/B}{U/B} · Legendary Planeswalker — Ashiok · Starting Loyalty: 5
 *
 * Spells and abilities your opponents control can't cause their controller to search their library.
 * −1: Target player mills four cards. Then exile each opponent's graveyard.
 *
 * The static forbids only an opponent's own spells and abilities making them search their own
 * library: a mandatory "search …, then shuffle" still shuffles, a "you may search" can't be chosen
 * (so it doesn't shuffle), and searches it doesn't cover — yours, or theirs of another library —
 * are untouched. The −1 exiles the milled cards too when it targets an opponent.
 */
val AshiokDreamRender = card("Ashiok, Dream Render") {
    manaCost = "{1}{U/B}{U/B}"
    colorIdentity = "UB"
    typeLine = "Legendary Planeswalker — Ashiok"
    startingLoyalty = 5
    oracleText = "Spells and abilities your opponents control can't cause their controller to search their library.\n" +
        "−1: Target player mills four cards. Then exile each opponent's graveyard."

    staticAbility {
        ability = PlayersCantSearchLibraries(
            affected = Player.EachOpponent,
            scope = LibrarySearchProhibition.OWN_LIBRARY_BY_OWN_EFFECTS,
        )
    }

    loyaltyAbility(-1) {
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(4, player) then Effects.ExileOpponentsGraveyards()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "228"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2df3258-c053-48a8-974f-d80899b2cd93.jpg?1783933381"
        ruling(
            "2019-05-03",
            "Spells and abilities your opponents control may cause other players to search their libraries, " +
                "and spells and abilities you control may cause your opponents to search their libraries."
        )
        ruling(
            "2019-05-03",
            "If an effect says \"You may search your library . . . If you do, shuffle your library\" or " +
                "\"You may search your library . . . then shuffle your library,\" your opponents can't choose " +
                "to search, so they won't shuffle."
        )
        ruling(
            "2019-05-03",
            "If an effect says \"Search your library . . . then shuffle your library,\" your opponents " +
                "shuffle their libraries even though they can't search."
        )
        ruling(
            "2019-05-03",
            "Effects that instruct your opponents to reveal or look at cards from the top of their " +
                "libraries will still work. Only effects that use the word \"search\" are affected."
        )
        ruling(
            "2019-05-03",
            "If Ashiok's last ability targets an opponent, the cards put into their graveyard will be among " +
                "the cards exiled by the ability. No player may take any actions between the time the cards are " +
                "put into a graveyard and the time your opponents' graveyards are all exiled."
        )
    }
}
