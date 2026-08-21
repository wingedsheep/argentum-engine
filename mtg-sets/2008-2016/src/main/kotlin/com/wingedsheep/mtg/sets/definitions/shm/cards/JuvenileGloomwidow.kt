package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Juvenile Gloomwidow
 * {G}{G}
 * Creature — Spider
 * 1 / 3
 *
 * Reach (This creature can block creatures with flying.)
 * Wither (This deals damage to creatures in the form of -1/-1 counters.)
 *
 * - Keyword-only creature. This is plain [Keyword.REACH], not the older Gloomwidow "can block only
 *   creatures with flying" restriction — the printed line grants blocking, it does not limit it.
 */
val JuvenileGloomwidow = card("Juvenile Gloomwidow") {
    manaCost = "{G}{G}"
    typeLine = "Creature — Spider"
    power = 1
    toughness = 3
    oracleText = "Reach (This creature can block creatures with flying.)\n" +
        "Wither (This deals damage to creatures in the form of -1/-1 counters.)"

    keywords(Keyword.REACH, Keyword.WITHER)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "121"
        artist = "Thomas M. Baxa"
        flavorText = "Gloomwidow venom is particularly virulent during the spider's first years, when it does most of its hunting near the forest floor."
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7f323d5-f26c-46e9-9d6a-be5c254fe8b6.jpg?1783942742"
    }
}
