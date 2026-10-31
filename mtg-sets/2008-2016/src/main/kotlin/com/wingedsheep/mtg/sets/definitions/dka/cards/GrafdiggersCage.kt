package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantEnterTheBattlefield
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Grafdigger's Cage
 * {1}
 * Artifact
 * Creature cards in graveyards and libraries can't enter the battlefield.
 * Players can't cast spells from graveyards or libraries.
 *
 * The creature-scoped sibling of Weathered Runestone: an entry lock matched against the card in the
 * graveyard or library it would leave, and the cast lock's "where" axis. A manifested card is
 * judged as the face-down 2/2 creature it would enter as, so manifesting from a library is
 * impossible while the Cage is out (ruling 2019-07-12).
 */
val GrafdiggersCage = card("Grafdigger's Cage") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Creature cards in graveyards and libraries can't enter the battlefield.\n" +
        "Players can't cast spells from graveyards or libraries."

    staticAbility {
        ability = CantEnterTheBattlefield(
            GameObjectFilter.Creature,
            fromZones = setOf(Zone.GRAVEYARD, Zone.LIBRARY)
        )
    }
    staticAbility {
        ability = PlayersCantCastSpells(
            Player.Each,
            fromZones = setOf(Zone.GRAVEYARD, Zone.LIBRARY)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "149"
        artist = "Daniel Ljunggren"
        flavorText = "\"If you wind up in one of mine, you can be sure as silver it will be your last.\"\n—Grafdigger Wulmer"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a6240e7-d3aa-40e9-a627-58e7bf62525c.jpg?1783940792"
        ruling(
            "2019-07-12",
            "Players can still play lands from graveyards or libraries if an effect allows them to do so."
        )
        ruling(
            "2019-07-12",
            "If an effect exiles a card from a library and allows a player to cast it, that player may do so. The spell is cast from exile, not a library."
        )
        ruling(
            "2019-07-12",
            "Look at the card as it exists in your graveyard to determine whether it can enter the battlefield. For example, Sculpting Steel can be put onto the battlefield as a copy of a creature, but Phyrexian Metamorph can't be put onto the battlefield, even if it would copy a noncreature artifact."
        )
        ruling(
            "2019-07-12",
            "Manifesting a card from a graveyard or library is an impossible action while Grafdigger's Cage is on the battlefield."
        )
    }
}
