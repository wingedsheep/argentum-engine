package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Noble Elephant
 * {3}{W}
 * Creature — Elephant
 * 2/2
 *
 * Trample
 * Banding (Any creatures with banding, and up to one without, can attack in a band.
 * Bands are blocked as a group. If any creatures with banding you control are blocking
 * or being blocked by a creature, you divide that creature's combat damage, not its
 * controller, among any of the creatures it's being blocked by or is blocking.)
 *
 * Both keywords are fully handled by the combat engine (Banding via CR 702.22 in
 * CombatDamageManager / AttackPhaseManager), so the card needs no per-card wiring.
 */
val NobleElephant = card("Noble Elephant") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elephant"
    power = 2
    toughness = 2
    oracleText = "Trample\n" +
        "Banding (Any creatures with banding, and up to one without, can attack in a band. " +
        "Bands are blocked as a group. If any creatures with banding you control are blocking " +
        "or being blocked by a creature, you divide that creature's combat damage, not its " +
        "controller, among any of the creatures it's being blocked by or is blocking.)"

    keywords(Keyword.TRAMPLE, Keyword.BANDING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Tony Roberts"
        flavorText = "\"Proud am I, / strong am I, / courageous in defending my children " +
            "/ and fierce in punishing what stands in my way.\"\n—\"So the Elephant Speaks,\" Zhalfirin song"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65f399cb-dddb-422a-8d36-938b82b59e10.jpg?1562719755"
    }
}
