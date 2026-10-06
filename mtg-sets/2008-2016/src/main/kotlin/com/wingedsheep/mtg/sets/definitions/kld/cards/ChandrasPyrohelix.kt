package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Chandra's Pyrohelix
 * {1}{R}
 * Instant
 * Chandra's Pyrohelix deals 2 damage divided as you choose among one or two targets.
 *
 * Canonical definition lives in KLD (earliest real-expansion printing). WAR and J22
 * are reprints that add only a [com.wingedsheep.sdk.model.Printing] row.
 */
val ChandrasPyrohelix = card("Chandra's Pyrohelix") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Chandra's Pyrohelix deals 2 damage divided as you choose among one or two targets."

    spell {
        target = AnyTarget(count = 2, minCount = 1)
        effect = Effects.DividedDamage(
            total = 2,
            minTargets = 1,
            maxTargets = 2
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Kieran Yanner"
        flavorText = "\"This one's for my mom, and this one's for me.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05659715-3002-4bd0-919e-664814c1ca57.jpg?1783937196"
        ruling("2019-05-03", "You divide the damage as you cast Chandra's Pyrohelix, not as it resolves. Each target must be assigned at least 1 damage. In other words, as you cast Chandra's Pyrohelix, you choose whether to have it deal 2 damage to a single target, or deal 1 damage to each of two targets.")
        ruling("2019-05-03", "If Chandra's Pyrohelix targets two creatures and one becomes an illegal target, the remaining target is dealt 1 damage, not 2.")
    }
}
