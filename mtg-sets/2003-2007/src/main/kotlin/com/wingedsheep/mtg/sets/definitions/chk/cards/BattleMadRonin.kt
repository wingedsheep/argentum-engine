package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Battle-Mad Ronin
 * {1}{R}
 * Creature — Human Samurai
 * 1/1
 * Bushido 2 (Whenever this creature blocks or becomes blocked, it gets +2/+2 until end of turn.)
 * This creature attacks each combat if able.
 *
 * "Attacks each combat if able" is [MustAttack] (`lea/cards/Juggernaut.kt`).
 */
val BattleMadRonin = card("Battle-Mad Ronin") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Samurai"
    power = 1
    toughness = 1
    oracleText = "Bushido 2 (Whenever this creature blocks or becomes blocked, it gets +2/+2 until end of turn.)\n" +
        "This creature attacks each combat if able."

    keywordAbility(KeywordAbility.bushido(2))

    staticAbility {
        ability = MustAttack()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "Wayne England"
        flavorText = "\"I fought fiercely, bravely, and without mercy. Had I not struck down my captain in the heat of battle, I might have become a hero instead of an outcast.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6e4394a-fa91-4cf9-99c1-dc0bc1011c5b.jpg?1783944304"
    }
}
