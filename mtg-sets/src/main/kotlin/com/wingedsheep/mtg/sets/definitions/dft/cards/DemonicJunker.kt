package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Demonic Junker — Aetherdrift #83
 * {6}{B} · Artifact — Vehicle · 4/3
 *
 * Affinity for artifacts
 * When this Vehicle enters, for each player, destroy up to one target creature that player
 * controls. If a creature you controlled was destroyed this way, put two +1/+1 counters on this
 * Vehicle.
 * Crew 2
 *
 * Modeling notes:
 *
 *  - **"For each player, destroy up to one target creature that player controls"** — a *targeted*
 *    per-player fan-out, rendered as one optional target slot per player, the same two-slot
 *    approximation Kitesail Larcenist and Unstable Glyphbridge document for two-player games:
 *    `yours` (up to one creature you control) and `theirs` (up to one creature an opponent
 *    controls). A creature has exactly one controller, so the two control-scoped filters can never
 *    overlap and the same creature can't fill both slots. Both are `optional = true` — "up to one"
 *    means the controller may pick none for a player.
 *
 *  - **One simultaneous destruction, not two.** Both slots are gathered together and destroyed in a
 *    single [MoveType.Destroy] move, so the creatures die at the same time (CR 701.7b) and any
 *    "whenever another creature dies" trigger sees one batch rather than two sequential deaths.
 *
 *  - **"If a creature you controlled was destroyed this way"** — *destroyed*, not merely targeted.
 *    An indestructible or regenerated creature, or one that left the battlefield in response, is
 *    not destroyed, so it must not pay off. The pipeline partitions the chosen targets into yours /
 *    theirs *before* the move (a permanent's `ControllerComponent` is stripped on the way to the
 *    graveyard), destroys the union with `moveTracked` to record which ones actually died, then
 *    subtracts the opponent's half — what remains are the creatures *you* controlled that were
 *    genuinely destroyed. Non-empty ⇒ the two counters go on.
 *
 *  - **The counters land on the Vehicle itself** ([EffectTarget.Self]) and only if it's still on
 *    the battlefield; the trigger targeting its own controller's creature can't have destroyed it,
 *    since a Vehicle isn't a creature unless crewed.
 */
val DemonicJunker = card("Demonic Junker") {
    manaCost = "{6}{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Vehicle"
    oracleText = "Affinity for artifacts (This spell costs {1} less to cast for each artifact you " +
        "control.)\n" +
        "When this Vehicle enters, for each player, destroy up to one target creature that player " +
        "controls. If a creature you controlled was destroyed this way, put two +1/+1 counters on " +
        "this Vehicle.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"
    power = 4
    toughness = 3

    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield

        // "for each player, destroy up to one target creature that player controls"
        // (two-player rendering: one optional slot per player — see KDoc).
        target(
            "up to one target creature you control",
            TargetPermanent(filter = TargetFilter.Creature.youControl(), optional = true)
        )
        target(
            "up to one target creature an opponent controls",
            TargetPermanent(filter = TargetFilter.Creature.opponentControls(), optional = true)
        )

        effect = Effects.Pipeline(
            descriptionOverride = "For each player, destroy up to one target creature that player " +
                "controls. If a creature you controlled was destroyed this way, put two +1/+1 " +
                "counters on this Vehicle."
        ) {
            val chosen = gather(CardSource.ChosenTargets)
            // Split before destroying: controller information is gone once a permanent hits the
            // graveyard.
            val split = filterSplit(chosen, GameObjectFilter.Creature.youControl())
            val destroyed = moveTracked(
                chosen,
                CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Destroy
            )
            // Everything actually destroyed, minus the opponent's half ⇒ the creatures you
            // controlled that were destroyed this way.
            val destroyedYours = exclude(destroyed, split.rest)
            ifNotEmpty(destroyedYours) {
                run(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self))
            }
        }
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Stephan Martiniere"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4aad569e-4acb-4416-9d4f-64e6991de3ed.jpg?1783907896"
    }
}
