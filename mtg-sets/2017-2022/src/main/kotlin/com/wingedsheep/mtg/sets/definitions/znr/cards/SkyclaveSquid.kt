package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Skyclave Squid — Zendikar Rising #82
 * {1}{U} · Creature — Squid · 3/2
 *
 * Defender
 * Landfall — Whenever a land you control enters, this creature can attack this turn as though it
 * didn't have defender.
 */
val SkyclaveSquid = card("Skyclave Squid") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Squid"
    power = 3
    toughness = 2
    oracleText = "Defender\n" +
        "Landfall — Whenever a land you control enters, this creature can attack this turn as " +
        "though it didn't have defender."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.CanAttackDespiteDefenderThisTurn()
        description = "Landfall — Whenever a land you control enters, this creature can attack this " +
            "turn as though it didn't have defender."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Livia Prima"
        flavorText = "An ancient sea monster dragged the Murasa Skyclave under the waves, then smaller " +
            "sea monsters moved in."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/581ed049-7b5b-47d9-96af-f737631a221f.jpg?1783929385"
    }
}
