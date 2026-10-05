package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Acrobatic Maneuver
 * {2}{W}
 * Instant
 * Exile target creature you control, then return that card to the battlefield under its owner's control.
 * Draw a card.
 */
val AcrobaticManeuver = card("Acrobatic Maneuver") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature you control, then return that card to the battlefield under its owner's control.\n" +
        "Draw a card."

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.Move(creature, Zone.EXILE) then
            Effects.Move(creature, Zone.BATTLEFIELD) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Winona Nelson"
        flavorText = "Renegades find ever more creative ways to work around the Consulate's aether regulations."
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74f92174-f38f-4623-a5f7-f5be7f7dcc9d.jpg?1783937239"
        ruling("2016-09-20", "After the creature returns to the battlefield, it will be a new object with no connection to the creature that was exiled. It won't be in combat or have any additional abilities it may have had before it was exiled. Any +1/+1 counters on it or Auras attached to it are removed, and any Equipment will no longer be attached.")
        ruling("2016-09-20", "If a creature token is exiled this way, it will cease to exist and won't return to the battlefield.")
    }
}
