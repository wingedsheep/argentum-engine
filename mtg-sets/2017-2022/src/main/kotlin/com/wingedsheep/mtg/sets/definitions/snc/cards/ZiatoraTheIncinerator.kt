package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ziatora, the Incinerator — Streets of New Capenna #231
 * {3}{B}{R}{G} · Legendary Creature — Demon Dragon · Mythic · 6/6
 *
 * Flying
 * At the beginning of your end step, you may sacrifice another creature. When you do, Ziatora
 * deals damage equal to that creature's power to any target and you create three Treasure tokens.
 *
 * Modeling notes (Bolg of the North's shape):
 *  - The sacrifice is a resolution-time choice ([Effects.Pipeline] select + [Effects.SacrificeTarget]),
 *    not a target; only an actual sacrifice fires the CR 603.12 "When you do" reflexive ability,
 *    whose "any target" is chosen as it goes on the stack.
 *  - "That creature's power" is last-known information (ruling 2022-04-29). The reflexive ability
 *    resolves from a fresh context after a stack round-trip, so the power is snapshotted into the
 *    pipeline via `storeNumber` while the creature is still on the battlefield (counters and pumps
 *    included) and read back with [DynamicAmounts.storedNumber].
 */
val ZiatoraTheIncinerator = card("Ziatora, the Incinerator") {
    manaCost = "{3}{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Legendary Creature — Demon Dragon"
    power = 6
    toughness = 6
    oracleText = "Flying\nAt the beginning of your end step, you may sacrifice another creature. " +
        "When you do, Ziatora deals damage equal to that creature's power to any target and you " +
        "create three Treasure tokens."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val toSacrifice = selectTarget(
                    TargetObject(filter = TargetFilter.CreatureYouControl.other()),
                    nonTargeting = true,
                    prompt = "Choose another creature to sacrifice"
                )
                // Named: the reflexive trigger below reads it once this pipeline has finished.
                storeNumber(
                    DynamicAmounts.powerOf(toSacrifice.asTarget),
                    name = "ziatoraSacrificedPower"
                )
                run(Effects.SacrificeTarget(toSacrifice.asTarget))
            },
            optional = true,
            descriptionOverride = "You may sacrifice another creature. When you do, Ziatora deals " +
                "damage equal to that creature's power to any target and you create three " +
                "Treasure tokens.",
        ) {
            val any = target(Targets.Any)
            effect = Effects.DealDamage(
                DynamicAmounts.storedNumber("ziatoraSacrificedPower"),
                any,
            ) then Effects.CreateTreasure(3)
        }
        description = "At the beginning of your end step, you may sacrifice another creature. " +
            "When you do, Ziatora deals damage equal to that creature's power to any target and " +
            "you create three Treasure tokens."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "231"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba4fa4c8-f09f-4d97-a7d1-1b93caf7d4f9.jpg?1783923066"
        ruling(
            "2022-04-29",
            "Use the creature's power as it last existed on the battlefield to determine how much " +
                "damage Ziatora deals."
        )
    }
}
