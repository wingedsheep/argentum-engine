package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Frontline Rebel
 * {2}{R}
 * Creature — Human Warrior
 * 3/3
 * This creature attacks each combat if able.
 */
val FrontlineRebel = card("Frontline Rebel") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 3
    oracleText = "This creature attacks each combat if able."

    staticAbility {
        ability = MustAttack()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Winona Nelson"
        flavorText = "\"Will you be strong and stand with me?\""
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b742738a-2663-474d-b75a-28f1f67dc335.jpg?1783936754"
        ruling("2017-02-09", "If, during your declare attackers step, Frontline Rebel is tapped or is affected by a spell or ability that says it can't attack, then it doesn't attack. If there's a cost associated with having it attack, you aren't forced to pay that cost, so it doesn't have to attack in that case either.")
    }
}
