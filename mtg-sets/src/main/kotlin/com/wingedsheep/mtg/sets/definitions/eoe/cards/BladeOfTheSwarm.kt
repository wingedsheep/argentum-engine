package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Blade of the Swarm
 * {3}{B}
 * Creature — Insect Assassin
 * 3/1
 *
 * When this creature enters, choose one —
 *  • Put two +1/+1 counters on this creature.
 *  • Put target exiled card with warp on the bottom of its owner's library.
 *
 * The warp-exile-targeting mode reuses the engine's exile-zone target enumeration
 * (extended in [com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils])
 * with the existing [com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsWarpExiled]
 * marker, then composes [MoveToZoneEffect] with [ZonePlacement.Bottom] to send the
 * targeted card to its owner's library bottom — no new effect class required.
 */
val BladeOfTheSwarm = card("Blade of the Swarm") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Insect Assassin"
    power = 3
    toughness = 1
    oracleText = "When this creature enters, choose one —\n" +
        "• Put two +1/+1 counters on this creature.\n" +
        "• Put target exiled card with warp on the bottom of its owner's library."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
                "Put two +1/+1 counters on this creature",
            ),
            Mode.withTarget(
                Effects.Move(EffectTarget.ContextTarget(0), Zone.LIBRARY, ZonePlacement.Bottom),
                TargetObject(
                    filter = TargetFilter(
                        GameObjectFilter.Any.warpExiled(),
                        zone = Zone.EXILE,
                    ),
                ),
                "Put target exiled card with warp on the bottom of its owner's library",
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Nino Is"
        flavorText = "The Eumidians sought the secret of weftwalking in Drix DNA. Though they failed, their attempts yielded other gains."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b157330a-2652-4ed9-b8fa-8e72b4eda15c.jpg?1752946918"
    }
}
