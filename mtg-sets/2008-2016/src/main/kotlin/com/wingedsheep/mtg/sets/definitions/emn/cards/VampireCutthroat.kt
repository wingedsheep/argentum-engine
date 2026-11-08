package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Vampire Cutthroat
 * {B}
 * Creature — Vampire Rogue
 * 1/1
 *
 * Skulk (This creature can't be blocked by creatures with greater power.)
 * Lifelink (Damage dealt by this creature also causes you to gain that much life.)
 */
val VampireCutthroat = card("Vampire Cutthroat") {
    manaCost = "{B}"
    typeLine = "Creature — Vampire Rogue"
    power = 1
    toughness = 1
    oracleText = "Skulk (This creature can't be blocked by creatures with greater power.)\n" +
        "Lifelink (Damage dealt by this creature also causes you to gain that much life.)"

    keywords(Keyword.SKULK, Keyword.LIFELINK)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Matt Stewart"
        flavorText = "Not all monsters have tentacles."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/954d53f3-ebbe-48e0-9e1a-7019d2b0740c.jpg?1783937474"
    }
}
