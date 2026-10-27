package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Armed and Armored — Kaldheim #379 (earliest printing; reprinted in NEC).
 * {1}{W} · Instant
 *
 * Vehicles you control become artifact creatures until end of turn. Choose a Dwarf you control.
 * Attach any number of Equipment you control to it.
 *
 * Three steps in printed order:
 *  1. Every Vehicle you control as the spell resolves gains the Creature type until end of turn
 *     (Push the Limit's animation; a Vehicle is already an artifact). Vehicles you gain later in
 *     the turn aren't affected (2021-02-05 ruling).
 *  2. "Choose a Dwarf you control" is not a target — a mid-resolution, non-targeting
 *     `selectTarget` (Crush Underfoot's shape), so a hexproof/shroud Dwarf can be chosen and an
 *     opponent can't respond to the choice. Running after step 1 means an animated Dwarf Vehicle
 *     is a legal recipient.
 *  3. Any number (0..all) of your Equipment are chosen and each attached to that Dwarf
 *     (Beatrix, Loyal General's shape); an illegal attach is skipped by the attach executor.
 *
 * With no Dwarf, the selection stores nothing and every attach is a no-op — whatever part of the
 * effect can happen does (2021-02-05 ruling).
 */
val ArmedAndArmored = card("Armed and Armored") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Vehicles you control become artifact creatures until end of turn. Choose a Dwarf " +
        "you control. Attach any number of Equipment you control to it."

    spell {
        effect = Effects.Pipeline {
            run(
                Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE).youControl()),
                    Effects.AddCardType("Creature", EffectTarget.IterationEntity, Duration.EndOfTurn),
                )
            )
            val dwarf = selectTarget(
                TargetObject(filter = TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.DWARF).youControl())),
                nonTargeting = true,
                prompt = "Choose a Dwarf you control to attach Equipment to",
            )
            val equipment = gather(
                GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT),
                player = Player.You,
            )
            val chosen = chooseAnyNumber(
                from = equipment,
                useTargetingUI = true,
                prompt = "Choose any number of Equipment you control to attach to the chosen Dwarf",
            )
            run(
                Effects.ForEachInCollection(
                    chosen,
                    Effects.AttachTargetEquipmentToCreature(
                        equipmentTarget = EffectTarget.IterationEntity,
                        creatureTarget = dwarf.asTarget,
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "379"
        artist = "Svetlin Velinov"
        flavorText = "The dwarves of Axgard seldom have reason to leave their impregnable underground " +
            "kingdom—but when they do, they go in force."
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f4c1253-7eba-454b-9269-3695ba746a7a.jpg?1783928122"

        ruling("2021-02-05", "You can cast Armed and Armored even if you don't control any Vehicles or Dwarves. Whatever part of the effect that can happen does, and the rest is ignored.")
        ruling("2021-02-05", "The set of Vehicles affected by Armed and Armored is determined as the spell resolves. Vehicles you begin to control later in the turn won't automatically become creatures.")
    }
}
