package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sage-Eye Avengers — {4}{U}{U} 4/5 Djinn Monk.
 * "Prowess. Whenever this creature attacks, you may return target creature to its owner's hand if
 * its power is less than this creature's power."
 *
 * The power comparison is checked on resolution against the Avengers' current power — it is not a
 * targeting restriction, and the "may" is only offered when the comparison holds.
 */
class SageEyeAvengersScenarioTest : ScenarioTestBase() {

    /**
     * Resolve the attack trigger aimed at [target]. [bounce] answers the "may" prompt; `null`
     * asserts no prompt is offered at all.
     */
    private fun TestGame.drive(target: EntityId, bounce: Boolean?) {
        var guard = 0
        while (guard++ < 40) {
            when (val decision = getPendingDecision()) {
                is ChooseTargetsDecision -> selectTargets(listOf(target))
                is YesNoDecision -> answerYesNo(bounce ?: error("No bounce prompt expected: $decision"))
                null -> if (state.stack.isNotEmpty()) resolveStack() else return
                else -> error("Unexpected decision: $decision")
            }
        }
    }

    private fun board(opposing: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Sage-Eye Avengers")
        .withCardOnBattlefield(2, opposing)
        .withCardInHand(1, "Giant Growth")
        .withLandsOnBattlefield(1, "Forest", 1)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.attack() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Sage-Eye Avengers" to 2)).error shouldBe null
    }

    init {
        test("a creature with lesser power can be returned to its owner's hand") {
            val game = board("Hill Giant")
            val giant = game.findPermanent("Hill Giant")!!

            game.attack()
            game.drive(target = giant, bounce = true)

            game.isOnBattlefield("Hill Giant") shouldBe false
            withClue("returned to its owner's (the opponent's) hand") {
                game.isInHand(2, "Hill Giant") shouldBe true
            }
        }

        test("declining the may leaves the creature in play") {
            val game = board("Hill Giant")
            val giant = game.findPermanent("Hill Giant")!!

            game.attack()
            game.drive(target = giant, bounce = false)

            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("a creature with equal power can be targeted but is not returned") {
            val game = board("Rumbling Baloth")
            val baloth = game.findPermanent("Rumbling Baloth")!!

            game.attack()
            game.drive(target = baloth, bounce = null)

            game.isOnBattlefield("Rumbling Baloth") shouldBe true
        }

        test("the comparison reads the Avengers' current power, including prowess and pumps") {
            val game = board("Craw Wurm")
            val avengers = game.findPermanent("Sage-Eye Avengers")!!
            val wurm = game.findPermanent("Craw Wurm")!!

            game.castSpell(1, "Giant Growth", avengers).error shouldBe null
            game.resolveStack()
            withClue("prowess +1/+1 and Giant Growth +3/+3 make the Avengers 8/9") {
                game.state.projectedState.getPower(avengers) shouldBe 8
            }

            game.attack()
            game.drive(target = wurm, bounce = true)

            game.isOnBattlefield("Craw Wurm") shouldBe false
            game.isInHand(2, "Craw Wurm") shouldBe true
        }
    }
}
