package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Will Riker, First in the Field
 * {3}{R}
 * Legendary Creature — Human Officer
 * 4/3
 * At the beginning of combat on your turn, reveal cards from the top of your library until you
 *   reveal two land cards. Put those land cards onto the battlefield and the rest on the bottom of
 *   your library in a random order. Sacrifice those lands at the beginning of your next end step.
 *
 * `gatherUntilMatch(count = 2)` walks to the second land (Skyserpent Seeker's shape); the lands
 * that actually entered (`moveTracked`) each get a delayed end-step sacrifice (Push the Limit's
 * shape). The trigger only fires during your own turn's combat, so "the next end step" the delayed
 * trigger waits for is always your end step this turn.
 */
val WillRikerFirstInTheField = card("Will Riker, First in the Field") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Officer"
    oracleText = "At the beginning of combat on your turn, reveal cards from the top of your library " +
        "until you reveal two land cards. Put those land cards onto the battlefield and the rest on the " +
        "bottom of your library in a random order. Sacrifice those lands at the beginning of your next " +
        "end step."
    power = 4
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.Pipeline {
            val (_, allRevealed) = gatherUntilMatch(GameObjectFilter.Land, count = DynamicAmounts.fixed(2))
            reveal(allRevealed)
            val (lands, rest) = filterSplit(allRevealed, GameObjectFilter.Land)
            val entered = moveTracked(lands, CardDestination.ToZone(Zone.BATTLEFIELD))
            toLibraryBottom(rest, order = CardOrder.Random)
            run(
                Effects.ForEachInCollection(
                    entered,
                    Effects.CreateDelayedTrigger(
                        step = Step.END,
                        effect = Effects.SacrificeTarget(EffectTarget.IterationEntity),
                    ),
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/9422d025-9460-44b6-8071-d896f20b87ea.jpg?1793013606"
    }
}
