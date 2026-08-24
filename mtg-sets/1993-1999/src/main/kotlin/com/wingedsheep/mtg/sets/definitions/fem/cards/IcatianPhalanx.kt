package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Icatian Phalanx
 * {4}{W}
 * Creature — Human Soldier
 * 2/4
 *
 * Banding only (CR 702.22) — the combat engine handles it, so the card just declares the keyword.
 */
val IcatianPhalanx = card("Icatian Phalanx") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "Banding (Any creatures with banding, and up to one without, can attack in a band. " +
        "Bands are blocked as a group. If any creatures with banding you control are blocking or " +
        "being blocked by a creature, you divide that creature's combat damage, not its controller, " +
        "among any of the creatures it's being blocked by or is blocking.)"
    power = 2
    toughness = 4

    keywords(Keyword.BANDING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "11"
        artist = "Kaja Foglio"
        flavorText = "Even after the wall was breached in half a dozen places, the Phalanxes fought on, standing solidly against the onrushing raiders. Disciplined and dedicated, they held their ranks to the end, even in the face of tremendous losses."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bc02d30-3eef-4a48-8b11-b4f37219ab3a.jpg?1783947917"
    }
}
