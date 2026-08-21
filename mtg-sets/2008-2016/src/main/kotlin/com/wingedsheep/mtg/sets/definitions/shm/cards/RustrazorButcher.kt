package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Rustrazor Butcher
 * {1}{R}
 * Creature — Goblin Warrior
 * 1 / 2
 *
 * First strike
 * Wither (This deals damage to creatures in the form of -1/-1 counters.)
 *
 * - Keyword-only creature. The two keywords combine in the engine's own damage steps: the
 *   first-strike damage is what gets converted into -1/-1 counters, so neither needs scripting.
 */
val RustrazorButcher = card("Rustrazor Butcher") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 2
    oracleText = "First strike\n" +
        "Wither (This deals damage to creatures in the form of -1/-1 counters.)"

    keywords(Keyword.FIRST_STRIKE, Keyword.WITHER)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Ron Spencer"
        flavorText = "A Bloodwort's blade is salted with blood and peppered with rust, seasoning and slaughtering in a single swipe."
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daa0dd3e-51d1-4d75-b0f4-835992f420d6.jpg?1783942745"
    }
}
