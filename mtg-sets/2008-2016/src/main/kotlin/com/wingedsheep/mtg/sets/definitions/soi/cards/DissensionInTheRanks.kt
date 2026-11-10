package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dissension in the Ranks
 * {3}{R}{R}
 * Instant
 * Target blocking creature fights another target blocking creature.
 *
 * Clash of Titans's shape narrowed to blockers: one two-slot blocking-creature requirement — its
 * slots must be distinct objects, which is the "another". The two may share a controller.
 */
val DissensionInTheRanks = card("Dissension in the Ranks") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target blocking creature fights another target blocking creature."

    spell {
        val (first, second) = targets(TargetFilter.BlockingCreature, count = 2)
        effect = Effects.Fight(first, second)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Jakub Kasper"
        flavorText = "\"It came on as quickly as taking in a breath of air. Cathars who had fought " +
            "shoulder to shoulder were at each other's throats. They were not themselves.\"\n" +
            "—Halmig, Order of Saint Traft"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c1b563a-7856-40bf-954a-a0110ed4880d.jpg?1783937758"
        ruling("2016-04-08", "Destroying a blocking creature doesn't cause the attacking creature it blocked to become unblocked.")
    }
}
