package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.OpponentsCantMakeYouSacrifice

/**
 * Sigarda, Host of Herons
 * {2}{G}{W}{W}
 * Legendary Creature — Angel
 * 5/5
 *
 * Flying, hexproof
 * Spells and abilities your opponents control can't cause you to sacrifice permanents.
 *
 * The third line is [OpponentsCantMakeYouSacrifice], a player-scoped "can't" rather than a
 * per-permanent one — it protects *the player*, so it also covers permanents that entered after
 * Sigarda and stops an opponent's optional sacrifice from being offered at all. Sacrifice costs
 * an opponent's spell or ability imposes ("unless you sacrifice …", ward—sacrifice) become
 * unpayable rather than merely declined.
 */
val SigardaHostOfHerons = card("Sigarda, Host of Herons") {
    manaCost = "{2}{G}{W}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Angel"
    power = 5
    toughness = 5
    oracleText = "Flying, hexproof\n" +
        "Spells and abilities your opponents control can't cause you to sacrifice permanents."

    keywords(Keyword.FLYING, Keyword.HEXPROOF)

    staticAbility {
        ability = OpponentsCantMakeYouSacrifice
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "210"
        artist = "Chris Rahn"
        flavorText = "Great devotion yields great reward."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/feccd0e2-fae6-4ced-acdf-4252ed5c56e7.jpg?1783940655"

        ruling("2018-12-07", "As a spell or ability an opponent controls resolves, if it would force you to sacrifice a permanent, you just don't. That part of the effect does nothing. If that spell or ability gives you the option to sacrifice a permanent (as Desecration Demon does), you can't take that option.")
        ruling("2018-12-07", "If a spell or ability an opponent controls states that something happens unless you sacrifice a permanent (as Mogis, God of Slaughter does), you can't choose to sacrifice a permanent. On the other hand, if a spell or ability an opponent controls instructs you to sacrifice a permanent unless you perform an action (as Killing Wave does), you can choose whether or not to perform the action. If you don't perform the action, nothing happens, since you can't sacrifice any permanents.")
        ruling("2018-12-07", "Sigarda's ability affects only sacrifices. It won't stop a creature from dying due to lethal damage or having 0 toughness, and it won't stop a permanent from being put into its owner's graveyard due to the \"legend rule.\" None of these are sacrifices; they're the result of game rules.")
    }
}
