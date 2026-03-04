package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.UntapDuringOtherUntapSteps

/**
 * Seedborn Muse
 * {3}{G}{G}
 * Creature — Spirit
 * 2/4
 * Untap all permanents you control during each other player's untap step.
 */
val SeedbornMuse = card("Seedborn Muse") {
    manaCost = "{3}{G}{G}"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 4
    oracleText = "Untap all permanents you control during each other player's untap step."

    staticAbility {
        ability = UntapDuringOtherUntapSteps
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "138"
        artist = "Adam Rex"
        flavorText = "\"Her voice is wilderness, savage and pure.\" —Kamahl, druid acolyte"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35b13321-e429-4497-aef2-93a9df421d38.jpg?1562905861"
    }
}
