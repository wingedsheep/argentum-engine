package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Hungering Yeti
 * {4}{R}
 * Creature — Yeti
 * 4/4
 * As long as you control a green or blue permanent, you may cast this spell as though it had flash.
 * (You may cast it any time you could cast an instant.)
 *
 * The flash clause is the card-level `conditionalFlash`, a timing permission checked as you begin
 * casting, gated on the projected colours of the permanents you control.
 */
val HungeringYeti = card("Hungering Yeti") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Yeti"
    power = 4
    toughness = 4
    oracleText = "As long as you control a green or blue permanent, you may cast this spell as though it had flash. " +
        "(You may cast it any time you could cast an instant.)"

    conditionalFlash = Conditions.YouControl(GameObjectFilter.Permanent.withAnyColor(Color.GREEN, Color.BLUE))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "105"
        artist = "Tyler Jacobson"
        flavorText = "No clan is as adept at saying \"go away\" as the Temur."
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02f7c596-5633-41e8-adcd-d4dc6eae5c88.jpg?1783938687"
        ruling(
            "2014-11-24",
            "Hungering Yeti checks if you control a green or blue permanent only as you begin to cast it. " +
                "Once you begin to cast Hungering Yeti as though it had flash, it doesn't matter what happens " +
                "to the green or blue permanents you control."
        )
    }
}
