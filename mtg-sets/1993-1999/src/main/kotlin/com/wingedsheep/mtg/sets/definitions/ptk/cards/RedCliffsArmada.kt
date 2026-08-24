package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless

/**
 * Red Cliffs Armada
 * {4}{U}
 * Creature — Human Soldier
 * 5/4
 * This creature can't attack unless defending player controls an Island.
 *
 * The Island Fish Jasconius attack clause: `DefendingPlayerControlsLandType` resolves against the
 * player actually being attacked, not any opponent, so it stays correct in multiplayer.
 */
val RedCliffsArmada = card("Red Cliffs Armada") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Soldier"
    power = 5
    toughness = 4
    oracleText = "This creature can't attack unless defending player controls an Island."

    staticAbility {
        ability = CantAttackUnless(Conditions.DefendingPlayerControlsLandType("Island"))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Zhang Jiazhen"
        flavorText = "By the battle of Red Cliffs in the year 208, the Wu kingdom controlled more than 7,000 warships on the Yangtze."
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8fe0f33-35c7-439b-bece-4a9461b92352.jpg"
    }
}
