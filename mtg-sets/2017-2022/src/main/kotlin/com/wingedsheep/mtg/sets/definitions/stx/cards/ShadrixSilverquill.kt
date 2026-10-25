package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shadrix Silverquill — Strixhaven: School of Mages #230 (canonical printing)
 * {3}{W}{B} · Legendary Creature — Elder Dragon · 2/5
 *
 * Flying, double strike
 * At the beginning of combat on your turn, you may choose two. Each mode must target a different player.
 * • Target player creates a 2/1 white and black Inkling creature token with flying.
 * • Target player draws a card and loses 1 life.
 * • Target player puts a +1/+1 counter on each creature they control.
 *
 * A choose-two modal trigger with `mayChooseNone` (exactly zero or two modes, never one) and
 * `eachModeTargetsDifferent` (the two modes' target players differ), both read as the trigger is
 * put on the stack. The counter mode is a [Effects.ForEachInGroup] over the targeted player's
 * creatures, so it works even when they control none (ruling).
 */
val ShadrixSilverquill = card("Shadrix Silverquill") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 2
    toughness = 5
    oracleText = "Flying, double strike\n" +
        "At the beginning of combat on your turn, you may choose two. Each mode must target a different player.\n" +
        "• Target player creates a 2/1 white and black Inkling creature token with flying.\n" +
        "• Target player draws a card and loses 1 life.\n" +
        "• Target player puts a +1/+1 counter on each creature they control."

    keywords(Keyword.FLYING, Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.Modal(
            modes = listOf(
                mode("Target player creates a 2/1 white and black Inkling creature token with flying") {
                    val player = target(Targets.Player)
                    effect = Effects.CreateToken(
                        power = 2,
                        toughness = 1,
                        colors = setOf(Color.WHITE, Color.BLACK),
                        creatureTypes = setOf("Inkling"),
                        keywords = setOf(Keyword.FLYING),
                        controller = player
                    )
                },
                mode("Target player draws a card and loses 1 life") {
                    val player = target(Targets.Player)
                    effect = Effects.DrawCards(1, player) then Effects.LoseLife(1, player)
                },
                mode("Target player puts a +1/+1 counter on each creature they control") {
                    val player = target(Targets.Player)
                    effect = Effects.ForEachInGroup(
                        GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player)),
                        Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
                    )
                }
            ),
            chooseCount = 2,
            mayChooseNone = true,
            eachModeTargetsDifferent = true
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "230"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab701909-83d6-4d39-9a84-e6a9b2cb38d6.jpg?1783927294"
        ruling("2021-04-16", "You may choose exactly zero modes or two modes. You can't choose only one mode. If you choose two modes, you choose which two and the target players as you put the triggered ability on the stack.")
        ruling("2021-04-16", "You can target a player with the third mode even if they control no creatures.")
    }
}
