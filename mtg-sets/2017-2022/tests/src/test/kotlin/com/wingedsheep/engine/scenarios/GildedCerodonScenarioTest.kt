package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gilded Cerodon (HOU #94) — {4}{R} Creature — Beast 4/4.
 *
 *   Whenever this creature attacks, if you control a Desert or there is a Desert card in your
 *   graveyard, target creature can't block this turn.
 */
class GildedCerodonScenarioTest : ScenarioTestBase() {

    private val name = "Gilded Cerodon"
    private val desert = "Desert of the Fervent"

    init {
        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withCardOnBattlefield(1, name)
            .withCardOnBattlefield(2, "Grizzly Bears")

        fun attackAndStopBears(game: TestGame) {
            val bears = game.findPermanent("Grizzly Bears")!!
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(name to 2)).error shouldBe null
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("Grizzly Bears can't block this turn") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf(name))).error shouldNotBe null
            }
        }

        test("a Desert you control turns the attack trigger on") {
            val game = board().withCardOnBattlefield(1, desert).build()
            attackAndStopBears(game)
        }

        test("a Desert card in your graveyard turns the attack trigger on") {
            val game = board().withCardInGraveyard(1, desert).build()
            attackAndStopBears(game)
        }

        test("with no Desert on the battlefield or in the graveyard the attack doesn't trigger") {
            val game = board()
                .withCardOnBattlefield(1, "Mountain")
                .withCardInGraveyard(1, "Mountain")
                .withCardOnBattlefield(2, desert)
                .build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(name to 2)).error shouldBe null
            withClue("no target prompt: the intervening-if is false (an opponent's Desert doesn't count)") {
                (game.getPendingDecision() is ChooseTargetsDecision) shouldBe false
            }
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("Grizzly Bears may block") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf(name))).error shouldBe null
            }
        }
    }
}
