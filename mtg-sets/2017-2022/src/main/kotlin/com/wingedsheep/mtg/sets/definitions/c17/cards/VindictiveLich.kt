package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Vindictive Lich
 * {3}{B}
 * Creature — Zombie Wizard
 * 4/1
 * When this creature dies, choose one or more. Each mode must target a different player.
 * • Target opponent sacrifices a creature of their choice.
 * • Target opponent discards two cards.
 * • Target opponent loses 5 life.
 *
 * A "choose one or more" modal dies trigger (`minChooseCount = 1`, `chooseCount = 3`) with
 * `eachModeTargetsDifferent`, as on Shadrix Silverquill: the engine only offers a mode while the modes
 * picked so far plus it can still be given distinct opponents, so with N opponents at most N modes can
 * be chosen (ruling). Each mode targets an opponent, announced as the trigger goes on the stack.
 */
val VindictiveLich = card("Vindictive Lich") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Wizard"
    power = 4
    toughness = 1
    oracleText = "When this creature dies, choose one or more. Each mode must target a different player.\n" +
        "• Target opponent sacrifices a creature of their choice.\n" +
        "• Target opponent discards two cards.\n" +
        "• Target opponent loses 5 life."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Modal(
            modes = listOf(
                mode("Target opponent sacrifices a creature of their choice") {
                    val opponent = target(Targets.Opponent)
                    effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, opponent)
                },
                mode("Target opponent discards two cards") {
                    val opponent = target(Targets.Opponent)
                    effect = Effects.Discard(2, opponent)
                },
                mode("Target opponent loses 5 life") {
                    val opponent = target(Targets.Opponent)
                    effect = Effects.LoseLife(5, opponent)
                },
            ),
            chooseCount = 3,
            minChooseCount = 1,
            eachModeTargetsDifferent = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Toma Feizo Gas"
        flavorText = "Heart, mind, soul—weapons all."
        imageUri = "https://cards.scryfall.io/normal/front/3/2/3227d983-e0c5-4dbd-aaff-fb9265199a82.jpg?1783935945"
        ruling("2017-08-25", "You can't choose more modes for Vindictive Lich's triggered ability than you have opponents, but you need to choose at least one target if you can.")
        ruling("2017-08-25", "You don't have to choose a mode for each opponent. For example, you could make one opponent lose 5 life and spare each other opponent. They might not repay your kindness.")
    }
}
