package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tajuru Blightblade — Zendikar Rising #208
 * {G} · Creature — Elf Rogue · 1/1
 *
 * Deathtouch
 */
val TajuruBlightblade = card("Tajuru Blightblade") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Rogue"
    oracleText = "Deathtouch"
    power = 1
    toughness = 1

    keywords(Keyword.DEATHTOUCH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "208"
        artist = "Dmitry Burmak"
        flavorText = "Ora Ondar, the Khalni Garden, is a lush tangle of growth in the harsh lands of Akoum. " +
            "Its plant growth is marvelously diverse: a brilliant spectrum of vibrant life . . . and speedy death."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f3d6020-6767-406c-bf28-6b3e9ae72f50.jpg"
    }
}
