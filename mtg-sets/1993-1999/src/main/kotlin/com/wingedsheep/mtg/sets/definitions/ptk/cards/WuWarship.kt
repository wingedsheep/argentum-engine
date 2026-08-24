package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless

/**
 * Wu Warship
 * {2}{U}
 * Creature — Human Soldier
 * 3/3
 * This creature can't attack unless defending player controls an Island.
 *
 * The same [CantAttackUnless] shape as Goblin Rock Sled, aimed at the *defending* player's
 * Islands — a nonbasic land with the Island type counts, which is why the condition is a land
 * subtype test rather than a basic-land one.
 */
val WuWarship = card("Wu Warship") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "This creature can't attack unless defending player controls an Island."

    staticAbility {
        ability = CantAttackUnless(Conditions.DefendingPlayerControlsLandType(Subtype.ISLAND.value))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "Jiang Zhuqing"
        flavorText = "Both Wu and Wei warships patrolled the Yangtze River, the natural border between the two kingdoms."
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e211d47-dab4-429a-a90b-5b8489441886.jpg"
    }
}
