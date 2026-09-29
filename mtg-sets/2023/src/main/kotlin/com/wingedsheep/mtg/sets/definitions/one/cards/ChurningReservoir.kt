package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Churning Reservoir
 * {R}
 * Artifact
 *
 * At the beginning of your upkeep, put an oil counter on another target nontoken artifact or
 * creature you control.
 * {2}, {T}: Create a 1/1 red Phyrexian Goblin creature token. Activate only if an oil counter was
 * removed from a permanent you controlled this turn or a permanent with an oil counter on it was
 * put into a graveyard this turn.
 *
 * The activation gate is two pieces of turn history: an oil counter leaving a permanent you
 * controlled (any removal path, recorded at the settle boundary), or any player's permanent going
 * to a graveyard with an oil counter on it (read off its last-known counters).
 */
val ChurningReservoir = card("Churning Reservoir") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "At the beginning of your upkeep, put an oil counter on another target nontoken " +
        "artifact or creature you control.\n" +
        "{2}, {T}: Create a 1/1 red Phyrexian Goblin creature token. Activate only if an oil counter " +
        "was removed from a permanent you controlled this turn or a permanent with an oil counter on " +
        "it was put into a graveyard this turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val recipient = target(TargetFilter(GameObjectFilter.CreatureOrArtifact.nontoken().youControl()).other())
        effect = Effects.AddCounters(CounterType.OIL, 1, recipient)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.Any(
                    Conditions.CounterRemovedFromPermanentYouControlledThisTurn(CounterType.OIL),
                    Conditions.PermanentWithCounterPutIntoGraveyardThisTurn(CounterType.OIL),
                )
            )
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Phyrexian", "Goblin"),
            imageUri = "https://cards.scryfall.io/normal/front/3/6/3663e79b-2bf9-44af-a638-c0ad9067d8d4.jpg?1783918169",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c117239-8e40-4e7e-ab1b-82f2ddf55cf4.jpg?1783918032"
    }
}
