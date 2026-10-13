package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.ai.engine.LimitedCardRater
import com.wingedsheep.mtg.sets.definitions.msh.cards.AimScientists
import com.wingedsheep.mtg.sets.definitions.msh.cards.LeaderSuperGenius
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.ConniveEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

/**
 * What the shared card-effect walk descends into.
 *
 * The wrapper cases are the fragile ones: a named keyword action that wraps a composite is
 * invisible to both scorers unless [EffectWalker.fold] knows to descend, and the failure is silent
 * — the card keeps working in the engine while its rating and its intent tags quietly change.
 * [com.wingedsheep.sdk.scripting.effects.ConniveEffect] is exactly that case: every conniving card
 * used to hand the walk a bare `CompositeEffect`, so these pin that wrapping the pipeline in the
 * keyword action changed nothing here.
 */
class EffectWalkerTest : FunSpec({

    test("mixed token batches expose each token payoff to the card scorers") {
        val creature = Effects.CreateToken(power = 3, toughness = 3, creatureTypes = setOf("Robot"))
        val treasure = Effects.CreateTreasure()
        EffectWalker.leaves(Effects.CreateTokensTogether(creature, treasure)) shouldBe
            listOf(creature, treasure)
    }

    test("a connive is walked as its pipeline, not as one opaque leaf") {
        val connive = Effects.Connive() as ConniveEffect

        withClue("the wrapper contributes no leaf of its own — the walk is the body's") {
            EffectWalker.leaves(connive) shouldBe EffectWalker.leaves(connive.body)
        }
        withClue("the pipeline's draw is what the scorers read a connive by") {
            EffectWalker.leaves(connive).filterIsInstance<DrawCardsEffect>() shouldHaveSize 1
        }
    }

    test("a conniving card still reads as card draw") {
        withClue("A.I.M. Scientists' ETB connive") {
            CardIntentAnalyzer.analyze(AimScientists).tags shouldContain IntentTag.DRAW
        }
        withClue("Leader, Super-Genius' begin-combat connive") {
            CardIntentAnalyzer.analyze(LeaderSuperGenius).tags shouldContain IntentTag.DRAW
        }
    }

    test("a conniving trigger is worth its draw in the limited heuristic") {
        // Renamed off both rating tables so `rate` falls through to the structural heuristic —
        // every printed conniving card has a vendored Draftsim pick rating that would shadow it.
        val probe = AimScientists.copy(name = "Connive Heuristic Probe")
        val withoutTrigger = probe.copy(script = probe.script.copy(triggeredAbilities = emptyList()))

        withClue("DrawCards(1) scores 0.6, discounted by 0.8 for living on a triggered ability") {
            (LimitedCardRater.rate(probe) - LimitedCardRater.rate(withoutTrigger)) shouldBe
                (0.48 plusOrMinus 1e-9)
        }
    }
})
