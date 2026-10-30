package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustBeBlocked

/**
 * Treeshaker Chimera
 * {5}{G}{G}
 * Creature — Chimera
 * 8/5
 * All creatures able to block this creature do so.
 * When this creature dies, draw three cards.
 *
 * The Lure clause is Nessian Boar's static `MustBeBlocked(allCreatures = true)`, which binds only
 * creatures able to block it (a tapped blocker, or one whose block carries a cost, stays free). The
 * death draw is a plain `Triggers.self.dies()` into [Effects.DrawCards].
 */
val TreeshakerChimera = card("Treeshaker Chimera") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Chimera"
    power = 8
    toughness = 5
    oracleText = "All creatures able to block this creature do so.\n" +
        "When this creature dies, draw three cards."

    staticAbility {
        ability = MustBeBlocked(allCreatures = true)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(3)
        description = "When this creature dies, draw three cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "297"
        artist = "Vincent Proce"
        flavorText = "The wisdom of the past is written on the bones of ancients."
        imageUri = "https://cards.scryfall.io/normal/front/4/5/456627ec-81a4-40db-91dc-d25a59cd2837.jpg?1783931489"
        ruling(
            "2020-01-24",
            "If a creature can't block Treeshaker Chimera for any reason (such as being tapped), then it " +
                "doesn't block. If there's a cost associated with having it block, the player isn't forced to " +
                "pay that cost, so it doesn't have to block in that case either. If there's a cost associated " +
                "with having it block Treeshaker Chimera, but not to block other attacking creatures, that " +
                "creature is free to block one of those other creatures or to not block at all."
        )
        ruling(
            "2020-01-24",
            "If a creature is required to block two or more different creatures, its controller chooses which " +
                "one that creature blocks."
        )
    }
}
