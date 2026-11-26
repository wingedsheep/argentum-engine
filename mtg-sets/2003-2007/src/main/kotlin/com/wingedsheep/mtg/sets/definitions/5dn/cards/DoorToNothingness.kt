package com.wingedsheep.mtg.sets.definitions.`5dn`.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Door to Nothingness
 * {5}
 * Artifact
 *
 * This artifact enters tapped.
 * {W}{W}{U}{U}{B}{B}{R}{R}{G}{G}, {T}, Sacrifice this artifact: Target player loses the game.
 */
val DoorToNothingness = card("Door to Nothingness") {
    manaCost = "{5}"
    colorIdentity = "WUBRG"
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n" +
        "{W}{W}{U}{U}{B}{B}{R}{R}{G}{G}, {T}, Sacrifice this artifact: Target player loses the game."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{W}{W}{U}{U}{B}{B}{R}{R}{G}{G}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        val player = target(Targets.Player)
        effect = Effects.LoseGame(player)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Puddnhead"
        flavorText = "\"All memory of your existence will be wiped from reality. You will die, and no one will mourn.\"\n—Memnarch"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c92ffeae-6b51-4426-a080-b1b065b1290d.jpg?1783944383"
    }
}
