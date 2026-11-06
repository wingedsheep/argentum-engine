package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bastion Inventor
 * {5}{U}
 * Creature — Vedalken Artificer
 * 4/4
 * Improvise
 * Hexproof
 */
val BastionInventor = card("Bastion Inventor") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Artificer"
    power = 4
    toughness = 4
    oracleText = "Improvise (Your artifacts can help cast this spell. Each artifact you tap after " +
        "you're done activating mana abilities pays for {1}.)\n" +
        "Hexproof (This creature can't be the target of spells or abilities your opponents control.)"

    keywords(Keyword.IMPROVISE, Keyword.HEXPROOF)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Tony Foti"
        flavorText = "\"This armor transcends the limitations of mere flesh and bone.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/5/856f804d-0213-4b86-bc6a-6a0a1147c4f9.jpg?1783936775"
        ruling("2017-02-09", "Improvise doesn't change a spell's mana cost or mana value.")
        ruling("2017-02-09", "When calculating a spell's total cost, include any alternative costs, additional costs, or anything else that increases or reduces the cost to cast the spell. Improvise applies after the total cost is calculated.")
        ruling("2017-02-09", "Because improvise isn't an alternative cost, it can be used in conjunction with alternative costs.")
        ruling("2017-02-09", "Improvise can't pay for {W}, {U}, {B}, {R}, {G}, or {C} mana symbols in a spell's total cost.")
        ruling("2017-02-09", "Improvise can't be used to pay for anything other than the cost of casting the spell. For example, it can't be used during the resolution of an ability that says \"Counter target spell unless its controller pays {3}.\"")
        ruling("2017-02-09", "If an artifact you control has a mana ability with {T} in the cost, activating that ability while casting a spell with improvise will result in the artifact being tapped when you pay the spell's costs. You won't be able to tap it again for improvise. Similarly, if you sacrifice an artifact to activate a mana ability while casting a spell with improvise, that artifact won't be on the battlefield when you pay the spell's costs, so you won't be able to tap it for improvise.")
        ruling("2017-02-09", "Tapping an artifact won't cause its abilities to stop applying unless those abilities say so.")
        ruling("2017-02-09", "Equipment attached to a creature doesn't become tapped when that creature becomes tapped, and tapping that Equipment doesn't cause the creature to become tapped.")
    }
}
