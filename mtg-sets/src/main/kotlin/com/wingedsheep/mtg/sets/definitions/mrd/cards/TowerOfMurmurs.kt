package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tower of Murmurs — Mirrodin #268
 * {4} · Artifact
 *
 * {8}, {T}: Target player mills eight cards.
 *
 * Modelling notes:
 * - Printed as "Target player puts the top eight cards of their library into their graveyard";
 *   the modern Oracle wording is the mill keyword action (CR 701.13), which is what
 *   [Patterns.Library.mill] models. It is *not* a cost and not optional.
 * - Any player is a legal target, including its controller — self-mill is a real (if grim) use.
 * - A library with fewer than eight cards simply mills what it has; the player doesn't lose
 *   until they next try to draw from an empty library (CR 104.3c).
 */
val TowerOfMurmurs = card("Tower of Murmurs") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{8}, {T}: Target player mills eight cards."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{8}"), Costs.Tap)
        val player = target("target player", Targets.Player)
        effect = Patterns.Library.mill(8, player)
        description = "{8}, {T}: Target player mills eight cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "268"
        artist = "Glen Angus"
        flavorText = "Etched on its surface are warnings from a long-lost race of ur-golems " +
            "pushed to the brink of extinction."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76be1ca8-b68e-436d-86b6-2a2a07da1be9.jpg?1783944497"
    }
}
