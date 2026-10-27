package com.wingedsheep.mtg.sets.definitions.bbd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Archfiend of Despair
 * {6}{B}{B}
 * Creature — Demon
 * 6/6
 * Flying
 * Your opponents can't gain life.
 * At the beginning of each end step, each opponent loses life equal to the life that player lost
 * this turn. (Damage causes loss of life.)
 *
 * The static is a [PreventLifeGain] replacement on each opponent (as Gríma Wormtongue).
 * "Each end step" is `Triggers.anyPlayer.beginningOf(Step.END)`. The loss iterates the opponents
 * with `ForEachPlayer`, which rebinds "you" to the visited opponent, so each loses exactly the
 * life *they* lost this turn — read as the ability resolves, so a second Archfiend's trigger
 * sees the first one's loss too (2018-06-08 ruling). `lifeLostThisTurn` never nets gains.
 */
val ArchfiendOfDespair = card("Archfiend of Despair") {
    manaCost = "{6}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 6
    toughness = 6
    oracleText = "Flying\nYour opponents can't gain life.\nAt the beginning of each end step, each opponent " +
        "loses life equal to the life that player lost this turn. (Damage causes loss of life.)"

    keywords(Keyword.FLYING)

    replacementEffect(PreventLifeGain(appliesTo = EventPattern.LifeGainEvent(player = Player.EachOpponent)))

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.LoseLife(DynamicAmounts.lifeLostThisTurn(Player.You), EffectTarget.Controller)
        )
        description = "At the beginning of each end step, each opponent loses life equal to the life " +
            "that player lost this turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "44"
        artist = "Josh Hass"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11d8b7ac-fb65-4496-9d9f-184fa8ad8012.jpg?1783934864"
        ruling(
            "2018-06-08",
            "Archfiend of Despair's last ability counts only how much life was lost. It doesn't care " +
                "whether a player also gained life."
        )
        ruling(
            "2018-06-08",
            "The amount of life to lose is determined only as Archfiend of Despair's triggered ability " +
                "resolves. For example, if you control two of them and an opponent lost 3 life earlier in " +
                "the turn, the first ability to resolve would have that player lose 3 life, and the second " +
                "would have that player lose 6 life."
        )
    }
}
