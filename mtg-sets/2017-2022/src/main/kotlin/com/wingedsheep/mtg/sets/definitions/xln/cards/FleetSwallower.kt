package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.model.Rarity

/**
 * Fleet Swallower
 * {5}{U}{U}
 * Creature — Fish
 * 6/6
 * Whenever this creature attacks, target player mills half their library, rounded up.
 *
 * Any player may be targeted, not only the one being attacked. The half is read off the targeted
 * player's library at resolution ([divRoundedUp], Maddening Cacophony's shape), so two triggers
 * each halve what is left in turn.
 */
val FleetSwallower = card("Fleet Swallower") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Fish"
    power = 6
    toughness = 6
    oracleText = "Whenever this creature attacks, target player mills half their library, rounded up."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(
            DynamicAmounts.zone(player.asPlayer, Zone.LIBRARY).count() divRoundedUp 2,
            player
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Tomasz Jedruszek"
        flavorText = "\"Captain, I think that island is following us.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7edbde38-1653-4eb5-b196-8881375781cb.jpg?1783935781"
        ruling("2017-09-29", "Fleet Swallower's triggered ability can target any player, not just the player it's attacking.")
        ruling("2017-09-29", "If two Fleet Swallowers attack, each trigger determines the top half of that player's library separately. For example, if the target player has fifteen cards in their library, that player puts the top eight cards into the graveyard, then puts the top four cards into the graveyard.")
    }
}
