package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Maverick Thopterist
 * {3}{U}{R}
 * Creature — Human Artificer
 * 2/2
 * Improvise
 * When this creature enters, create two 1/1 colorless Thopter artifact creature tokens with flying.
 */
val MaverickThopterist = card("Maverick Thopterist") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 2
    oracleText = "Improvise (Your artifacts can help cast this spell. Each artifact you tap after " +
        "you're done activating mana abilities pays for {1}.)\n" +
        "When this creature enters, create two 1/1 colorless Thopter artifact creature tokens with flying."

    keywords(Keyword.IMPROVISE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            count = 2,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/f/d/fd03a2b9-bfd7-4e1d-b660-e9f945699b78.jpg?1783937238",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/cad17d67-538e-4c60-a582-a394032a5112.jpg"
        ruling("2018-07-13", "Improvise doesn't change a spell's mana cost or mana value.")
        ruling("2018-07-13", "When calculating a spell's total cost, include any alternative costs, additional costs, or anything else that increases or reduces the cost to cast the spell. Improvise applies after the total cost is calculated.")
        ruling("2018-07-13", "Improvise can't pay for {W}, {U}, {B}, {R}, {G}, or {C} mana symbols in a spell's total cost.")
        ruling("2018-07-13", "Improvise can't be used to pay for anything other than the cost of casting the spell. For example, it can't be used during the resolution of an ability that says \"Counter target spell unless its controller pays {3}.\"")
        ruling("2018-07-13", "If an artifact you control has a mana ability with {T} in the cost, activating that ability while casting a spell with improvise will result in the artifact being tapped when you pay the spell's costs. You won't be able to tap it again for improvise. Similarly, if you sacrifice an artifact to activate a mana ability while casting a spell with improvise, that artifact won't be on the battlefield when you pay the spell's costs, so you won't be able to tap it for improvise.")
        ruling("2018-07-13", "Tapping an artifact won't cause its abilities to stop applying unless those abilities say so.")
        ruling("2018-07-13", "Equipment attached to a creature doesn't become tapped when that creature becomes tapped, and tapping that Equipment doesn't cause the creature to become tapped.")
        ruling("2018-07-13", "When using improvise to cast a spell with {X} in its mana cost, first choose the value for X. That choice, plus any cost increases or decreases, will determine the spell's total cost. Then you can tap artifacts you control to help pay that cost. For example, if you cast Saheeli's Directive and choose X to be 3, the total cost is {3}{R}{R}{R}. If you tap two artifacts, you'll have to pay {1}{R}{R}{R}.")
    }
}
