package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Riftburst Hellion — Murders at Karlov Manor #228
 * {5}{R}{G} · Creature — Hellion · 6/7
 *
 * Reach
 * Disguise {4}{R/G}{R/G}
 *
 * The whole card is the mana curve. Seven mana for a 6/7 reach body is unplayable on rate, so the
 * disguise line is the real cost: {3} for a 2/2 with ward {2} on turn three, then {4}{R/G}{R/G} to
 * flip it whenever the board wants a 6/7. Total mana spent is higher than hard-casting, split across
 * two turns — the archetypal limited "curve smoother" that disguise exists to enable.
 *
 * Nothing triggers on the flip, so there is no incentive to hold the reveal: turning it face up
 * mid-combat (CR 702.168c — a special action, no stack, no responses) is purely a combat trick that
 * turns a chump 2/2 block into a 6/7 wall.
 */
val RiftburstHellion = card("Riftburst Hellion") {
    manaCost = "{5}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Hellion"
    oracleText = "Reach\n" +
        "Disguise {4}{R/G}{R/G} (You may cast this card face down for {3} as a 2/2 creature with " +
        "ward {2}. Turn it face up any time for its disguise cost.)"
    power = 6
    toughness = 7
    keywords(Keyword.REACH)
    disguise = "{4}{R/G}{R/G}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "228"
        artist = "Brent Hollowell"
        flavorText = "Like any Ravnican denizen, it decided to visit Plaza West for lunch."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fae9044-a859-434d-8dc6-4f9d455ca5e1.jpg?1783912840"
    }
}
