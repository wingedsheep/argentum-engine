package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The Speed Demon — Aetherdrift #105
 * {3}{B}{B} · Legendary Creature — Demon · 5/5
 *
 * Flying, trample
 * Start your engines!
 * At the beginning of your end step, you draw X cards and lose X life, where X is your speed.
 *
 * X is a plain [DynamicAmount.Speed] over [Player.You]. Speed can only change via the inherent
 * speed trigger (an opponent losing life during your turn, CR 702.179d), so drawing the cards
 * can't move it between the two halves and reading it twice matches the single printed X. A
 * controller with no speed reads as 0 (CR 702.179f) — which can't happen while The Speed Demon
 * itself is on the battlefield, since start your engines! sets speed to 1 as a state-based action,
 * but does fall out correctly if the trigger somehow resolves after it has left.
 */
val TheSpeedDemon = card("The Speed Demon") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Demon"
    oracleText = "Flying, trample\n" +
        "Start your engines! (If you have no speed, it starts at 1. It increases once on each of " +
        "your turns when an opponent loses life. Max speed is 4.)\n" +
        "At the beginning of your end step, you draw X cards and lose X life, where X is your speed."
    power = 5
    toughness = 5

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    startYourEngines()

    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.Composite(
            Effects.DrawCards(DynamicAmount.Speed(Player.You)),
            Effects.LoseLife(DynamicAmount.Speed(Player.You), EffectTarget.Controller),
        )
        description = "At the beginning of your end step, you draw X cards and lose X life, " +
            "where X is your speed."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "105"
        artist = "Helge C. Balzer"
        flavorText = "Caution is for those who falter."
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62242a80-0444-4a0e-a868-97eabcc77648.jpg?1783907890"
        ruling(
            "2025-02-07",
            "If an effect needs to know what a player's speed is and that player doesn't have a " +
                "speed, their speed is considered 0.",
        )
        ruling(
            "2025-02-07",
            "Start your engines! isn't a triggered ability. Increasing your speed to 1 is something " +
                "that happens as a state-based action as soon as you control a permanent with the " +
                "ability. Notably, this includes gaining control of a permanent with the ability " +
                "that another player controls.",
        )
    }
}
