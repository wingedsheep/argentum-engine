package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Barricade Breaker
 * {7}
 * Artifact Creature — Juggernaut
 * 7/5
 * Improvise
 * This creature attacks each combat if able.
 */
val BarricadeBreaker = card("Barricade Breaker") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Juggernaut"
    power = 7
    toughness = 5
    oracleText = "Improvise (Your artifacts can help cast this spell. Each artifact you tap after " +
        "you're done activating mana abilities pays for {1}.)\n" +
        "This creature attacks each combat if able."

    keywords(Keyword.IMPROVISE)

    staticAbility {
        ability = MustAttack()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "144"
        artist = "Karl Kopinski"
        flavorText = "\"The Consulate sought to crush us. Let's give them a taste of their own medicine.\"\n—Venkat Dasai, renegade commander"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cc81a94-955d-4734-b056-9b9a86cae60b.jpg?1783936730"
        ruling("2017-02-09", "If, during your declare attackers step, Barricade Breaker is tapped or is affected by a spell or ability that says it can't attack, then it doesn't attack. If there's a cost associated with having it attack, you aren't forced to pay that cost, so it doesn't have to attack in that case either.")
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
