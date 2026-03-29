package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.core.Keyword

/**
 * Burrowguard Mentor
 * {G}{W}
 * Creature — Rabbit Soldier
 * *|*
 * Trample
 * Burrowguard Mentor's power and toughness are each equal to the number of creatures you control.
 */
val BurrowguardMentor = card("Burrowguard Mentor") {
    manaCost = "{G}{W}"
    typeLine = "Creature — Rabbit Soldier"
    oracleText = "Trample\nBurrowguard Mentor's power and toughness are each equal to the number of creatures you control."

    keywords(Keyword.TRAMPLE)
    dynamicStats(DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Dmitry Burmak"
        flavorText = "\"On your paws! I've been fighting in these fields for more seasons than you've had hot dinners!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87138ace-3594-499e-bad5-ec76148613ea.jpg?1721427007"
    }
}
