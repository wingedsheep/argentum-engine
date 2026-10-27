package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Sword of the Animist
 * {2}
 * Legendary Artifact — Equipment
 *
 * Equipped creature gets +1/+1.
 * Whenever equipped creature attacks, you may search your library for a basic land card, put it
 * onto the battlefield tapped, then shuffle.
 * Equip {2}
 *
 * The attack trigger is Heart-Piercer Bow's `Triggers.attached.attacks()` carrying Ondu Giant's
 * optional basic-land search to the battlefield tapped.
 */
val SwordOfTheAnimist = card("Sword of the Animist") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\n" +
        "Whenever equipped creature attacks, you may search your library for a basic land card, " +
        "put it onto the battlefield tapped, then shuffle.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "240"
        artist = "Daniel Ljunggren"
        flavorText = "The blade glows only for Zendikar's chosen."
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3687c36-c0ae-4dba-9379-b420236bf529.jpg?1783938307"
    }
}
