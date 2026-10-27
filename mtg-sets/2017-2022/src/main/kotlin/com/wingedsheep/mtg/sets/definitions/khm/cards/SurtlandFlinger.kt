package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Surtland Flinger — Kaldheim #377
 * {3}{R}{R} · Creature — Giant Berserker · Rare · 4/6
 *
 * Whenever this creature attacks, you may sacrifice another creature. When you do, this creature
 * deals damage equal to the sacrificed creature's power to any target. If the sacrificed creature
 * was a Giant, this creature deals twice that much damage instead.
 *
 * Ziatora's shape: the sacrifice is a resolution-time, non-targeting choice, and only an actual
 * sacrifice fires the CR 603.12 "When you do" reflexive ability, whose "any target" is chosen as it
 * goes on the stack. The damage amount — the creature's power as it last existed on the
 * battlefield, doubled if it was a Giant (2021-02-05 ruling) — is snapshotted into the pipeline
 * while the creature is still on the battlefield, so counters, pumps and type-changing effects
 * (Changeling) all count, and read back once the reflexive ability resolves.
 */
val SurtlandFlinger = card("Surtland Flinger") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant Berserker"
    power = 4
    toughness = 6
    oracleText = "Whenever this creature attacks, you may sacrifice another creature. When you do, " +
        "this creature deals damage equal to the sacrificed creature's power to any target. If the " +
        "sacrificed creature was a Giant, this creature deals twice that much damage instead."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val toSacrifice = selectTarget(
                    TargetObject(filter = TargetFilter.CreatureYouControl.other()),
                    nonTargeting = true,
                    prompt = "Choose another creature to sacrifice"
                )
                val power = DynamicAmounts.powerOf(toSacrifice.asTarget)
                // Named: the reflexive trigger below reads it once this pipeline has finished.
                storeNumber(
                    DynamicAmounts.conditional(
                        Conditions.CollectionContainsMatch(
                            toSacrifice,
                            GameObjectFilter.Creature.withSubtype("Giant")
                        ),
                        power * 2,
                        power,
                    ),
                    name = "surtlandFlingerDamage"
                )
                run(Effects.SacrificeTarget(toSacrifice.asTarget))
            },
            optional = true,
            descriptionOverride = "You may sacrifice another creature. When you do, Surtland Flinger " +
                "deals damage equal to that creature's power to any target, or twice that much if it " +
                "was a Giant.",
        ) {
            val any = target(Targets.Any)
            effect = Effects.DealDamage(DynamicAmounts.storedNumber("surtlandFlingerDamage"), any)
        }
        description = "Whenever Surtland Flinger attacks, you may sacrifice another creature. When you " +
            "do, Surtland Flinger deals damage equal to the sacrificed creature's power to any target. " +
            "If the sacrificed creature was a Giant, Surtland Flinger deals twice that much damage instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "377"
        artist = "Dmitry Burmak"
        flavorText = "Victory before camaraderie."
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a628052-8175-495f-bfab-eeb3a0388e4e.jpg?1783928121"
        ruling(
            "2021-02-05",
            "Surtland Flinger's triggered ability goes on the stack without a target. While that " +
                "ability is resolving, you may sacrifice another creature. When you do, the reflexive " +
                "triggered ability triggers and you pick a target to be dealt damage. Players may take " +
                "actions after you've sacrificed the creature but before damage is dealt."
        )
        ruling(
            "2021-02-05",
            "Use the power of the creature as it was sacrificed to determine how much damage is " +
                "dealt. Damage will be dealt before blockers are declared."
        )
    }
}
