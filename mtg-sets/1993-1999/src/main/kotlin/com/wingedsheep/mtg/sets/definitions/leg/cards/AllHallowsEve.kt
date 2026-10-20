package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * All Hallow's Eve
 * {2}{B}{B}
 * Sorcery
 * Exile All Hallow's Eve with two scream counters on it.
 * At the beginning of your upkeep, if this card is exiled with a scream counter on it, remove a
 * scream counter from it. If there are no more scream counters on it, put it into your graveyard
 * and each player returns all creature cards from their graveyard to the battlefield.
 *
 * The spell's only instruction re-routes its own resolution: [Effects.MarkSpellExileWithCounters]
 * aimed at [EffectTarget.Self] (the resolving spell) sends it to exile with two scream counters
 * instead of the graveyard — and only if it actually resolves, so a countered All Hallow's Eve
 * goes to the graveyard with nothing on it.
 *
 * The countdown is the suspend shape: an upkeep trigger that functions from exile
 * (`triggerZones = {EXILE}`, CR 113.6b) gated by an intervening "if" on the scream counter
 * (CR 603.4), so a copy that reached exile some other way never triggers. The move to the
 * graveyard is `fromZone = EXILE`-guarded; the mass return is Living Death's batch move,
 * each card under its owner's control.
 */
val AllHallowsEve = card("All Hallow's Eve") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Exile All Hallow's Eve with two scream counters on it.\n" +
        "At the beginning of your upkeep, if this card is exiled with a scream counter on it, " +
        "remove a scream counter from it. If there are no more scream counters on it, put it into " +
        "your graveyard and each player returns all creature cards from their graveyard to the " +
        "battlefield."

    spell {
        effect = Effects.MarkSpellExileWithCounters(
            target = EffectTarget.Self,
            counterType = CounterType.SCREAM,
            count = 2
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        triggerZones = setOf(Zone.EXILE)
        interveningIf = Conditions.All(
            Conditions.SourceInZone(Zone.EXILE),
            Conditions.SourceHasCounter(CounterType.SCREAM)
        )
        effect = Effects.RemoveCounters(CounterType.SCREAM, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.Not(Conditions.SourceHasCounter(CounterType.SCREAM)),
                then = Effects.Move(EffectTarget.Self, Zone.GRAVEYARD, fromZone = Zone.EXILE) then
                    Effects.Pipeline {
                        val dead = gather(
                            CardSource.FromZone(
                                zone = Zone.GRAVEYARD,
                                player = Player.Each,
                                filter = GameObjectFilter.Creature
                            )
                        )
                        move(dead, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
                    }
            )
        description = "At the beginning of your upkeep, if this card is exiled with a scream counter " +
            "on it, remove a scream counter from it. If there are no more scream counters on it, put " +
            "it into your graveyard and each player returns all creature cards from their graveyard " +
            "to the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18787a2d-6688-47e9-94bc-ccf229df823f.jpg?1783948069"
        ruling(
            "2009-10-01",
            "The only thing that happens when All Hallow's Eve resolves is that it's exiled with two scream counters on it."
        )
        ruling(
            "2009-10-01",
            "All Hallow's Eve's triggered ability functions from the exile zone. This ability has an \"intervening 'if' " +
                "clause.\" It won't trigger at all unless All Hallow's Eve is exiled and has a scream counter on it " +
                "(which can happen only if it resolves as a spell)."
        )
        ruling(
            "2009-10-01",
            "Each creature returned to the battlefield by All Hallow's Eve enters under its owner's control."
        )
        ruling(
            "2004-10-04",
            "Since you can respond to triggered abilities, it is legal to sacrifice creatures using some spell or " +
                "ability prior to resolving the ability that removes the final counter from All Hallow's Eve."
        )
    }
}
