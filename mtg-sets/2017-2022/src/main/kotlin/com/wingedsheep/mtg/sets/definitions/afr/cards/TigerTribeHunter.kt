package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tiger-Tribe Hunter
 * {3}{R}{R}
 * Creature — Human Barbarian
 * 4/4
 * Trample
 * Pack tactics — Whenever this creature attacks, if you attacked with creatures with total power 6
 * or greater this combat, you may sacrifice another creature. When you do, this creature deals
 * damage equal to the sacrificed creature's power to target creature.
 *
 * Modeling notes:
 *  - Pack tactics is the attack trigger gated by [AttackPredicate.AttackersTotalPowerAtLeast]
 *    (Gnoll Hunter's shape).
 *  - The sacrifice is Bolg of the North's shape: a resolution-time choice, not a target, so the
 *    trigger goes on the stack untargeted and only an actual sacrifice fires the "When you do"
 *    reflexive ability, which chooses its target creature then (ruling 2021-07-23).
 *  - "The sacrificed creature's power" is last-known information, so it is stored into the
 *    pipeline while the creature is still on the battlefield and read back by the reflexive.
 */
val TigerTribeHunter = card("Tiger-Tribe Hunter") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Barbarian"
    oracleText = "Trample\nPack tactics — Whenever this creature attacks, if you attacked with " +
        "creatures with total power 6 or greater this combat, you may sacrifice another creature. " +
        "When you do, this creature deals damage equal to the sacrificed creature's power to " +
        "target creature."
    power = 4
    toughness = 4

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackersTotalPowerAtLeast(6)))
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
                    name = "tigerTribeHunterSacrificedPower"
                )
                run(Effects.SacrificeTarget(toSacrifice.asTarget))
            },
            optional = true,
            descriptionOverride = "You may sacrifice another creature. When you do, Tiger-Tribe " +
                "Hunter deals damage equal to the sacrificed creature's power to target creature.",
        ) {
            val creature = target(TargetFilter.Creature)
            effect = Effects.DealDamage(
                DynamicAmounts.storedNumber("tigerTribeHunterSacrificedPower"),
                creature,
            )
        }
        description = "Pack tactics — Whenever Tiger-Tribe Hunter attacks, if you attacked with " +
            "creatures with total power 6 or greater this combat, you may sacrifice another " +
            "creature. When you do, Tiger-Tribe Hunter deals damage equal to the sacrificed " +
            "creature's power to target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46e0c229-aa31-4c79-bafe-28a8d8e64c61.jpg?1783926471"
        ruling(
            "2021-07-23",
            "The pack tactics ability goes on the stack without a target. If you sacrifice another " +
                "creature as it resolves, the reflexive triggered ability will trigger and choose a " +
                "target for it at that time."
        )
        ruling(
            "2021-07-23",
            "Use the sacrificed creature's power as it last existed on the battlefield to determine " +
                "how much damage is dealt."
        )
    }
}
