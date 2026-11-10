package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Farbog Revenant (Shadows over Innistrad #110)
 * {2}{B}
 * Creature — Spirit
 * 1/3
 * Skulk
 * Lifelink
 */
val FarbogRevenant = card("Farbog Revenant") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    oracleText = "Skulk (This creature can't be blocked by creatures with greater power.)\n" +
        "Lifelink (Damage dealt by this creature also causes you to gain that much life.)"
    power = 1
    toughness = 3

    keywords(Keyword.SKULK, Keyword.LIFELINK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Min Yum"
        flavorText = "Spiteful geists rise from the muck of the Farbogs, eager to lure travelers to their doom."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f6de332-debb-49f3-8b29-1719060ab00c.jpg?1783937775"
        ruling(
            "2016-04-08",
            "Skulk matters only as blockers are chosen. Modifying either creature's power after " +
                "blockers are chosen won't cause the attacking creature to become unblocked."
        )
        ruling(
            "2016-04-08",
            "If you cause a creature to have 0 power or less, use the actual value (which may be " +
                "negative) to determine whether it can block or be blocked. A creature with skulk and 0 " +
                "or less power most likely won't be blocked, but it won't deal combat damage and won't " +
                "trigger any abilities that trigger when combat damage is dealt."
        )
    }
}
