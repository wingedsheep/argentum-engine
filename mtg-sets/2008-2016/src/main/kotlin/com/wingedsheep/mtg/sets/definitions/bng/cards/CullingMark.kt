package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Culling Mark
 * {2}{G}
 * Sorcery
 *
 * Target creature blocks this turn if able.
 *
 * Canonical printing: Born of the Gods, the card's only printing. "Blocks this turn if able" is
 * [Effects.MarkMustBlockThisTurn] — a blocking requirement checked as declare blockers begins, not a
 * guarantee (a tapped creature, or one with no attacker to block, simply doesn't block).
 */
val CullingMark = card("Culling Mark") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature blocks this turn if able."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.MarkMustBlockThisTurn(creature)
    }

    metadata {
        ruling(
            "2014-02-01",
            "The creature blocks only if it's able to do so as the declare blockers step begins. If, at that " +
                "time, the creature is tapped, it's affected by a spell or ability that says it can't block, or " +
                "no creatures are attacking its controller or a planeswalker controlled by that player, then it " +
                "doesn't block. If there's a cost associated with having the creature block, the player isn't " +
                "forced to pay that cost. If that cost isn't paid, the creature won't block."
        )
        ruling("2014-02-01", "The controller of the creature chooses which attacking creature that creature blocks.")
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Tomasz Jedruszek"
        flavorText = "Hunt without Nylea's leave and you may find yourself the next quarry."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4c45eda-9f00-4b40-b91d-0ed00151923c.jpg?1783939536"
    }
}
