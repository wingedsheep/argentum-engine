package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wurmcoil Larva — Modern Horizons 3 #112
 * {3}{B}{B} · Artifact Creature — Phyrexian Wurm · 3/3
 *
 * Deathtouch, lifelink
 * When this creature dies, create a 1/2 black Phyrexian Wurm artifact creature token with
 * deathtouch and a 2/1 black Phyrexian Wurm artifact creature token with lifelink.
 */
val WurmcoilLarva = card("Wurmcoil Larva") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Phyrexian Wurm"
    power = 3
    toughness = 3
    oracleText = "Deathtouch, lifelink\n" +
        "When this creature dies, create a 1/2 black Phyrexian Wurm artifact creature token with " +
        "deathtouch and a 2/1 black Phyrexian Wurm artifact creature token with lifelink."

    keywords(Keyword.DEATHTOUCH, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Phyrexian", "Wurm"),
            keywords = setOf(Keyword.DEATHTOUCH),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/1/a/1a7800c9-6808-4f33-87ad-50ad8c79f2d7.jpg?1783911116",
        ) then Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Phyrexian", "Wurm"),
            keywords = setOf(Keyword.LIFELINK),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/1/4/1482ab42-875c-46fc-aa0b-b18154488985.jpg?1783911115",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "David Astruga"
        flavorText = "Even a scrap of Phyrexian machinery can be lethal."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc9b30e0-3934-4e4c-bdd9-5b7696b45948.jpg?1783911275"
    }
}
