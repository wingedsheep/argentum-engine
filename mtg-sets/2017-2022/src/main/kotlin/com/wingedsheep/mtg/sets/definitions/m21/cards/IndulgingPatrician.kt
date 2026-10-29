package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Indulging Patrician — Core Set 2021 #219 (canonical printing)
 * {1}{W}{B} · Creature — Vampire Noble · 1/4
 *
 * Flying
 * Lifelink
 * At the beginning of your end step, if you gained 3 or more life this turn, each opponent loses
 * 3 life.
 *
 * Intervening "if" (CR 603.4) on total life gained this turn, not net — Angelic Accord's
 * condition on your own end step.
 */
val IndulgingPatrician = card("Indulging Patrician") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Vampire Noble"
    power = 1
    toughness = 4
    oracleText = "Flying\n" +
        "Lifelink (Damage dealt by this creature also causes you to gain that much life.)\n" +
        "At the beginning of your end step, if you gained 3 or more life this turn, each opponent loses 3 life."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouGainedLifeThisTurnAtLeast(3)
        effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "At the beginning of your end step, if you gained 3 or more life this turn, each " +
            "opponent loses 3 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "Miranda Meeks"
        flavorText = "\"Just a nibble for now. I wouldn't want to ruin my appetite.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/4/642e0c48-1f77-41e0-abbe-f570b757cd66.jpg?1783930663"
        ruling("2020-06-23", "Each opponent loses just 3 life, no matter how much life you've gained past 3 life.")
        ruling("2020-06-23", "Indulging Patrician's last ability looks at how much life you've gained in the turn, even if it wasn't on the battlefield when you gained life. It doesn't care if you also lost life, even if you lost more life than you gained.")
        ruling("2020-06-23", "If you haven't gained 3 life by the time your end step begins, Indulging Patrician's last ability won't trigger at all.")
        ruling("2020-06-23", "In a Two-Headed Giant game, Indulging Patrician's last ability causes the opposing team to lose 6 life.")
    }
}
