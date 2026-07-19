package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gnawing Crescendo
 * {2}{R}
 * Instant
 *
 * Creatures you control get +2/+0 until end of turn. Whenever a nontoken creature you control dies
 * this turn, create a 1/1 black Rat creature token with "This token can't block."
 *
 * Two clauses composed:
 *  - The team pump — [Patterns.Group.modifyStatsForAll] over creatures you control (+2/+0, end of
 *    turn), same shape as Rabbit Response / Valley Rally.
 *  - A turn-duration event-based delayed trigger ([CreateDelayedTriggerEffect] with
 *    `expiry = EndOfTurn`, `fireOnce = false`, mirroring Waltz of Rage) that fires once per
 *    nontoken-creature-you-control death this turn, each firing making the shared [woeRatToken].
 *    The per-creature `leavesBattlefield` spec (ANY binding, nontoken filter) matches the singular
 *    "a nontoken creature" wording — a board wipe fires it once per qualifying creature.
 */
val GnawingCrescendo = card("Gnawing Crescendo") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Creatures you control get +2/+0 until end of turn. Whenever a nontoken creature " +
        "you control dies this turn, create a 1/1 black Rat creature token with \"This token can't block.\""

    spell {
        effect = Patterns.Group.modifyStatsForAll(
            2, 0,
            GroupFilter(GameObjectFilter.Creature.youControl())
        ).then(
            CreateDelayedTriggerEffect(
                trigger = Triggers.leavesBattlefield(
                    filter = GameObjectFilter.Creature.youControl().nontoken(),
                    to = Zone.GRAVEYARD,
                    binding = TriggerBinding.ANY
                ),
                expiry = DelayedTriggerExpiry.EndOfTurn,
                fireOnce = false,
                effect = woeRatToken()
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Alexey Kruglov"
        flavorText = "Totentanz's song had no lyrics, but the rats understood it nonetheless. " +
            "\"You own this land,\" it said. \"You deserve to feast.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/5/254fc64a-9734-44a6-8869-ab03512f1a99.jpg?1783915094"
    }
}
