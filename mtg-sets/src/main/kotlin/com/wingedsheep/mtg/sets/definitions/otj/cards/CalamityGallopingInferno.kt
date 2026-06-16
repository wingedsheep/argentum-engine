package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Calamity, Galloping Inferno
 * {4}{R}{R}
 * Legendary Creature — Horse Mount
 * 4/6
 *
 * Haste
 * Whenever Calamity attacks while saddled, choose a nonlegendary creature that saddled it this
 * turn and create a tapped and attacking token that's a copy of it. Sacrifice that token at the
 * beginning of the next end step. Repeat this process once.
 * Saddle 1
 *
 * "While saddled" is the intervening-if [Conditions.SourceIsSaddled] (CR 603.4) on the attack
 * trigger. The pool of choosable creatures is "creatures that saddled it this turn"
 * ([CardSource.CreaturesThatSaddledSource], read off Calamity's [CrewSaddleContributorsComponent]),
 * narrowed to nonlegendary ones via the selection filter.
 *
 * "Repeat this process once" means the choose-and-copy happens twice; each iteration makes an
 * independent choice (the same saddler may be chosen both times, the engine's "choose" allows it),
 * so the ability is two sequential choose → copy blocks. Each created token enters tapped and
 * attacking and is scheduled to be sacrificed at the beginning of the next end step (any player's
 * — `sacrificeOnlyOnControllersTurn = false`) via [CreateTokenCopyOfTargetEffect.sacrificeAtStep].
 *
 * If no nonlegendary creature saddled Calamity this turn, the choice has no legal option and that
 * iteration makes no token — matching "choose ... and create" with no valid choice.
 */
val CalamityGallopingInferno = card("Calamity, Galloping Inferno") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Horse Mount"
    power = 4
    toughness = 6
    oracleText = "Haste\n" +
        "Whenever Calamity attacks while saddled, choose a nonlegendary creature that saddled it " +
        "this turn and create a tapped and attacking token that's a copy of it. Sacrifice that " +
        "token at the beginning of the next end step. Repeat this process once.\n" +
        "Saddle 1 (Tap any number of other creatures you control with total power 1 or more: This " +
        "Mount becomes saddled until end of turn. Saddle only as a sorcery.)"

    keywords(Keyword.HASTE)
    keywordAbility(KeywordAbility.saddle(1))

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.SourceIsSaddled
        effect = Effects.Pipeline {
            // First iteration.
            val saddlersA = gather(CardSource.CreaturesThatSaddledSource)
            val chosenA = chooseExactly(
                1,
                from = saddlersA,
                filter = GameObjectFilter.Creature.nonlegendary(),
                useTargetingUI = true,
                prompt = "Choose a nonlegendary creature that saddled Calamity this turn to copy"
            )
            run(
                CreateTokenCopyOfTargetEffect(
                    target = EffectTarget.PipelineTarget(chosenA.key),
                    tapped = true,
                    attacking = true,
                    sacrificeAtStep = Step.END,
                    sacrificeOnlyOnControllersTurn = false
                )
            )

            // Repeat this process once — a second, independent choose-and-copy.
            val saddlersB = gather(CardSource.CreaturesThatSaddledSource)
            val chosenB = chooseExactly(
                1,
                from = saddlersB,
                filter = GameObjectFilter.Creature.nonlegendary(),
                useTargetingUI = true,
                prompt = "Choose a nonlegendary creature that saddled Calamity this turn to copy (again)"
            )
            run(
                CreateTokenCopyOfTargetEffect(
                    target = EffectTarget.PipelineTarget(chosenB.key),
                    tapped = true,
                    attacking = true,
                    sacrificeAtStep = Step.END,
                    sacrificeOnlyOnControllersTurn = false
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "116"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7a70f5a-2056-4c26-b6ea-9f751b5d0d8c.jpg?1712355721"
    }
}
