package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AddLandTypeByCounter
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetChooser
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Quicksilver Fountain — Mirrodin #233 (canonical printing)
 * {3} · Artifact · Rare
 *
 * At the beginning of each player's upkeep, that player puts a flood counter on target non-Island
 * land they control of their choice. That land is an Island for as long as it has a flood counter
 * on it.
 * At the beginning of each end step, if all lands on the battlefield are Islands, remove all flood
 * counters from them.
 *
 * Modelling notes:
 * - **"That player … of their choice" is the whole card.** The trigger belongs to the Fountain's
 *   controller but the *choice* belongs to whoever's upkeep it is, and the land must be one *they*
 *   control — so a Fountain you play still floods your own lands on your own turns. That routing is
 *   [TargetChooser.TriggeringPlayer]: the land stays a target of the controller's ability (CR 115
 *   legality, respondable on the stack) and only the selection decision goes to the other player.
 *   Modelling it as a resolution-time pipeline selection instead would have quietly dropped the
 *   targeting — shroud, and the trigger's removal when a player controls only Islands.
 * - The "they control" half is [com.wingedsheep.sdk.scripting.predicates.ControllerPredicate]
 *   `ControlledByTriggeringPlayer`, which already resolves through the triggering entity for a step
 *   trigger (a step trigger's triggering entity *is* the active player). It is deliberately the same
 *   player the chooser resolves to, so the decision can never offer a land the chooser doesn't own.
 * - **The Island half is free**: [AddLandTypeByCounter] is a global Layer 4 static keyed on the
 *   counter, so it applies to every flooded land regardless of who flooded it, and it stops applying
 *   the instant the counter comes off. That is what "for as long as it has a flood counter on it"
 *   means, and it is *why* the second ability only has to remove counters — it never has to undo a
 *   type change.
 * - "Non-Island" reads *projected* subtypes, so an already-flooded land is not a legal target again:
 *   the Layer 4 static has made it an Island. This is the printed lock-out that makes the card
 *   eventually flood a whole board rather than pile counters on one land.
 * - "If all lands on the battlefield are Islands" is counted rather than quantified — zero
 *   non-Island lands across *every* player's battlefield. That reading is also correct in the
 *   degenerate case: with no lands at all the condition holds, and removing counters from nothing is
 *   a no-op. As an intervening-if it is checked both when the trigger would go on the stack and
 *   again as it resolves (CR 603.4).
 * - "Remove all flood counters from them" is [Effects.ForEachInGroup] over every land rather than a
 *   single target, because the clause is plural and the lands belong to different players.
 */
val QuicksilverFountain = card("Quicksilver Fountain") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "At the beginning of each player's upkeep, that player puts a flood counter on " +
        "target non-Island land they control of their choice. That land is an Island for as long " +
        "as it has a flood counter on it.\n" +
        "At the beginning of each end step, if all lands on the battlefield are Islands, remove " +
        "all flood counters from them."

    // "At the beginning of each player's upkeep, that player puts a flood counter on target
    // non-Island land they control of their choice."
    triggeredAbility {
        trigger = Triggers.phase(Step.UPKEEP, Player.Each)
        val land = target(
            "non-Island land they control",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Land
                        .notSubtype(Subtype.ISLAND)
                        .controlledByTriggeringPlayer()
                ),
                chooser = TargetChooser.TriggeringPlayer
            )
        )
        effect = Effects.AddCounters(Counters.FLOOD, 1, land)
        description = "At the beginning of each player's upkeep, that player puts a flood counter " +
            "on target non-Island land they control of their choice."
    }

    // "That land is an Island for as long as it has a flood counter on it."
    staticAbility {
        ability = AddLandTypeByCounter(landType = "Island", counterType = Counters.FLOOD)
    }

    // "At the beginning of each end step, if all lands on the battlefield are Islands, remove all
    // flood counters from them."
    triggeredAbility {
        trigger = Triggers.phase(Step.END, Player.Each)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmount.AggregateBattlefield(
                Player.Each,
                GameObjectFilter.Land.notSubtype(Subtype.ISLAND)
            ),
            ComparisonOperator.EQ,
            DynamicAmount.Fixed(0)
        )
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllLands,
            effect = Effects.RemoveAllCountersOfType(Counters.FLOOD, EffectTarget.Self)
        )
        description = "At the beginning of each end step, if all lands on the battlefield are " +
            "Islands, remove all flood counters from them."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "233"
        artist = "Trevor Hairsine"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4f2bf63-e3cf-4a58-86b7-f3bab3b90712.jpg"
    }
}
