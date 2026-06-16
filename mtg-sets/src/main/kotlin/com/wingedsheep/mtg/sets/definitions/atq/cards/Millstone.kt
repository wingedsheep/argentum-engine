package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Millstone
 * {2}
 * Artifact
 * {2}, {T}: Target player mills two cards.
 */
val Millstone = card("Millstone") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Target player mills two cards."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val player = target("target player", Targets.Player)
        effect = Patterns.Library.mill(2, player)
        description = "{2}, {T}: Target player mills two cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Kaja Foglio"
        flavorText = "More than one mage was driven insane by the sound of the Millstone relentlessly grinding away."
        imageUri = "https://cards.scryfall.io/normal/front/1/0/107646bc-2181-49f4-8821-1eaa46291855.jpg?1562898439"
    }
}
