package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Start Your Engines — Kaladesh #135
 * {3}{R} · Sorcery
 *
 * Vehicles you control become artifact creatures until end of turn. Creatures you control get
 * +2/+0 until end of turn.
 *
 * Push the Limit's two-step shape, in printed order:
 *  1. Every Vehicle you control gains the Creature type until end of turn ([Effects.AddCardType],
 *     the same change crew makes — a Vehicle is already an artifact and keeps its printed P/T).
 *  2. Creatures you control get +2/+0 — snapshotted *after* step 1, so the freshly animated
 *     Vehicles are in the group and get the bonus too (2016-09-20 ruling).
 * Both groups are fixed as the spell resolves; permanents you gain later in the turn aren't
 * affected.
 */
private val VehiclesYouControl =
    GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE).youControl())

val StartYourEngines = card("Start Your Engines") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Vehicles you control become artifact creatures until end of turn. Creatures you " +
        "control get +2/+0 until end of turn."

    spell {
        effect = Effects.Pipeline {
            run(
                Effects.ForEachInGroup(
                    VehiclesYouControl,
                    Effects.AddCardType("Creature", EffectTarget.IterationEntity, Duration.EndOfTurn),
                )
            )
            run(
                Effects.ForEachInGroup(
                    GroupFilter.AllCreaturesYouControl,
                    Effects.ModifyStats(2, 0, EffectTarget.IterationEntity),
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Darek Zabrocki"
        flavorText = "Inventors from around the world have come to the Fair hoping their creations " +
            "will fly, drive, or sail their way to a top prize."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbae1eb8-697d-43e1-b03e-f4246a9f225e.jpg?1783937187"

        ruling("2016-09-20", "Vehicles you control become artifact creatures and then also get +2/+0.")
        ruling("2016-09-20", "The set of Vehicles and creatures affected by Start Your Engines is determined as the spell resolves. Creatures you begin to control later in the turn won't get +2/+0, and Vehicles you begin to control later in the turn won't automatically become creatures and won't get +2/+0.")
    }
}
