package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Perilous Snare — Aetherdrift #23
 * {2}{W} · Artifact
 *
 * Start your engines!
 * When this artifact enters, exile target nonland permanent an opponent controls until this
 * artifact leaves the battlefield.
 * Max speed — {T}: Put a +1/+1 counter on target creature or Vehicle you control. Activate only as
 * a sorcery.
 *
 * An Oblivion Ring on the exile side: [Effects.ExileUntilLeaves] parks the permanent in the
 * source's linked-exile pile, and the paired leaves-the-battlefield trigger returns it under its
 * *owner's* control (CR 610.3) — so bouncing the Snare in response to its own ETB trigger still
 * gives the card back.
 *
 * The second ability lives in a [maxSpeed] block, which folds "your speed is 4" into the ability's
 * activation condition rather than checking it once — dropping out of max speed turns the ability
 * off. `TimingRule.SorcerySpeed` carries the "Activate only as a sorcery" rider.
 */
val PerilousSnare = card("Perilous Snare") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "When this artifact enters, exile target nonland permanent an opponent controls until this " +
        "artifact leaves the battlefield.\n" +
        "Max speed — {T}: Put a +1/+1 counter on target creature or Vehicle you control. Activate " +
        "only as a sorcery."

    startYourEngines()

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val exiled = target(
            "exiled",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.NonlandPermanent.opponentControls()))
        )
        effect = Effects.ExileUntilLeaves(exiled)
        description = "When this artifact enters, exile target nonland permanent an opponent " +
            "controls until this artifact leaves the battlefield."
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    maxSpeed {
        activatedAbility {
            cost = Costs.Tap
            val boosted = target(
                "boosted",
                TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle.youControl()))
            )
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, boosted)
            timing = TimingRule.SorcerySpeed
            description = "Put a +1/+1 counter on target creature or Vehicle you control."
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47f7e468-2196-4960-a612-37ab326e2a17.jpg?1783907916"
    }
}
