package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Serra Avenger
 * {W}{W}
 * Creature — Angel
 * 3/3
 * You can't cast Serra Avenger during your first, second, or third turns of the game.
 * Flying, vigilance
 *
 * The cast restriction is `castOnlyIf(NOT controller-turns-taken ≤ 3)`, the same shape as
 * Jace Reawakened: castable only once its controller has taken at least four turns.
 */
val SerraAvenger = card("Serra Avenger") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 3
    oracleText = "You can't cast Serra Avenger during your first, second, or third turns of the game.\nFlying, vigilance"

    // "You can't cast Serra Avenger during your first, second, or third turns of the game."
    spell {
        castOnlyIf(Conditions.Not(Conditions.ControllerTurnsTakenAtMost(3)))
    }

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Scott M. Fischer"
        flavorText = "\"Those who endure in the face of suffering, those whose faith shines long in evil days, they shall see salvation.\"\n—*Song of All*, canto 904"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e9d7c1c-3bfd-4705-9bc2-5ca3f84cc32a.jpg?1783943249"
    }
}
