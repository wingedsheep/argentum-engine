package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gaea's Avenger
 * {1}{G}{G}
 * Creature — Treefolk
 * Power/toughness: 1+star/1+star
 * Gaea's Avenger's power and toughness are each equal to 1 plus the number of
 * artifacts your opponents control.
 *
 * Characteristic-defining ability: `dynamicStats` sets base power and toughness to
 * (artifacts opponents control), with a +1 offset on each, so the printed star values
 * are replaced in Layer 7b.
 */
val GaeasAvenger = card("Gaea's Avenger") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk"
    oracleText = "Gaea's Avenger's power and toughness are each equal to 1 plus the number of artifacts your opponents control."

    dynamicStats(
        DynamicAmounts.battlefield(Player.EachOpponent, GameObjectFilter.Artifact).count(),
        powerOffset = 1,
        toughnessOffset = 1
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Pete Venters"
        flavorText = "After the destruction of Argoth, Gaea was willing to instill a portion of her own powers into some of her more vengeful followers."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39d763bd-b0a9-46ba-bcd2-9304063446f2.jpg?1562907103"
    }
}
