package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardsInZonesCantBeTargeted
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantPlayLands
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tomik, Distinguished Advokist
 * {W}{W}
 * Legendary Creature — Human Advisor
 * 2/3
 *
 * Flying
 * Lands on the battlefield and land cards in graveyards can't be the targets of spells or abilities
 * your opponents control.
 * Your opponents can't play land cards from graveyards.
 *
 * The targeting lock is [CardsInZonesCantBeTargeted] over the battlefield and graveyards, scoped to
 * opponents' spells and abilities — every land, whoever controls it, not just Tomik's controller's
 * (so it isn't hexproof, which is relative to each permanent's own controller). The land-play lock
 * is [PlayersCantPlayLands] whose land filter is matched where the card is, so only a land played
 * out of a graveyard (Crucible of Worlds, Ramunap Excavator) is stopped.
 */
val TomikDistinguishedAdvokist = card("Tomik, Distinguished Advokist") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Advisor"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Lands on the battlefield and land cards in graveyards can't be the targets of spells or " +
        "abilities your opponents control.\n" +
        "Your opponents can't play land cards from graveyards."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CardsInZonesCantBeTargeted(
            zones = setOf(Zone.BATTLEFIELD, Zone.GRAVEYARD),
            filter = GameObjectFilter.Land,
            sourceController = Player.EachOpponent
        )
    }

    staticAbility {
        ability = PlayersCantPlayLands(
            affected = Player.EachOpponent,
            landFilter = GameObjectFilter.Any.currentlyIn(Zone.GRAVEYARD)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Johannes Voss"
        flavorText = "\"Some bend the law. I tie it into knots.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f346387c-2f13-472b-b5b1-968b490eb38f.jpg?1783933473"
        ruling(
            "2019-05-03",
            "Tomik's middle ability is similar to hexproof, but effects that interact with hexproof " +
                "don't count these lands or land cards as having hexproof."
        )
        ruling(
            "2019-05-03",
            "Players can't normally play a land card from a graveyard. If a player finds a way to do so, " +
                "Tomik's last ability takes precedence over that permission."
        )
    }
}
