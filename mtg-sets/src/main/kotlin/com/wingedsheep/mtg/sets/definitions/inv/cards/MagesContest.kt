package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetSpell

/**
 * Mages' Contest
 * {1}{R}{R}
 * Instant
 * You and target spell's controller bid life. You start the bidding with a bid of 1.
 * In turn order, each player may top the high bid. The bidding ends if the high bid stands.
 * The high bidder loses life equal to the high bid. If you win the bidding, counter that spell.
 */
val MagesContest = card("Mages' Contest") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "You and target spell's controller bid life. You start the bidding with a bid of 1. " +
        "In turn order, each player may top the high bid. The bidding ends if the high bid stands. " +
        "The high bidder loses life equal to the high bid. If you win the bidding, counter that spell."

    spell {
        target = TargetSpell()
        effect = Effects.OpenLifeBid(Effects.CounterSpell(), Player.ControllerOf("target spell"))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "154"
        artist = "Bradley Williams"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c516861c-68d9-4d02-a343-689dba0526c6.jpg?1562934507"
    }
}
