package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Arni Slays the Troll — Kaldheim #201
 * {R}{G} · Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Target creature you control fights up to one target creature you don't control.
 * II — Add {R}. Put two +1/+1 counters on up to one target creature you control.
 * III — You gain life equal to the greatest power among creatures you control.
 *
 * Chapter I with no second target chosen fights nobody (Warbriar Blessing's shape). Chapter II adds
 * the {R} first; with no target chosen only the counter step is skipped. The greatest power in
 * chapter III is read on resolution, and a total of 0 or less gains nothing.
 */
val ArniSlaysTheTroll = card("Arni Slays the Troll") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Target creature you control fights up to one target creature you don't control.\n" +
        "II — Add {R}. Put two +1/+1 counters on up to one target creature you control.\n" +
        "III — You gain life equal to the greatest power among creatures you control."

    sagaChapter(1) {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            ),
            optional = true
        )
        effect = Effects.Fight(mine, theirs)
    }

    sagaChapter(2) {
        val mine = target(TargetFilter.CreatureYouControl, optional = true)
        effect = Effects.AddMana(Color.RED) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, mine)
    }

    sagaChapter(3) {
        effect = Effects.GainLife(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d18396f9-ae20-4471-84ab-a2148319bc39.jpg?1783928202"
        ruling(
            "2021-02-05",
            "For the chapter I ability, if either target is an illegal target as that ability resolves, " +
                "neither creature will deal or be dealt damage."
        )
        ruling("2021-02-05", "The chapter II ability isn't a mana ability. It uses the stack and can be responded to.")
        ruling(
            "2021-02-05",
            "If you choose no targets for the chapter II ability, you'll simply add {R} as the ability resolves. " +
                "However, if you do choose a target creature you control, and that creature is an illegal target " +
                "as the ability resolves, the ability won't resolve. You won't add {R} in that case."
        )
        ruling(
            "2021-02-05",
            "For the chapter III ability, use the greatest power among creatures you control as the ability " +
                "resolves to determine how much life you will gain. If you control no creatures at that time, " +
                "or if all creatures you control have power 0 or less, you won't gain any life."
        )
    }
}
