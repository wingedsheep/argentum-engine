package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantEnterTheBattlefield
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Weathered Runestone
 * {2}
 * Artifact
 * Nonland permanent cards in graveyards and libraries can't enter the battlefield.
 * Players can't cast spells from graveyards or libraries.
 *
 * Two zone-scoped locks, as on Soulless Jailer: an entry prohibition matched against the card in
 * the graveyard or library it would leave (lands still enter, per the ruling), and the cast
 * prohibition's "where" axis. A card exiled first and then cast is cast from exile, so it is
 * unaffected.
 */
val WeatheredRunestone = card("Weathered Runestone") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Nonland permanent cards in graveyards and libraries can't enter the battlefield.\n" +
        "Players can't cast spells from graveyards or libraries."

    staticAbility {
        ability = CantEnterTheBattlefield(
            GameObjectFilter.NonlandPermanent,
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
        rarity = Rarity.UNCOMMON
        collectorNumber = "247"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fc2478f-e624-46fb-85af-1254564cd4d2.jpg?1783928181"
        ruling(
            "2021-02-05",
            "Players can still play lands from graveyards if an effect allows them to. Similarly, effects can put land cards onto the battlefield from any zone."
        )
        ruling(
            "2021-02-05",
            "If an effect exiles a card from a graveyard or library and allows a player to cast it, the player may do so. The spell is cast from exile."
        )
        ruling(
            "2021-02-05",
            "Look at the card as it exists in your graveyard or library to determine whether it can enter the battlefield. If it's a double-faced card, use its front-face characteristics only."
        )
    }
}
