package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Jerren, Corrupted Bishop // Ormendahl, the Corrupter (MID #109).
 *
 * "At the beginning of your end step, if you have exactly 13 life, you may pay {4}{B}{B}. If you
 * do, transform Jerren."
 *
 * Exercises the exact-life intervening "if" (13 triggers, 14 doesn't), the optional mana payment,
 * and that paying it turns Jerren into the 6/6 flying, trampling, lifelinking Ormendahl.
 */
class JerrenCorruptedBishopScenarioTest : ScenarioTestBase() {

    init {
        fun board(life: Int) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Jerren, Corrupted Bishop", summoningSickness = false)
            .withLandsOnBattlefield(1, "Swamp", 6)
            .withLifeTotal(1, life)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            .build()

        test("at exactly 13 life, paying {4}{B}{B} transforms Jerren into Ormendahl") {
            val game = board(13)
            val jerren = game.findPermanent("Jerren, Corrupted Bishop")!!

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("the trigger offers the {4}{B}{B} payment") {
                (game.getPendingDecision() is YesNoDecision) shouldBe true
            }
            game.answerYesNo(true).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay().error shouldBe null
            game.resolveStack()

            game.state.getEntity(jerren)!!.get<CardComponent>()!!.name shouldBe "Ormendahl, the Corrupter"
            val projected = game.state.projectedState
            projected.getPower(jerren) shouldBe 6
            projected.getToughness(jerren) shouldBe 6
            projected.hasKeyword(jerren, Keyword.FLYING) shouldBe true
            projected.hasKeyword(jerren, Keyword.TRAMPLE) shouldBe true
            projected.hasKeyword(jerren, Keyword.LIFELINK) shouldBe true
        }

        test("declining the payment leaves Jerren untransformed") {
            val game = board(13)
            val jerren = game.findPermanent("Jerren, Corrupted Bishop")!!

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            (game.getPendingDecision() is YesNoDecision) shouldBe true
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.state.getEntity(jerren)!!.get<CardComponent>()!!.name shouldBe "Jerren, Corrupted Bishop"
        }

        test("at 14 life the intervening if fails and nothing is offered") {
            val game = board(14)
            val jerren = game.findPermanent("Jerren, Corrupted Bishop")!!

            game.passUntilPhase(Phase.ENDING, Step.END)

            withClue("no trigger on the stack and no payment prompt") {
                game.state.stack shouldBe emptyList()
                game.getPendingDecision() shouldBe null
            }
            game.state.getEntity(jerren)!!.get<CardComponent>()!!.name shouldBe "Jerren, Corrupted Bishop"
        }
    }
}
