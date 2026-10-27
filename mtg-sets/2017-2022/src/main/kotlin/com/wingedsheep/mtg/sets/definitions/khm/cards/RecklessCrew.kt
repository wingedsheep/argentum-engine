package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reckless Crew — Kaldheim #146.
 * {3}{R} · Sorcery
 *
 * Create X 2/1 red Dwarf Berserker creature tokens, where X is the number of Vehicles you control
 * plus the number of Equipment you control. For each of those tokens, you may attach an Equipment
 * you control to it.
 *
 * X is two battlefield counts added together, read as the spell resolves (2021-02-05 ruling) — a
 * permanent that is both a Vehicle and an Equipment counts once for each.
 *
 * "For each of those tokens, you may attach an Equipment you control to it" pairs Equipment with
 * the new tokens one-to-one: no token gets more than one Equipment, and no Equipment can go to two
 * tokens (2021-02-05 rulings). The pipeline picks the Equipment side first — up to one per token
 * created — then gives each chosen Equipment to a still-unequipped token from this spell's
 * [CollectionSlot.CreatedTokens]. The tokens are brand new, so "unequipped" is exactly "hasn't been
 * given an Equipment by this spell yet". An Equipment that can't legally equip a token is skipped
 * by the attach executor and stays where it was. The attaches all happen inside this one resolution,
 * with no priority or state-based-action check between them.
 */
val RecklessCrew = card("Reckless Crew") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Create X 2/1 red Dwarf Berserker creature tokens, where X is the number of " +
        "Vehicles you control plus the number of Equipment you control. For each of those tokens, " +
        "you may attach an Equipment you control to it."

    spell {
        effect = Effects.Pipeline {
            run(
                Effects.CreateToken(
                    count = DynamicAmounts.battlefield(
                        Player.You,
                        GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE),
                    ).count() + DynamicAmounts.battlefield(
                        Player.You,
                        GameObjectFilter.Permanent.withSubtype(Subtype.EQUIPMENT),
                    ).count(),
                    power = 2,
                    toughness = 1,
                    colors = setOf(Color.RED),
                    creatureTypes = setOf("Dwarf", "Berserker"),
                )
            )
            val tokens = CollectionSlot.CreatedTokens
            val equipment = gather(
                GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT),
                player = Player.You,
            )
            val chosen = chooseUpTo(
                DynamicAmounts.distinctEntitiesIn(tokens),
                from = equipment,
                useTargetingUI = true,
                prompt = "Choose up to one Equipment you control for each Dwarf Berserker token",
            )
            run(
                Effects.ForEachInCollection(
                    chosen,
                    Effects.Pipeline {
                        val token = chooseExactly(
                            1,
                            from = tokens,
                            filter = GameObjectFilter.Creature
                                .withStatePredicate(StatePredicate.Not(StatePredicate.IsEquipped)),
                            useTargetingUI = true,
                            prompt = "Choose a Dwarf Berserker token to attach this Equipment to",
                        )
                        run(
                            Effects.AttachTargetEquipmentToCreature(
                                equipmentTarget = EffectTarget.IterationEntity,
                                creatureTarget = token.asTarget,
                            )
                        )
                    }
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Izzy"
        flavorText = "\"Boat's burning! Row faster!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bdd68d9d-b0b9-4915-8a12-658eb0e3dd4a.jpg?1783928224"

        ruling("2021-02-05", "Count the number of Vehicles and Equipment you control as Reckless Crew resolves to determine how many Dwarf Berserkers to create.")
        ruling("2021-02-05", "All Equipment you decide to attach to tokens become attached at the same time.")
        ruling("2021-02-05", "You can't have more than one Equipment become attached to the same token.")
    }
}
