package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Push the Limit — Aetherdrift #143
 * {5}{R}{R} · Sorcery
 *
 * Return all Mount and Vehicle cards from your graveyard to the battlefield. Sacrifice them at
 * the beginning of the next end step.
 * Vehicles you control become artifact creatures until end of turn. Creatures you control gain
 * haste until end of turn.
 *
 * Four steps, in printed order — the order is load-bearing, not cosmetic:
 *
 *  1. Mass reanimation. `moveTracked` is used rather than plain `move` so the pipeline holds the
 *     cards that *actually* arrived on the battlefield; a card that couldn't be returned must not
 *     be scheduled for sacrifice.
 *  2. The sacrifice clause becomes one delayed trigger *per* returned permanent, scheduled inside
 *     [ForEachInCollectionEffect] where `EffectTarget.Self` is the current permanent.
 *     `CreateDelayedTriggerExecutor` bakes that into a concrete entity id at scheduling time, so
 *     each trigger still finds its permanent at the end step. N single-permanent triggers firing
 *     together at the same step are indistinguishable from the printed one-ability-sacrifices-all:
 *     same controller, same time, same permanents. (A whole-collection reference would *not*
 *     survive — the executor bakes per-target effects, not pipeline collections, so the trigger
 *     would fire against an empty pipeline and quietly sacrifice nothing.)
 *  3. Vehicles gain the Creature card type for the turn ([Effects.AddCardType], the same Layer-4
 *     change crew makes; a Vehicle is already an artifact per CR 301.7). This runs *after* the
 *     reanimation, so the Vehicles that just came back are animated too.
 *  4. Creatures gain haste — after step 3, so the freshly animated Vehicles are creatures by the
 *     time this group is snapshotted and get haste as well. That is the whole point of the card:
 *     the wrecks come back, wake up, and attack the turn they return.
 *
 * Note the asymmetry in the last two clauses: step 3 is Vehicles only (a returned Mount is
 * already a creature and needs no animation), step 4 is every creature you control, not just the
 * returned ones — the printed text says "Creatures you control", so an unrelated board also
 * gains haste.
 */
private val MountOrVehicleCardInGraveyard = GameObjectFilter.Any
    .withAnyOfSubtypes(listOf(Subtype("Mount"), Subtype.VEHICLE))

private val VehiclesYouControl =
    GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE).youControl())

val PushTheLimit = card("Push the Limit") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Return all Mount and Vehicle cards from your graveyard to the battlefield. " +
        "Sacrifice them at the beginning of the next end step.\n" +
        "Vehicles you control become artifact creatures until end of turn. Creatures you control " +
        "gain haste until end of turn."

    spell {
        effect = Effects.Pipeline {
            val wrecks = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = MountOrVehicleCardInGraveyard,
                ),
                name = "wrecks",
            )
            val returned = moveTracked(
                wrecks,
                CardDestination.ToZone(Zone.BATTLEFIELD),
                underOwnersControl = true,
                name = "returned",
            )
            run(
                ForEachInCollectionEffect(
                    collection = returned.key,
                    effect = CreateDelayedTriggerEffect(
                        step = Step.END,
                        effect = Effects.SacrificeTarget(EffectTarget.Self),
                    ),
                )
            )
            run(
                Effects.ForEachInGroup(
                    VehiclesYouControl,
                    Effects.AddCardType("Creature", EffectTarget.Self, Duration.EndOfTurn),
                )
            )
            run(
                Effects.ForEachInGroup(
                    GroupFilter.AllCreaturesYouControl,
                    Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self, Duration.EndOfTurn),
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21de84a2-2654-4e1a-a569-bf385bb43685.jpg?1783907878"
    }
}
