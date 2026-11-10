package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Goblin Freerunner
 * {3}{R}
 * Creature — Goblin Warrior Ally
 * 3/2
 * Surge {1}{R} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)
 * Menace (This creature can't be blocked except by two or more creatures.)
 */
val GoblinFreerunner = card("Goblin Freerunner") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior Ally"
    power = 3
    toughness = 2
    oracleText = "Surge {1}{R} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)\n" +
        "Menace (This creature can't be blocked except by two or more creatures.)"

    keywordAbility(KeywordAbility.surge("{1}{R}"))
    keywords(Keyword.MENACE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Wayne Reynolds"
        flavorText = "There are old goblins and ambitious goblins, but very few old, ambitious goblins."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5082e0a1-5c3e-4b8c-ba88-290151564133.jpg?1783937906"
    }
}
