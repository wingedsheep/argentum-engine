package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Inscription of Abundance — Zendikar Rising #186
 * {1}{G} · Instant
 *
 * Kicker {2}{G}
 * Choose one. If this spell was kicked, choose any number instead.
 * • Put two +1/+1 counters on target creature.
 * • Target player gains X life, where X is the greatest power among creatures they control.
 * • Target creature you control fights target creature you don't control.
 *
 * The kicked mode count is a cast-time `dynamicChooseCount` over [Conditions.WasKicked] (one mode
 * unkicked, one to three kicked; no mode twice). Modes resolve in printed order, so X in the life
 * mode is read after the counters mode has already grown a creature.
 */
val InscriptionOfAbundance = card("Inscription of Abundance") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Kicker {2}{G}\n" +
        "Choose one. If this spell was kicked, choose any number instead.\n" +
        "• Put two +1/+1 counters on target creature.\n" +
        "• Target player gains X life, where X is the greatest power among creatures they control.\n" +
        "• Target creature you control fights target creature you don't control."

    keywordAbility(KeywordAbility.kicker("{2}{G}"))

    spell {
        modal(
            chooseCount = 3,
            minChooseCount = 1,
            dynamicChooseCount = DynamicAmounts.conditional(
                condition = Conditions.WasKicked,
                ifTrue = 3,
                ifFalse = 1
            )
        ) {
            mode("Put two +1/+1 counters on target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
            }
            mode("Target player gains X life, where X is the greatest power among creatures they control") {
                val player = target(Targets.Player)
                effect = Effects.GainLife(
                    DynamicAmounts.battlefield(player.asPlayer, GameObjectFilter.Creature).maxPower(),
                    player
                )
            }
            mode("Target creature you control fights target creature you don't control") {
                val yourCreature = target(TargetFilter(GameObjectFilter.Creature.youControl()))
                val theirCreature = target(
                    TargetFilter(
                        GameObjectFilter.Creature.copy(
                            controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                        )
                    )
                )
                effect = Effects.Fight(yourCreature, theirCreature)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "186"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcd2e01e-a143-4b62-8b01-d253fb35c590.jpg?1783929341"
        ruling(
            "2020-09-25",
            "If more than one mode is chosen, perform them in the order written. Nothing can happen " +
                "in between, however, and no player may choose to take actions. Any abilities that " +
                "trigger will be put onto the stack after the spell has finished resolving."
        )
        ruling("2020-09-25", "If any targets become illegal, the other targets will still be affected as appropriate.")
        ruling(
            "2020-09-25",
            "If the second mode is chosen and each creature the target player controls has negative " +
                "power, that player doesn't gain or lose life."
        )
        ruling(
            "2020-09-25",
            "If either target creature is an illegal target as Inscription of Abundance resolves with " +
                "its last mode chosen, no creature will deal or be dealt damage."
        )
        ruling("2020-09-25", "If you kick Inscription of Abundance, you can't choose any one mode more than once.")
    }
}
