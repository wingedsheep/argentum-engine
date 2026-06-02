package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Firebrand Ranger
 * {1}{R}
 * Creature — Human Soldier Ranger
 * 2/1
 * {G}, {T}: You may put a basic land card from your hand onto the battlefield.
 */
val FirebrandRanger = card("Firebrand Ranger") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier Ranger"
    power = 2
    toughness = 1
    oracleText = "{G}, {T}: You may put a basic land card from your hand onto the battlefield."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{G}"),
            Costs.Tap
        )
        effect = HandPatterns.putFromHand(
            filter = GameObjectFilter.BasicLand,
            entersTapped = false
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Quinton Hoover"
        flavorText = "A skilled ranger can glance at the mud on your boots and tell where you last camped."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee05211e-cf08-4dea-9740-ed06f8682153.jpg?1562942810"
    }
}
